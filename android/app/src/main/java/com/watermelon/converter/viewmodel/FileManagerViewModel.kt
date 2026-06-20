// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.converter.data.files.FileKind
import com.watermelon.converter.data.files.FileNode
import com.watermelon.converter.data.files.MarkedFilesStore
import com.watermelon.converter.data.files.RealFileRepository
import com.watermelon.converter.data.files.TypeFilter
import com.watermelon.converter.jni.RealSvgConverter
import com.watermelon.converter.jni.SvgConverter
import com.watermelon.converter.logging.AppLogger
import com.watermelon.converter.util.StoragePermission
import com.watermelon.converter.util.WvgcPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A row in the flattened, indented tree view. */
data class TreeRow(
    val node: FileNode,
    val depth: Int,
    val expanded: Boolean,
)

/** Preview content for the docked pane. */
sealed interface PreviewState {
    data object Empty : PreviewState
    data object Loading : PreviewState
    @Suppress("ArrayInDataClass")
    data class SvgImage(val name: String, val png: ByteArray) : PreviewState
    data class Failed(val name: String, val message: String) : PreviewState
}

/** Outcome of a "convert marked files" run, shown as a small confirmation. */
data class ConvertMarkedResult(
    val succeeded: Int,
    val failed: Int,
    val outputZip: File,
)

