// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-folder file marking. Process-scoped, like HistoryStore: you can
 * navigate to a different directory in the tree and the marks from folders
 * you've already visited stay marked, so you can build a selection across
 * many locations before acting on it (copy / cut / convert).
 */
object MarkedFilesStore {
    private val _marked = MutableStateFlow<Set<String>>(emptySet()) // absolute paths
    val marked: StateFlow<Set<String>> = _marked.asStateFlow()

    fun isMarked(file: File): Boolean = _marked.value.contains(file.absolutePath)

    fun toggle(file: File) {
        val key = file.absolutePath
        _marked.value = _marked.value.toMutableSet().apply {
            if (contains(key)) remove(key) else add(key)
        }
    }

    fun clear() { _marked.value = emptySet() }

    fun count(): Int = _marked.value.size

    /** Resolve marked paths back to File objects, dropping any that vanished. */
    fun files(): List<File> = _marked.value.map { File(it) }.filter { it.exists() }
}