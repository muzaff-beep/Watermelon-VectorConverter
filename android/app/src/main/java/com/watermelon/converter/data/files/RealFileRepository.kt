// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import android.content.Context
import android.os.Environment
import com.watermelon.converter.logging.AppLogger
import java.io.File

/** A top-level storage root shown in the file manager: internal or one external volume. */
data class StorageRoot(
    val label: String,
    val isPrimary: Boolean,
    val root: File,
)

/**
 * Filesystem browsing via plain java.io.File, backed by MANAGE_EXTERNAL_STORAGE.
 * One grant covers the whole device: internal shared storage + any SD card /
 * USB-OTG volume. Lazy: lists one directory level at a time (expand on tap).
 */
class RealFileRepository {

    /** Internal shared storage + any detected external (SD card / USB) volumes. */
    fun storageRoots(ctx: Context): List<StorageRoot> {
        val roots = ArrayList<StorageRoot>()
        roots.add(StorageRoot("Internal Storage", isPrimary = true, root = Environment.getExternalStorageDirectory()))
        try {
            val dirs = ctx.getExternalFilesDirs(null)
            dirLoop@ for (dir in dirs) {
                if (dir == null) continue@dirLoop
                // getExternalFilesDirs returns .../Android/data/<pkg>/files per volume;
                // walk up to the volume root.
                var vol = dir
                repeat(4) { vol = vol.parentFile ?: vol }
                if (vol.absolutePath == roots[0].root.absolutePath) continue@dirLoop
                if (roots.none { it.root.absolutePath == vol.absolutePath }) {
                    roots.add(StorageRoot("SD Card", isPrimary = false, root = vol))
                }
            }
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "external volume detection failed", e)
        }
        return roots
    }

    /**
     * Fast pass: names + extension only, no size/date. Used for the initial
     * render so the list appears immediately.
     */
    fun listChildrenFast(dir: File): List<FileNode> {
        val out = ArrayList<FileNode>()
        val children = dir.listFiles() ?: return out
        for (f in children) {
            val name = f.name
            if (name.startsWith(".")) continue
            out.add(FileNode(file = f, name = name, isDirectory = f.isDirectory, sizeBytes = 0L, lastModified = 0L))
        }
        out.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return out
    }

    /** Deferred pass: fills in size + lastModified for already-listed nodes. */
    fun withMetadata(node: FileNode): FileNode {
        val f = node.file
        return node.copy(
            sizeBytes = if (f.isFile) f.length() else 0L,
            lastModified = f.lastModified(),
        )
    }

    fun search(root: File, query: String, maxResults: Int = 200): List<FileNode> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        val out = ArrayList<FileNode>()
        fun walk(dir: File) {
            if (out.size >= maxResults) return
            val children = dir.listFiles() ?: return
            for (f in children) {
                if (out.size >= maxResults) return
                if (f.name.startsWith(".")) continue
                if (f.name.lowercase().contains(q)) out.add(FileNode.from(f))
                if (f.isDirectory) walk(f)
            }
        }
        walk(root)
        return out
    }

    /** Whether [dir] contains a matching SVG/XML at any depth (bounded). */
    fun containsMatching(dir: File, filter: TypeFilter, maxDepth: Int = 6): Boolean {
        fun walk(d: File, depth: Int): Boolean {
            if (depth > maxDepth) return false
            val children = d.listFiles() ?: return false
            for (f in children) {
                val name = f.name
                if (name.startsWith(".")) continue
                if (f.isFile) {
                    val isSvg = name.endsWith(".svg", true)
                    val isXml = name.endsWith(".xml", true)
                    if ((isSvg && filter.showSvg) || (isXml && filter.showXml)) return true
                } else if (f.isDirectory) {
                    if (walk(f, depth + 1)) return true
                }
            }
            return false
        }
        return walk(dir, 0)
    }

    fun matchingFilesIn(dir: File, filter: TypeFilter): List<File> {
        val children = dir.listFiles() ?: return emptyList()
        return children.filter { f ->
            f.isFile && !f.name.startsWith(".") && run {
                val isSvg = f.name.endsWith(".svg", true)
                val isXml = f.name.endsWith(".xml", true)
                (isSvg && filter.showSvg) || (isXml && filter.showXml)
            }
        }
    }

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

    fun rename(file: File, newName: String): File? {
        return try {
            val target = File(file.parentFile, newName)
            if (file.renameTo(target)) target else null
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "rename failed for ${file.path}", e)
            null
        }
    }

    fun copyInto(src: File, destDir: File): File? {
        return try {
            val target = uniqueTarget(destDir, src.name)
            val out = File(destDir, target)
            src.copyTo(out, overwrite = false)
            out
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "copy failed for ${src.path}", e)
            null
        }
    }

    fun moveInto(src: File, destDir: File): File? {
        val copied = copyInto(src, destDir) ?: return null
        try {
            src.delete()
        } catch (e: Exception) {
            AppLogger.logError("RealFileRepository", "source delete failed after move for ${src.path}", e)
        }
        return copied
    }

    private fun uniqueTarget(destDir: File, name: String): String {
        if (!File(destDir, name).exists()) return name
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        var candidate: String
        do {
            candidate = if (ext.isEmpty()) "${base}_$i" else "${base}_$i.$ext"
            i++
        } while (File(destDir, candidate).exists())
        return candidate
    }
}
