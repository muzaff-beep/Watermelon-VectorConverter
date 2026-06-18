// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.files

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.watermelon.converter.logging.AppLogger

/**
 * SAF tree browsing. Lazy: lists one directory level at a time (expand on tap)
 * rather than walking the whole tree, which is the correct pattern for SAF
 * where each level is a ContentResolver query.
 */
class FileTreeRepository(private val context: Context) {

    /** Persist access to a granted tree so it survives app restarts. */
    fun persistTreePermission(treeUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            AppLogger.logError("FileTreeRepository", "could not persist permission", e)
        }
    }

    /** Previously-granted tree roots (so we can restore without re-picking). */
    fun persistedRoots(): List<Uri> =
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }

    /** List the immediate children of a tree/dir document uri. */
    fun listChildren(treeUri: Uri, parentDocId: String? = null): List<FileNode> {
        val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val out = ArrayList<FileNode>()
        val proj = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        try {
            context.contentResolver.query(childrenUri, proj, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (c.moveToNext()) {
                    val id = c.getString(idIdx)
                    val mime = c.getString(mimeIdx)
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    out.add(
                        FileNode(
                            uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                            name = c.getString(nameIdx) ?: "(unnamed)",
                            isDirectory = isDir,
                            mimeType = mime,
                            sizeBytes = if (c.isNull(sizeIdx)) 0 else c.getLong(sizeIdx),
                            lastModified = if (c.isNull(modIdx)) 0 else c.getLong(modIdx),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLogger.logError("FileTreeRepository", "listChildren failed for $docId", e)
        }
        // Directories first, then files; both alphabetical.
        out.sortWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return out
    }

}