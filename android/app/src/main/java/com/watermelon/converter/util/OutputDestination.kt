// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.watermelon.converter.logging.AppLogger
import java.io.File

/**
 * Resolves the user's preferred output destination and writes files there.
 * If the user has picked a custom SAF tree URI in Settings, it writes via
 * DocumentFile. If not, it falls back to the default WvgcPaths on the
 * shared filesystem (which requires MANAGE_EXTERNAL_STORAGE).
 */
object OutputDestination {

    /**
     * Write [bytes] to a file named [fileName] in the output destination.
     * Returns the display path of the written file (for the report/UI).
     */
    fun write(
        ctx: Context,
        bytes: ByteArray,
        fileName: String,
        destinationUri: String?,
        mime: String = "application/zip",
    ): String {
        return if (destinationUri != null) {
            writeToSaf(ctx, bytes, fileName, Uri.parse(destinationUri), mime)
        } else {
            writeToFile(bytes, fileName)
        }
    }

    private fun writeToSaf(
        ctx: Context,
        bytes: ByteArray,
        fileName: String,
        treeUri: Uri,
        mime: String,
    ): String {
        val dir = DocumentFile.fromTreeUri(ctx, treeUri)
            ?: throw IllegalStateException("Cannot open output destination tree")
        // Delete existing file with same name to avoid accumulation.
        dir.findFile(fileName)?.delete()
        val doc = dir.createFile(mime, fileName)
            ?: throw IllegalStateException("Cannot create $fileName in output destination")
        ctx.contentResolver.openOutputStream(doc.uri)?.use { it.write(bytes) }
            ?: throw IllegalStateException("Cannot open output stream for $fileName")
        AppLogger.log("OutputDestination", "wrote $fileName via SAF")
        return doc.uri.toString()
    }

    private fun writeToFile(bytes: ByteArray, fileName: String): String {
        val dir = WvgcPaths.batchFilesDir
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        AppLogger.log("OutputDestination", "wrote ${file.path}")
        return file.absolutePath
    }

    /**
     * Human-readable label for the current destination — shown in Settings.
     * Returns null if using the default (so Settings can show "Default (Watermelon/...)").
     */
    fun displayLabel(ctx: Context, destinationUri: String?): String {
        if (destinationUri == null) return WvgcPaths.batchFilesDir.absolutePath
        return try {
            val doc = DocumentFile.fromTreeUri(ctx, Uri.parse(destinationUri))
            doc?.name ?: destinationUri
        } catch (_: Exception) { destinationUri }
    }
}
