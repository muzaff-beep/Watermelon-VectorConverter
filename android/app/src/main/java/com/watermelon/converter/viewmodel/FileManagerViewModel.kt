// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.converter.data.files.FileKind
import com.watermelon.converter.data.files.FileNode
import com.watermelon.converter.data.files.MarkedFilesStore
import com.watermelon.converter.data.files.RealFileRepository
import com.watermelon.converter.data.files.TypeFilter
import com.watermelon.converter.data.files.VolumeNode
import com.watermelon.converter.data.prefs.SettingsRepository
import com.watermelon.converter.jni.RealSvgConverter
import com.watermelon.converter.jni.SvgConverter
import com.watermelon.converter.logging.AppLogger
import com.watermelon.converter.util.StorageVolumes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A row of the tree: either a top-level storage volume, or a file/folder entry. */
sealed interface RowNode {
    data class Volume(val volume: VolumeNode) : RowNode
    data class Entry(val node: FileNode) : RowNode
}

/** A row in the flattened, indented tree view. */
data class TreeRow(
    val row: RowNode,
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
    val savedTo: String,   // file path or destination-folder label, for display only
)

class FileManagerViewModel(
    app: Application,
    private val native: SvgConverter,
) : AndroidViewModel(app) {

    constructor(app: Application) : this(app, RealSvgConverter)

    private val repo = RealFileRepository()
    private val settingsRepo = SettingsRepository(app.applicationContext)

    private suspend fun outputDestUri(): String? =
        settingsRepo.settings.first().outputDestinationUri

    // --- volume tree (root) --------------------------------------------------

    /**
     * null = at the synthetic root (volume list shown).
     * non-null = browsing inside a granted volume's DocumentFile tree.
     */
    private val _currentDir = MutableStateFlow<DocumentFile?>(null)
    val currentDir: StateFlow<DocumentFile?> = _currentDir.asStateFlow()

    private val _volumes = MutableStateFlow<List<VolumeNode>>(emptyList())
    val volumes: StateFlow<List<VolumeNode>> = _volumes.asStateFlow()

    /** Volume id awaiting a SAF grant — UI observes this to launch the picker. */
    private val _pendingGrantVolumeId = MutableStateFlow<String?>(null)
    val pendingGrantVolumeId: StateFlow<String?> = _pendingGrantVolumeId.asStateFlow()

    private val _filter = MutableStateFlow(TypeFilter())
    val filter: StateFlow<TypeFilter> = _filter.asStateFlow()

    private val _rows = MutableStateFlow<List<TreeRow>>(emptyList())
    val rows: StateFlow<List<TreeRow>> = _rows.asStateFlow()

    val marked: StateFlow<Set<String>> = MarkedFilesStore.marked

    private val _preview = MutableStateFlow<PreviewState>(PreviewState.Empty)
    val preview: StateFlow<PreviewState> = _preview.asStateFlow()

    private val _previewedFile = MutableStateFlow<FileNode?>(null)
    val previewedFile: StateFlow<FileNode?> = _previewedFile.asStateFlow()

    private val _properties = MutableStateFlow<com.watermelon.converter.data.model.VectorProperties?>(null)
    val properties: StateFlow<com.watermelon.converter.data.model.VectorProperties?> = _properties.asStateFlow()

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

    private val _selected = MutableStateFlow<Set<String>>(emptySet()) // uri strings
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    /** Transient status text after a file op (e.g. "Copied 3 files"). */
    private val _opStatus = MutableStateFlow<String?>(null)
    val opStatus: StateFlow<String?> = _opStatus.asStateFlow()

    /** True while the tree is being (re)built or a directory/search is loading. */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val expanded = LinkedHashSet<String>()      // uri strings
    private val childrenCache = HashMap<String, List<FileNode>>()
    // Memoizes containsMatching() per directory so expand/collapse and re-render
    // don't repeat the recursive disk scan. Keyed by uri string; cleared when the
    // filter changes (affects what "matching" means) or files change on disk.
    private val containsCache = HashMap<String, Boolean>()

    // Cache resolved DocumentFile roots for granted volumes, keyed by volume id,
    // so re-entering a volume doesn't re-resolve the tree URI every time.
    private val volumeRootCache = HashMap<String, DocumentFile>()

    init {
        refreshVolumes()
    }

    /** Rebuild the volume list from StorageManager + current SAF grants. */
    private fun refreshVolumes() {
        viewModelScope.launch {
            val grants = settingsRepo.settings.first().volumeGrants
            val vols = withContext(Dispatchers.IO) { StorageVolumes.list(getApplication()) }
            _volumes.value = vols.map { v -> VolumeNode(volume = v, grantedTreeUri = grants[v.id]) }
            if (_currentDir.value == null) rebuild(showLoading = false)
        }
    }

    /** No-op retained for lifecycle call-site compatibility. */
    fun recheckPermission() { refreshVolumes() }

    /**
     * Tap on a volume row. If granted, navigate into it; if locked, request
     * the SAF grant — the UI observes [pendingGrantVolumeId] and launches the
     * system folder picker, then calls [onVolumeGranted] with the result.
     */
    fun tapVolume(volume: VolumeNode) {
        val uri = volume.grantedTreeUri
        if (uri == null) {
            _pendingGrantVolumeId.value = volume.volume.id
            return
        }
        enterVolume(volume.volume.id, uri)
    }

    private fun enterVolume(volumeId: String, treeUri: String) {
        val root = volumeRootCache.getOrPut(volumeId) {
            repo.volumeRootDoc(getApplication(), treeUri) ?: return
        }
        childrenCache.clear()
        containsCache.clear()
        expanded.clear()
        _currentDir.value = root
        rebuild(showLoading = true)
    }

    /** Called by the UI after the SAF folder picker returns a tree URI. */
    fun onVolumeGranted(treeUri: Uri) {
        val volumeId = _pendingGrantVolumeId.value ?: return
        _pendingGrantVolumeId.value = null
        viewModelScope.launch {
            settingsRepo.setVolumeGrant(volumeId, treeUri)
            refreshVolumes()
            enterVolume(volumeId, treeUri.toString())
        }
    }

    fun dismissPendingGrant() { _pendingGrantVolumeId.value = null }

    /** Navigate back to the synthetic volume-list root. */
    fun goToVolumeRoot() {
        _currentDir.value = null
        expanded.clear()
        childrenCache.clear()
        containsCache.clear()
        rebuild(showLoading = false)
    }

    fun setFilter(showSvg: Boolean, showXml: Boolean) {
        _filter.value = TypeFilter(showSvg, showXml)
        // Filter change re-filters cached data — no disk re-scan, no spinner.
        containsCache.clear()  // filter affects folder visibility, so drop that memo
        rebuild(showLoading = false)
    }

    fun toggleDir(node: FileNode) {
        val key = node.uriString
        if (expanded.contains(key)) expanded.remove(key) else expanded.add(key)
        // Expand/collapse reads from the children cache — no spinner, no reset.
        rebuild(showLoading = false)
    }

    fun isMarked(node: FileNode): Boolean = MarkedFilesStore.isMarked(node.doc)
    fun clearMarks() = MarkedFilesStore.clear()

    fun setQuery(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        val root = _currentDir.value ?: run { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _loading.value = true
            val results = withContext(Dispatchers.IO) {
                repo.search(root, q).filter { _filter.value.accepts(it) }
            }
            _searchResults.value = results
            _loading.value = false
        }
    }

    fun preview(node: FileNode) {
        _previewedFile.value = node
        _preview.value = PreviewState.Loading
        _properties.value = null
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                // Read bytes once — reused by both render and analysis.
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(node.doc.uri)?.use { it.readBytes() }
                        ?: throw java.io.IOException("cannot open ${node.doc.uri}")
                }

                when (node.kind) {
                    FileKind.Svg, FileKind.Xml -> {
                        // Render at 256px for the docked pane (faster than 512px;
                        // fullscreen expand can re-render at higher res if needed).
                        val renderDeferred = withContext(Dispatchers.IO) {
                            async {
                                when (node.kind) {
                                    FileKind.Svg -> native.renderSvgPreview(bytes, 256)
                                    else         -> native.renderVdPreview(String(bytes), 256)
                                }
                            }
                        }
                        val analyzeDeferred = withContext(Dispatchers.IO) {
                            async {
                                try {
                                    val json = native.analyzeVector(bytes)
                                    com.watermelon.converter.data.model.VectorProperties.fromJson(node.name, json)
                                } catch (e: Exception) {
                                    AppLogger.logError("FileManager", "analysis failed for ${node.name}", e)
                                    null
                                }
                            }
                        }
                        // Await both — they ran in parallel on IO
                        _preview.value    = PreviewState.SvgImage(node.name, renderDeferred.await())
                        _properties.value = analyzeDeferred.await()
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
        _properties.value = null
    }

    /** Toggle the mark on whatever file is currently previewed. Only SVG files
     *  can be marked (only they can be converted). */
    fun toggleMarkPreviewed() {
        val node = _previewedFile.value ?: return
        if (node.kind == FileKind.Svg) MarkedFilesStore.toggle(node.doc)
    }

    /**
     * Zip every marked SVG file, run native convertZip, write the result into
     * the unified Batch_files output directory. Works regardless of which
     * folders the files were marked in.
     */
    fun convertMarked() {
        val ctx = getApplication<Application>()
        val files = MarkedFilesStore.files().filter {
            it.isFile && (it.name ?: "").endsWith(".svg", ignoreCase = true)
        }
        if (files.isEmpty()) return
        _converting.value = true
        viewModelScope.launch {
            try {
                val outFile = withContext(Dispatchers.IO) {
                    val zipBytes = zipFiles(ctx, files)
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
                    val destUri = outputDestUri()
                    com.watermelon.converter.util.OutputDestination.write(
                        getApplication(), resultZip, outName, destUri,
                    )
                    val savedTo = com.watermelon.converter.util.OutputDestination
                        .displayLabel(getApplication(), destUri) + "/" + outName
                    ConvertMarkedResult(succeeded, failed, savedTo)
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

    private fun zipFiles(ctx: Application, files: List<DocumentFile>): ByteArray {
        val out = ByteArrayOutputStream()
        val used = HashSet<String>()
        ZipOutputStream(out).use { zos ->
            for (f in files) {
                var name = f.name ?: continue
                if (!used.add(name)) {
                    val base = name.substringBeforeLast('.', name)
                    val ext = name.substringAfterLast('.', "")
                    var i = 1
                    do {
                        name = if (ext.isEmpty()) "${base}_$i" else "${base}_$i.$ext"
                        i++
                    } while (!used.add(name))
                }
                val bytes = ctx.contentResolver.openInputStream(f.uri)?.use { it.readBytes() } ?: continue
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }

    // --- Phase 1: selection management --------------------------------------

    fun isSelected(node: FileNode): Boolean = _selected.value.contains(node.uriString)

    /** Long-press a file: enter selection mode and select it. */
    fun startSelection(node: FileNode) {
        if (node.isDirectory) return
        _selectionMode.value = true
        _selected.value = _selected.value + node.uriString
    }

    fun toggleSelect(node: FileNode) {
        if (node.isDirectory) return
        val key = node.uriString
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
        val dir = _currentDir.value ?: return
        viewModelScope.launch {
            val docs = withContext(Dispatchers.IO) {
                val f = if (svg) TypeFilter(showSvg = true, showXml = false)
                        else TypeFilter(showSvg = false, showXml = true)
                repo.matchingFilesIn(dir, f)
            }
            if (docs.isNotEmpty()) {
                _selectionMode.value = true
                _selected.value = _selected.value + docs.map { it.uri.toString() }
            }
        }
    }

    private fun selectedNodes(): List<FileNode> {
        val dir = _currentDir.value ?: return emptyList()
        // Selection keys are uri strings; resolve against the flattened tree's
        // cached FileNodes rather than re-querying, since we already hold them.
        val allCached = childrenCache.values.flatten()
        return allCached.filter { _selected.value.contains(it.uriString) && it.doc.exists() }
    }

    fun dismissOpStatus() { _opStatus.value = null }

    // --- Phase 1: file operations -------------------------------------------

    /** Zip & ship: zip the selected SVGs, convert, write to Batch_files. */
    fun zipAndShipSelected() {
        val ctx = getApplication<Application>()
        val files = selectedNodes().map { it.doc }.filter { (it.name ?: "").endsWith(".svg", true) }
        if (files.isEmpty()) {
            _opStatus.value = "No SVG files selected to convert"
            return
        }
        exitSelection()
        _converting.value = true
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val zipBytes = zipFiles(ctx, files)
                    val resultZip = native.convertZip(zipBytes, object : com.watermelon.converter.jni.ProgressCallback {
                        override fun onProgress(done: Int, total: Int, currentName: String) {}
                    })
  