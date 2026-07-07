// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-folder file marking. Process-scoped, like HistoryStore: you can
 * navigate to a different directory in the tree and the marks from folders
 * you've already visited stay marked, so you can build a selection across
 * many locations before acting on it (copy / cut / convert).
 *
 * Keyed internally by DocumentFile.uri.toString() — the SAF equivalent of an
 * absolute path — since DocumentFile has no filesystem path of its own.
 */
object MarkedFilesStore {
    private val _marked = MutableStateFlow<Set<String>>(emptySet()) // uri strings
    val marked: StateFlow<Set<String>> = _marked.asStateFlow()

    // Keep the actual DocumentFile handles around so files() can return them
    // without needing a Context/tree lookup to reconstruct from a bare uri.
    private val handles = HashMap<String, DocumentFile>()

    fun isMarked(doc: DocumentFile): Boolean = _marked.value.contains(doc.uri.toString())

    fun toggle(doc: DocumentFile) {
        val key = doc.uri.toString()
        _marked.value = _marked.value.toMutableSet().apply {
            if (contains(key)) {
                remove(key)
                handles.remove(key)
            } else {
                add(key)
                handles[key] = doc
            }
        }
    }

    fun clear() {
        _marked.value = emptySet()
        handles.clear()
    }

    fun count(): Int = _marked.value.size

    /** Resolve marked entries back to DocumentFile handles, dropping any that vanished. */
    fun files(): List<DocumentFile> =
        _marked.value.mapNotNull { handles[it] }.filter { it.exists() }
}
