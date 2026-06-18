// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.converter.data.files.FileKind
import com.watermelon.converter.data.files.FileNode
import com.watermelon.converter.data.files.FileTreeRepository
import com.watermelon.converter.data.files.TypeFilter
import com.watermelon.converter.data.repository.FileRepository
import com.watermelon.converter.jni.RealSvgConverter
import com.watermelon.converter.jni.SvgConverter
import com.watermelon.converter.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    data class XmlDrawable(val name: String, val xml: String) : PreviewState
    data class Failed(val name: String, val message: String) : PreviewState
}

class FileManagerViewModel(
    app: Application,
    private val native: SvgConverter,
) : AndroidViewModel(app) {

    constructor(app: Application) : this(app, RealSvgConverter)

    private val files = FileTreeRepository(app.applicationContext)
    private val io = FileRepository(app.applicationContext)

    private val _root = MutableStateFlow<Uri?>(null)
    val root: StateFlow<Uri?> = _root.asStateFlow()

    private val _filter = MutableStateFlow(TypeFilter())
    val filter: StateFlow<TypeFilter> = _filter.asStateFlow()

    private val _rows = MutableStateFlow<List<TreeRow>>(emptyList())
    val rows: StateFlow<List<TreeRow>> = _rows.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet()) // doc uris (for batch)
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _preview = MutableStateFlow<PreviewState>(PreviewState.Empty)
    val preview: StateFlow<PreviewState> = _preview.asStateFlow()

    // expansion state + cached children per directory uri
    private val expanded = LinkedHashSet<String>()
    private val childrenCache = HashMap<String, List<FileNode>>()

    init {
        files.persistedRoots().firstOrNull()?.let { openRoot(it, alreadyPersisted = true) }
    }

    fun openRoot(treeUri: Uri, alreadyPersisted: Boolean = false) {
        if (!alreadyPersisted) files.persistTreePermission(treeUri)
        _root.value = treeUri
        expanded.clear()
        childrenCache.clear()
        rebuild()
    }

    fun setFilter(showSvg: Boolean, showXml: Boolean) {
        _filter.value = TypeFilter(showSvg, showXml)
        rebuild()
    }

    fun toggleDir(node: FileNode) {
        val key = node.uri.toString()
        if (expanded.contains(key)) expanded.remove(key) else expanded.add(key)
        rebuild()
    }

    fun toggleSelect(node: FileNode) {
        val key = node.uri.toString()
        _selected.value = _selected.value.toMutableSet().apply {
            if (contains(key)) remove(key) else add(key)
        }
    }

    fun clearSelection() { _selected.value = emptySet() }

    /** Uris currently selected for a custom batch (SVG only — what convert accepts). */
    fun selectedSvgUris(): List<Uri> {
        val keys = _selected.value
        return flatten().mapNotNull { it.node }
            .filter { keys.contains(it.uri.toString()) && it.kind == FileKind.Svg }
            .map { it.uri }
    }

    fun preview(node: FileNode) {
        _preview.value = PreviewState.Loading
        viewModelScope.launch {
            try {
                when (node.kind) {
                    FileKind.Svg -> {
                        val png = withContext(Dispatchers.IO) {
                            val bytes = io.readBytes(node.uri)
                            native.renderSvgPreview(bytes, 512)
                        }
                        _preview.value = PreviewState.SvgImage(node.name, png)
                    }
                    FileKind.Xml -> {
                        val xml = withContext(Dispatchers.IO) {
                            String(io.readBytes(node.uri))
                        }
                        _preview.value = PreviewState.XmlDrawable(node.name, xml)
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

    // --- tree building -------------------------------------------------------

    private fun rebuild() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { flatten() }
            // apply type filter (dirs always kept)
            _rows.value = list.filter { _filter.value.accepts(it.node) }
        }
    }

    /** Depth-first flatten honoring expansion; lazily lists+caches each dir. */
    private fun flatten(): List<TreeRow> {
        val treeUri = _root.value ?: return emptyList()
        val out = ArrayList<TreeRow>()
        fun walk(docId: String?, depth: Int) {
            val cacheKey = docId ?: "ROOT"
            val kids = childrenCache.getOrPut(cacheKey) { files.listChildren(treeUri, docId) }
            for (node in kids) {
                val isExp = expanded.contains(node.uri.toString())
                out.add(TreeRow(node, depth, isExp))
                if (node.isDirectory && isExp) {
                    walk(android.provider.DocumentsContract.getDocumentId(node.uri), depth + 1)
                }
            }
        }
        walk(null, 0)
        return out
    }
}
