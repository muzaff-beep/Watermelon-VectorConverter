// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** SAF-based file IO. Reads bytes in Kotlin and hands ByteArray to native
 *  (never a content:// URI). Writes outputs back through SAF. (Contract C-3 handoff) */
class FileRepository(private val context: Context) {

    /** Read the full bytes of a content:// document. */
    fun readBytes(uri: Uri): ByteArray {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "cannot open input stream for $uri" }
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    /** Write bytes to a new file [displayName] under tree [treeUri]. Returns its Uri. */
    fun writeToTree(treeUri: Uri, displayName: String, mime: String, bytes: ByteArray): Uri? {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val existing = tree.findFile(displayName)
        existing?.delete()
        val file = tree.createFile(mime, displayName) ?: return null
        context.contentResolver.openOutputStream(file.uri).use { os ->
            requireNotNull(os) { "cannot open output stream" }
            os.write(bytes)
        }
        return file.uri
    }

    /** Suggested .xml name from an input .svg display name. */
    fun xmlNameFor(svgName: String): String =
        svgName.substringBeforeLast('.', svgName).let { "$it.xml" }

    /** Suggested .svg name from an input .xml display name (reverse direction). */
    fun svgNameFor(xmlName: String): String =
        xmlName.substringBeforeLast('.', xmlName).let { "$it.svg" }

    /** Best-effort display name for a content uri. */
    fun displayName(uri: Uri): String =
        DocumentFile.fromSingleUri(context, uri)?.name ?: "image.svg"

    /**
     * Build an in-memory ZIP from a list of document uris. Used to feed the
     * native convertZip path with a custom batch of loose SVG files selected
     * across folders in the file manager. Duplicate names are disambiguated.
     */
    fun zipBytes(uris: List<Uri>): ByteArray {
        val out = ByteArrayOutputStream()
        val used = HashSet<String>()
        ZipOutputStream(out).use { zos ->
            for (uri in uris) {
                var name = displayName(uri)
                // ensure unique entry names
                if (!used.add(name)) {
                    val base = name.substringBeforeLast('.', name)
                    val ext = name.substringAfterLast('.', "")
                    var i = 1
                    do {
                        name = if (ext.isEmpty()) "${base}_$i" else "${base}_$i.$ext"
                        i++
                    } while (!used.add(name))
                }
                zos.putNextEntry(ZipEntry(name))
                zos.write(readBytes(uri))
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
