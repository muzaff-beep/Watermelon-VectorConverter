// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import android.content.Context
import com.watermelon.converter.logging.AppLogger
import java.io.File

/**
 * Real filesystem browsing (MANAGE_EXTERNAL_STORAGE). Lazy: lists one
 * directory level at a time (expand on tap), same pattern as before, just
 * backed by java.io.File instead of SAF document queries — much faster.
 */
class RealFileRepository {

    /** The default starting point: app-scoped external files dir. No permission required. */
    fun defaultRoot(ctx: Context): File = com.watermelon.converter.util.WvgcPaths.fileManagerRoot(ctx)

    fun listChildren(dir: File): List<FileNode> {
        val out = ArrayList<FileNode>()
        try {
            val children = dir.listFiles() ?: return emptyList()
            for (f in children) {
                // Skip hidden/system dirs to keep the tree usable.
                if (f.name.startsWith(".")) continue
                out.add(
                    FileNode(
                        file = f,
                        name = f.name,
                        isDirectory = f.isDirectory,
                        sizeBytes = if (f.isFile) f.length() else 0L,
                        lastModified = f.lastModified(),
                    )
                )
            }
        } catch (e: SecurityException) {
            AppLogger.logError("RealFileRepository", "permission denied listing ${dir.path}", e)
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "listChildren failed for ${dir.path}", e)
        }
        out.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return out
    }

    /**
     * Recursive search within [root] (current folder + subfolders), matching
     * [query] case-insensitively against file/dir names. Bounded by [maxResults]
     * so a huge tree can't hang the UI.
     */
    fun search(root: File, query: String, maxResults: Int = 200): List<FileNode> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val out = ArrayList<FileNode>()
        fun walk(dir: File) {
            if (out.size >= maxResults) return
            val children = try { dir.listFiles() } catch (e: SecurityException) { null } ?: return
            for (f in children) {
                if (out.size >= maxResults) return
                if (f.name.startsWith(".")) continue
                if (f.name.lowercase().contains(q)) {
                    out.add(
                        FileNode(
                            file = f, name = f.name, isDirectory = f.isDirectory,
                            sizeBytes = if (f.isFile) f.length() else 0L,
                            lastModified = f.lastModified(),
                        )
                    )
                }
                if (f.isDirectory) walk(f)
            }
        }
        walk(root)
        return out
    }

    // --- Phase 1: folder filtering -----------------------------------------

    /**
     * Whether [dir] contains at least one file (at any depth, bounded) matching
     * the active type filter. Used to hide folders that have no SVG/XML so the
     * tree only shows relevant directories. Bounded depth keeps it responsive.
     */
    fun containsMatching(dir: File, filter: TypeFilter, maxDepth: Int = 6): Boolean {
        fun walk(d: File, depth: Int): Boolean {
            if (depth > maxDepth) return false
            val children = try { d.listFiles() } catch (e: SecurityException) { null } ?: return false
            for (f in children) {
                if (f.name.startsWith(".")) continue
                if (f.isFile) {
                    val isSvg = f.name.endsWith(".svg", true)
                    val isXml = f.name.endsWith(".xml", true)
                    if ((isSvg && filter.showSvg) || (isXml && filter.showXml)) return true
                } else if (f.isDirectory) {
                    if (walk(f, depth + 1)) return true
                }
            }
            return false
        }
        return walk(dir, 0)
    }

    // --- Phase 1: select-all ------------------------------------------------

    /**
     * All files directly within [dir] (one level) matching the active filter.
     * Used by "select all SVG" / "select all XML" actions.
     */
    fun matchingFilesIn(dir: File, filter: TypeFilter): List<File> {
        val children = try { dir.listFiles() } catch (e: SecurityException) { null } ?: return emptyList()
        return children.filter { f ->
            f.isFile && !f.name.startsWith(".") && run {
                val isSvg = f.name.endsWith(".svg", true)
                val isXml = f.name.endsWith(".xml", true)
                (isSvg && filter.showSvg) || (isXml && filter.showXml)
            }
        }
    }

    // --- Phase 1: file operations (File-to-File) ---------------------------

    /** Delete files (and empty dirs). Returns count actually deleted. */
    fun delete(files: List<File>): Int {
        var n = 0
        for (f in files) {
            try {
                if (f.delete()) n++
            } catch (e: Exception) {
                AppLogger.logError("RealFileRepository", "delete failed for ${f.path}", e)
            }
        }
        return n
    }

    /** Rename a single file to [newName] (name only, same parent dir). */
    fun rename(file: File, newName: String): File? {
        return try {
            val target = File(file.parentFile, newName)
            if (file.renameTo(target)) target else null
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "rename failed for ${file.path}", e)
            null
        }
    }

    /** Copy a file into [destDir] (File destination). Returns the new file. */
    fun copyInto(src: File, destDir: File): File? {
        return try {
            val target = uniqueTarget(destDir, src.name)
            src.copyTo(target, overwrite = false)
            target
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "copy failed for ${src.path}", e)
            null
        }
    }

    /** Move a file into [destDir] (rename if same volume, else copy+delete). */
    fun moveInto(src: File, destDir: File): File? {
        return try {
            val target = uniqueTarget(destDir, src.name)
            if (src.renameTo(target)) {
                target
            } else {
                src.copyTo(target, overwrite = false)
                src.delete()
                target
            }
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "move failed for ${src.path}", e)
            null
        }
    }

    /** Disambiguate a target name if it already exists (file.svg -> file_1.svg). */
    private fun uniqueTarget(destDir: File, name: String): File {
        var target = File(destDir, name)
        if (!target.exists()) return target
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        do {
            val newName = if (ext.isEmpty()) "${base}_$i" else "${base}_$i.$ext"
            target = File(destDir, newName)
            i++
        } while (target.exists())
        return target
    }
}
