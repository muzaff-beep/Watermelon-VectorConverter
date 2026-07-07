// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import java.io.File

/**
 * A node in the file-manager tree. Directories are expandable; files are
 * leaves. sizeBytes/lastModified may be 0 initially (fast pass) and filled
 * in later (deferred pass) — see RealFileRepository.withMetadata.
 */
data class FileNode(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
) {
    val absolutePath: String get() = file.absolutePath

    val kind: FileKind get() = when {
        isDirectory -> FileKind.Directory
        name.endsWith(".svg", ignoreCase = true) -> FileKind.Svg
        name.endsWith(".xml", ignoreCase = true) -> FileKind.Xml
        else -> FileKind.Other
    }

    companion object {
        fun from(file: File): FileNode = FileNode(
            file = file,
            name = file.name,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isFile) file.length() else 0L,
            lastModified = file.lastModified(),
        )
    }
}

enum class FileKind { Directory, Svg, Xml, Other }

/** Which file types the tree should show. */
data class TypeFilter(
    val showSvg: Boolean = true,
    val showXml: Boolean = true,
) {
    fun accepts(node: FileNode): Boolean = when (node.kind) {
        FileKind.Directory -> true
        FileKind.Svg -> showSvg
        FileKind.Xml -> showXml
        FileKind.Other -> false
    }
}
