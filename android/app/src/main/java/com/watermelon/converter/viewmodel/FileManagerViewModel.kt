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

    private val _previewedFile = MutableStateFlow<FileNode?>(null)
    val previewedFile: StateFlow<FileNode?> = _previewedFile.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileNode>>(emptyList())
    val searchResults: StateFlow<List<FileNode>> = _searchResults.asStateFlow()

    private val _converting = MutableStateFlow(false)
    val converting: StateFlow<Boolean> = _converting.asStateFlow()

    private val _convertResult = MutableStateFlow<ConvertMarkedResult?>(null)
    val convertResult: StateFlow<ConvertMarkedResult?> = _convertResult.asStateFlow()

    // --- Phase 1: selection (distinct from marking) -------------------------
    // Selection is transient and drives file operations (copy/move/delete/zip/
    // rename). Marking is the separate, conversion-only flag handled above.

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet()) // absolute paths
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    /** Transient status text after a file op (e.g. "Copied 3 files"). */
    private val _opStatus = MutableStateFlow<String?>(null)
    val opStatus: StateFlow<String?> = _opStatus.asStateFlow()

    /** True while the tree is being (re)built or a directory/search is loading. */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

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

    fun isMarked(node: FileNode): Boolean = MarkedFilesStore.isMarked(node.file)
    fun clearMarks() = MarkedFilesStore.clear()

    fun setQuery(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _loading.value = true
            val results = withContext(Dispatchers.IO) {
                repo.search(_currentDir.value, q).filter { _filter.value.accepts(it) }
            }
            _searchResults.value = results
            _loading.value = false
        }
    }

    fun preview(node: FileNode) {
        _previewedFile.value = node
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

    fun closePreview() {
        _preview.value = PreviewState.Empty
        _previewedFile.value = null
    }

    /** Toggle the mark on whatever file is currently previewed. Only SVG files
     *  can be marked (only they can be converted). */
    fun toggleMarkPreviewed() {
        val node = _previewedFile.value ?: return
        if (node.kind == FileKind.Svg) MarkedFilesStore.toggle(node.file)
    }

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
                    // Scan output: record each file to history, tally success/fail.
                    java.util.zip.ZipInputStream(resultZip.inputStream()).use { zis ->
                        while (true) {
                            val e = zis.nextEntry ?: break
                            val content = zis.readBytes()
                            if (e.name.endsWith(".error.txt")) {
                                failed++
                                val src = e.name.removeSuffix(".error.txt")
                                com.watermelon.converter.data.model.HistoryStore.add(
                                    src, "", ok = false, error = String(content).trim()
                                )
                            } else {
                                succeeded++
                                com.watermelon.converter.data.model.HistoryStore.add(
                                    e.name, String(content), ok = true
                                )
                            }
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

    // --- Phase 1: selection management --------------------------------------

    fun isSelected(node: FileNode): Boolean = _selected.value.contains(node.file.absolutePath)

    /** Long-press a file: enter selection mode and select it. */
    fun startSelection(node: FileNode) {
        if (node.isDirectory) return
        _selectionMode.value = true
        _selected.value = _selected.value + node.file.absolutePath
    }

    fun toggleSelect(node: FileNode) {
        if (node.isDirectory) return
        val key = node.file.absolutePath
        _selected.value = _selected.value.toMutableSet().apply {
            if (contains(key)) remove(key) else add(key)
        }
        if (_selected.value.isEmpty()) _selectionMode.value = false
    }

    fun exitSelection() {
        _selectionMode.value = false
        _selected.value = emptySet()
    }

    /** Select all SVGs in the current directory (one level), scoped to filter. */
    fun selectAllSvg() = selectAllOfType(svg = true)

    /** Select all XMLs in the current directory (one level), scoped to filter. */
    fun selectAllXml() = selectAllOfType(svg = false)

    private fun selectAllOfType(svg: Boolean) {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                val f = if (svg) TypeFilter(showSvg = true, showXml = false)
                        else TypeFilter(showSvg = false, showXml = true)
                repo.matchingFilesIn(_currentDir.value, f)
            }
            if (files.isNotEmpty()) {
                _selectionMode.value = true
                _selected.value = _selected.value + files.map { it.absolutePath }
            }
        }
    }

    private fun selectedFiles(): List<File> =
        _selected.value.map { File(it) }.filter { it.exists() }

    fun dismissOpStatus() { _opStatus.value = null }

    // --- Phase 1: file operations -------------------------------------------

    /** Zip & ship: zip the selected SVGs, convert, write to Batch_files. */
    fun zipAndShipSelected() {
        val files = selectedFiles().filter { it.name.endsWith(".svg", true) }
        if (files.isEmpty()) {
            _opStatus.value = "No SVG files selected to convert"
            return
        }
        exitSelection()
        _converting.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val zipBytes = zipFiles(files)
                    val resultZip = native.convertZip(zipBytes, object : com.watermelon.converter.jni.ProgressCallback {
                        override fun onProgress(done: Int, total: Int, currentName: String) {}
                    })
                    var succeeded = 0; var failed = 0
                    java.util.zip.ZipInputStream(resultZip.inputStream()).use { zis ->
                        while (true) {
                            val e = zis.nextEntry ?: break
                            val content = zis.readBytes()
                            if (e.name.endsWith(".error.txt")) {
                                failed++
                                val src = e.name.removeSuffix(".error.txt")
                                com.watermelon.converter.data.model.HistoryStore.add(
                                    src, "", ok = false, error = String(content).trim()
                                )
                            } else {
                                succeeded++
                                com.watermelon.converter.data.model.HistoryStore.add(
                                    e.name, String(content), ok = true
                                )
                            }
                        }
                    }
                    val outFile = File(WvgcPaths.batchFilesDir, "batch_${System.currentTimeMillis()}.zip")
                    outFile.writeBytes(resultZip)
                    ConvertMarkedResult(succeeded, failed, outFile)
                }
                _convertResult.value = result
            } catch (e: Exception) {
                AppLogger.logError("FileManager", "zipAndShipSelected failed", e)
                _opStatus.value = "Conversion failed"
            } finally {
                _converting.value = false
            }
        }
    }

    /** Delete the selected files. */
    fun deleteSelected() {
        val files = selectedFiles()
        if (files.isEmpty()) return
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { repo.delete(files) }
            invalidateAndRebuild()
            exitSelection()
            _opStatus.value = "Deleted $n file${if (n == 1) "" else "s"}"
        }
    }

    /**
     * Rename selected files. The first keeps [baseName]; the rest are numbered
     * baseName_1, baseName_2, … preserving each file's original extension.
     */
    fun renameSelected(baseName: String) {
        val files = selectedFiles()
        if (files.isEmpty() || baseName.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                files.forEachIndexed { i, f ->
                    val ext = f.name.substringAfterLast('.', "")
                    val stem = if (i == 0) baseName else "${baseName}_$i"
                    val newName = if (ext.isEmpty()) stem else "$stem.$ext"
                    repo.rename(f, newName)
                }
            }
            invalidateAndRebuild()
            exitSelection()
            _opStatus.value = "Renamed ${files.size} file${if (files.size == 1) "" else "s"}"
        }
    }

    /**
     * Copy or move selected files into a SAF-picked destination tree.
     * Source files are java.io.File; destination is a content:// tree, so we
     * write through DocumentFile (the correct bridge for an arbitrary picked
     * folder). [move] deletes the source on success.
     */
    fun copyOrMoveSelectedTo(treeUri: android.net.Uri, move: Boolean) {
        val files = selectedFiles()
        if (files.isEmpty()) return
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) {
                var count = 0
                val ctx = getApplication<Application>()
                val destDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, treeUri)
                    ?: return@withContext 0
                for (f in files) {
                    try {
                        val mime = if (f.name.endsWith(".svg", true)) "image/svg+xml" else "text/xml"
                        val created = destDir.createFile(mime, f.name) ?: continue
                        ctx.contentResolver.openOutputStream(created.uri)?.use { out ->
                            f.inputStream().use { it.copyTo(out) }
                        }
                        if (move) f.delete()
                        count++
                    } catch (e: Exception) {
                        AppLogger.logError("FileManager", "copy/move failed for ${f.path}", e)
                    }
                }
                count
            }
            if (move) invalidateAndRebuild()
            exitSelection()
            _opStatus.value = "${if (move) "Moved" else "Copied"} $n file${if (n == 1) "" else "s"}"
        }
    }

    private fun invalidateAndRebuild() {
        childrenCache.clear()
        rebuild()
    }

    // --- tree building -------------------------------------------------------

    private fun rebuild() {
        viewModelScope.launch {
            _loading.value = true
            val list = withContext(Dispatchers.IO) { flatten() }
            _rows.value = list
            _loading.value = false
        }
    }

    private fun flatten(): List<TreeRow> {
        val out = ArrayList<TreeRow>()
        val filter = _filter.value
        fun walk(dir: File, depth: Int) {
            val cacheKey = dir.absolutePath
            val kids = childrenCache.getOrPut(cacheKey) { repo.listChildren(dir) }
            for (node in kids) {
                if (node.isDirectory) {
                    // Only show folders that contain matching SVG/XML somewhere inside.
                    if (!repo.containsMatching(node.file, filter)) continue
                    val isExp = expanded.contains(node.file.absolutePath)
                    out.add(TreeRow(node, depth, isExp))
                    if (isExp) walk(node.file, depth + 1)
                } else {
                    // Files: show only those matching the active type filter.
                    if (filter.accepts(node)) out.add(TreeRow(node, depth, false))
                }
            }
        }
        walk(_currentDir.value, 0)
        return out
    }
}