class FileManagerViewModel(
    app: Application,
    private val native: SvgConverter,
) : AndroidViewModel(app) {

    constructor(app: Application) : this(app, RealSvgConverter)

    private val repo = RealFileRepository()

    private val _hasPermission = MutableStateFlow(StoragePermission.isGranted())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _currentDir = MutableStateFlow(repo.defaultRoot())
    val currentDir: StateFlow<File> = _currentDir.asStateFlow()

    private val _filter = MutableStateFlow(TypeFilter())
    val filter: StateFlow<TypeFilter> = _filter.asStateFlow()

    private val _rows = MutableStateFlow<List<TreeRow>>(emptyList())
    val rows: StateFlow<List<TreeRow>> = _rows.asStateFlow()

    val marked: StateFlow<Set<String>> = MarkedFilesStore.marked

    private val _preview = MutableStateFlow<PreviewState>(PreviewState.Empty)
    val preview: StateFlow<PreviewState> = _preview.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileNode>>(emptyList())
    val searchResults: StateFlow<List<FileNode>> = _searchResults.asStateFlow()

    private val _converting = MutableStateFlow(false)
    val converting: StateFlow<Boolean> = _converting.asStateFlow()

    private val _convertResult = MutableStateFlow<ConvertMarkedResult?>(null)
    val convertResult: StateFlow<ConvertMarkedResult?> = _convertResult.asStateFlow()

    private val expanded = LinkedHashSet<String>()      // absolute paths
    private val childrenCache = HashMap<String, List<FileNode>>()

    init {
        if (_hasPermission.value) {
            WvgcPaths.ensureDirs()
            rebuild()
        }
    }

    /** Call after returning from the system "All files access" settings screen. */
    fun recheckPermission() {
        val granted = StoragePermission.isGranted()
        _hasPermission.value = granted
        if (granted) {
            WvgcPaths.ensureDirs()
            rebuild()
        }
    }

    fun setFilter(showSvg: Boolean, showXml: Boolean) {
        _filter.value = TypeFilter(showSvg, showXml)
        rebuild()
    }

    fun toggleDir(node: FileNode) {
        val key = node.file.absolutePath
        if (expanded.contains(key)) expanded.remove(key) else expanded.add(key)
        rebuild()
    }

    fun toggleMark(node: FileNode) = MarkedFilesStore.toggle(node.file)
    fun isMarked(node: FileNode): Boolean = MarkedFilesStore.isMarked(node.file)
    fun clearMarks() = MarkedFilesStore.clear()

    fun setQuery(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                repo.search(_currentDir.value, q).filter { _filter.value.accepts(it) }
            }
            _searchResults.value = results
        }
    }

    fun preview(node: FileNode) {
        _preview.value = PreviewState.Loading
        viewModelScope.launch {
            try {
                when (node.kind) {
                    FileKind.Svg -> {
                        val png = withContext(Dispatchers.IO) {
                            native.renderSvgPreview(node.file.readBytes(), 512)
                        }
                        _preview.value = PreviewState.SvgImage(node.name, png)
                    }
                    FileKind.Xml -> {
                        val png = withContext(Dispatchers.IO) {
                            native.renderVdPreview(node.file.readText(), 512)
                        }
                        _preview.value = PreviewState.SvgImage(node.name, png)
                    }
                    else -> _preview.value = PreviewState.Empty
                }
            } catch (e: Exception) {
                AppLogger.logError("FileManager", "preview failed for ${node.name}", e)
                _preview.value = PreviewState.Failed(node.name, e.message ?: "preview failed")
            }
        }
    }

    fun closePreview() { _preview.value = PreviewState.Empty }

    /**
     * Zip every marked SVG file, run native convertZip, write the result into
     * the unified Batch_files output directory. Works regardless of which
     * folders the files were marked in.
     */
    fun convertMarked() {
        val files = MarkedFilesStore.files().filter {
            it.isFile && it.name.endsWith(".svg", ignoreCase = true)
        }
        if (files.isEmpty()) return
        _converting.value = true
        viewModelScope.launch {
            try {
                val outFile = withContext(Dispatchers.IO) {
                    val zipBytes = zipFiles(files)
                    var succeeded = 0
                    var failed = 0
                    val resultZip = native.convertZip(zipBytes, object : com.watermelon.converter.jni.ProgressCallback {
                        override fun onProgress(done: Int, total: Int, currentName: String) {}
                    })
                    // tally success/failure by scanning for .error.txt sidecars
                    java.util.zip.ZipInputStream(resultZip.inputStream()).use { zis ->
                        while (true) {
                            val e = zis.nextEntry ?: break
                            if (e.name.endsWith(".error.txt")) failed++ else succeeded++
                        }
                    }
                    val outName = "batch_${System.currentTimeMillis()}.zip"
                    val outFile = File(WvgcPaths.batchFilesDir, outName)
                    outFile.writeBytes(resultZip)
                    ConvertMarkedResult(succeeded, failed, outFile)
                }
                _convertResult.value = outFile
                MarkedFilesStore.clear()
            } catch (e: Exception) {
                AppLogger.logError("FileManager", "convertMarked failed", e)
            } finally {
                _converting.value = false
            }
        }
    }

    fun dismissConvertResult() { _convertResult.value = null }

    private fun zipFiles(files: List<File>): ByteArray {
        val out = ByteArrayOutputStream()
        val used = HashSet<String>()
        ZipOutputStream(out).use { zos ->
            for (f in files) {
                var name = f.name
                if (!used.add(name)) {
                    val base = name.substringBeforeLast('.', name)
                    val ext = name.substringAfterLast('.', "")
                    var i = 1
                    do {
                        name = if (ext.isEmpty()) "${base}_$i" else "${base}_$i.$ext"
                        i++
                    } while (!used.add(name))
                }
                zos.putNextEntry(ZipEntry(name))
                zos.write(f.readBytes())
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    // --- tree building -------------------------------------------------------

    private fun rebuild() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { flatten() }
            _rows.value = list.filter { _filter.value.accepts(it.node) }
        }
    }

    private fun flatten(): List<TreeRow> {
        val out = ArrayList<TreeRow>()
        fun walk(dir: File, depth: Int) {
            val cacheKey = dir.absolutePath
            val kids = childrenCache.getOrPut(cacheKey) { repo.listChildren(dir) }
            for (node in kids) {
                val isExp = expanded.contains(node.file.absolutePath)
                out.add(TreeRow(node, depth, isExp))
                if (node.isDirectory && isExp) {
                    walk(node.file, depth + 1)
                }
            }
        }
        walk(_currentDir.value, 0)
        return out
    }
}