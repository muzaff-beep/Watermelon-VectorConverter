// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import androidx.documentfile.provider.DocumentFile

/**
 * A node in the SAF-browsed tree. Directories are expandable; files are
 * leaves. Backed by DocumentFile so it works uniformly across the app's own
 * scoped storage and any user-granted external volume (SD card, USB-OTG).
 *
 * A DocumentFile's metadata (name/isDirectory/size/lastModified) requires a
 * content-resolver query per call on some providers, so these are captured
 * once at listing time rather than re-read from [doc] on every access.
 */
data class FileNode(
    val doc: DocumentFile,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
) {
    /** Stable identity for this node — used as list keys and mark/select keys. */
    val uriString: String get() = doc.uri.toString()

    val kind: FileKind get() = when {
        isDirectory -> FileKind.Directory
        name.endsWith(".svg", ignoreCase = true) -> FileKind.Svg
        name.endsWith(".xml", ignoreCase = true) -> FileKind.Xml
        else -> FileKind.Other
    }

    companion object {
        fun from(doc: DocumentFile): FileNode = FileNode(
            doc = doc,
            name = doc.name ?: "",
            isDirectory = doc.isDirectory,
            sizeBytes = if (doc.isFile) doc.length() else 0L,
            lastModified = doc.lastModified(),
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
        FileKind.Directory -> true          // always show dirs so the tree is navigable
        FileKind.Svg -> showSvg
        FileKind.Xml -> showXml
        FileKind.Other -> false             // never show non-SVG/XML files
    }
}