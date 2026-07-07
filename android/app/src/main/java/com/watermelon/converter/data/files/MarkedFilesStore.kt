// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Cross-folder file marking. Process-scoped: you can navigate to a different
 * directory and marks from folders you've already visited stay marked, so you
 * can build a selection across many locations before converting.
 */
object MarkedFilesStore {
    private val _marked = MutableStateFlow<Set<String>>(emptySet()) // absolute paths
    val marked: StateFlow<Set<String>> = _marked.asStateFlow()

    private val handles = HashMap<String, File>()

    fun isMarked(file: File): Boolean = _marked.value.contains(file.absolutePath)

    fun toggle(file: File) {
        val key = file.absolutePath
        _marked.value = _marked.value.toMutableSet().apply {
            if (contains(key)) {
                remove(key)
                handles.remove(key)
            } else {
                add(key)
                handles[key] = file
            }
        }
    }

    fun clear() {
        _marked.value = emptySet()
        handles.clear()
    }

    fun count(): Int = _marked.value.size

    fun files(): List<File> =
        _marked.value.mapNotNull { handles[it] }.filter { it.exists() }
}
