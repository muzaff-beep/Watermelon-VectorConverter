// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.watermelon.converter.logging.AppLogger
import com.watermelon.converter.util.WvgcVolume

/**
 * A synthetic top-level row representing one storage volume (internal shared
 * storage, or an SD card / USB-OTG volume). Shown at the root of the file
 * tree. If [grantedTreeUri] is null the volume hasn't been granted SAF access
 * yet — the UI shows a locked row and requests the grant when tapped.
 */
data class VolumeNode(
    val volume: WvgcVolume,
    val grantedTreeUri: String?,
) {
    val isLocked: Boolean get() = grantedTreeUri == null
}

/**
 * Real filesystem browsing via SAF (DocumentFile), uniform across the app's
 * own scoped storage and any user-granted external volume. Lazy: lists one
 * directory level at a time (expand on tap).
 *
 * There is no single java.io.File root anymore — the tree root is a virtual
 * list of volumes (see [rootNode]); entering a granted volume resolves to
 * that volume's DocumentFile tree root.
 */
class RealFileRepository {

    /**
     * Root DocumentFile for a granted volume, or null if [grantedTreeUri] is
     * null/invalid. This is what FileNode.doc is set to when navigating into
     * a volume from the synthetic root.
     */
    fun volumeRootDoc(ctx: Context, grantedTreeUri: String): DocumentFile? =
        try {
            DocumentFile.fromTreeUri(ctx, Uri.parse(grantedTreeUri))
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "failed to resolve volume root", e)
            null
        }

    fun listChildren(dir: DocumentFile): List<FileNode> {
        val out = ArrayList<FileNode>()
        try {
            for (doc in dir.listFiles()) {
                val name = doc.name ?: continue
                if (name.startsWith(".")) continue // keep the tree usable
                out.add(FileNode.from(doc))
            }
        } catch (e: SecurityException) {
            AppLogger.logError("RealFileRepository", "permission denied listing ${dir.uri}", e)
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "listChildren failed for ${dir.uri}", e)
        }
        out.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return out
    }

    /**
     * Recursive search within [root] (current folder + subfolders), matching
     * [query] case-insensitively against file/dir names. Bounded by [maxResults]
     * so a huge tree can't hang the UI.
     */
    fun search(root: DocumentFile, query: String, maxResults: Int = 200): List<FileNode> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val out = ArrayList<FileNode>()
        fun walk(dir: DocumentFile) {
            if (out.size >= maxResults) return
            val children = try { dir.listFiles() } catch (e: SecurityException) { return } catch (e: Exception) { return }
            for (doc in children) {
                if (out.size >= maxResults) return
                val name = doc.name ?: continue
                if (name.startsWith(".")) continue
                if (name.lowercase().contains(q)) out.add(FileNode.from(doc))
                if (doc.isDirectory) walk(doc)
            }
        }
        walk(root)
        return out
    }

    // --- folder filtering -----------------------------------------

    /**
     * Whether [dir] contains at least one file (at any depth, bounded) matching
     * the active type filter. Used to hide folders that have no SVG/XML so the
     * tree only shows relevant directories. Bounded depth keeps it responsive.
     */
    fun containsMatching(dir: DocumentFile, filter: TypeFilter, maxDepth: Int = 6): Boolean {
        fun walk(d: DocumentFile, depth: Int): Boolean {
            if (depth > maxDepth) return false
            val children = try { d.listFiles() } catch (e: SecurityException) { return false } catch (e: Exception) { return false }
            for (doc in children) {
                val name = doc.name ?: continue
                if (name.startsWith(".")) continue
                if (doc.isFile) {
                    val isSvg = name.endsWith(".svg", true)
                    val isXml = name.endsWith(".xml", true)
                    if ((isSvg && filter.showSvg) || (isXml && filter.showXml)) return true
                } else if (doc.isDirectory) {
                    if (walk(doc, depth + 1)) return true
                }
            }
            return false
        }
        return walk(dir, 0)
    }

    // --- select-all ------------------------------------------------

    /**
     * All files directly within [dir] (one level) matching the active filter.
     * Used by "select all SVG" / "select all XML" actions.
     */
    fun matchingFilesIn(dir: DocumentFile, filter: TypeFilter): List<DocumentFile> {
        val children = try { dir.listFiles() } catch (e: SecurityException) { return emptyList() } catch (e: Exception) { return emptyList() }
        return children.filter { doc ->
            val name = doc.name ?: return@filter false
            doc.isFile && !name.startsWith(".") && run {
                val isSvg = name.endsWith(".svg", true)
                val isXml = name.endsWith(".xml", true)
                (isSvg && filter.showSvg) || (isXml && filter.showXml)
            }
        }
    }

    // --- file operations (DocumentFile-to-DocumentFile) ---------------------

    /** Delete files (and empty dirs). Returns count actually deleted. */
    fun delete(files: List<DocumentFile>): Int {
        var n = 0
        for (f in files) {
            try {
                if (f.delete()) n++
            } catch (e: Exception) {
                AppLogger.logError("RealFileRepository", "delete failed for ${f.uri}", e)
            }
        }
        return n
    }

    /** Rename a single file to [newName] (name only, same parent dir). */
    fun rename(file: DocumentFile, newName: String): DocumentFile? {
        return try {
            if (file.renameTo(newName)) file else null
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "rename failed for ${file.uri}", e)
            null
        }
    }

    /** Copy a file into [destDir]. Returns the new DocumentFile. */
    fun copyInto(ctx: Context, src: DocumentFile, destDir: DocumentFile): DocumentFile? {
        return try {
            val name = src.name ?: return null
            val mime = src.type ?: "application/octet-stream"
            val target = uniqueTarget(destDir, name)
            val created = destDir.createFile(mime, target) ?: return null
            ctx.contentResolver.openInputStream(src.uri)?.use { input ->
                ctx.contentResolver.openOutputStream(created.uri)?.use { output ->
                    input.copyTo(output)
                } ?: return null
            } ?: return null
            created
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "copy failed for ${src.uri}", e)
            null
        }
    }

    /** Move a file into [destDir] (copy then delete source — SAF has no cross-tree rename). */
    fun moveInto(ctx: Context, src: DocumentFile, destDir: DocumentFile): DocumentFile? {
        val copied = copyInto(ctx, src, destDir) ?: return null
        try {
            src.delete()
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "source delete failed after move for ${src.uri}", e)
        }
        return copied
    }

    /** Disambiguate a target name if it already exists (file.svg -> file_1.svg). */
    private fun uniqueTarget(destDir: DocumentFile, name: String): String {
        if (destDir.findFile(name) == null) return name
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        var candidate: String
        do {
            candidate = if (ext.isEmpty()) "${base}_$i" else "${base}_$i.$ext"
            i++
        } while (destDir.findFile(candidate) != null)
        return candidate
    }
}
