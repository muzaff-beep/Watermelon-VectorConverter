// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.model

import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * File + graphical-structure properties for the preview panel properties
 * section. Combines filesystem metadata (from java.io.File) with the
 * structural analysis returned by the Rust core (Contract C-5.0).
 */
data class VectorProperties(
    // --- file metadata ---
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val createdMs: Long?,         // null if the filesystem doesn't store this

    // --- dimensions ---
    val width: Float,
    val height: Float,
    val viewportW: Float,
    val viewportH: Float,

    // --- graphical structure ---
    val pathCount: Int,
    val groupCount: Int,
    val usesPaths: Boolean,
    val usesGradients: Boolean,
    val usesSolidColors: Boolean,
    val usesStrokes: Boolean,
    val singleColorTintable: Boolean,
    val tintColor: String?,
    val isAnimated: Boolean,
) {
    companion object {
        fun from(file: File, analysisJson: String): VectorProperties {
            val j = JSONObject(analysisJson)
            val created: Long? = try {
                val attrs = java.nio.file.Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java)
                val ct = attrs.creationTime().toMillis()
                if (ct > 0L && ct != file.lastModified()) ct else null
            } catch (_: Exception) { null }

            return fromJsonObject(
                j = j,
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                createdMs = created,
            )
        }

        /**
         * Build from just the JSON string and a display name, for cases where
         * the source was a SAF URI (no java.io.File available).
         */
        fun fromJson(name: String, json: String): VectorProperties {
            val j = JSONObject(json)
            return fromJsonObject(
                j = j,
                name = name,
                path = "—",
                sizeBytes = 0L,
                lastModified = System.currentTimeMillis(),
                createdMs = null,
            )
        }

        private fun fromJsonObject(
            j: JSONObject,
            name: String,
            path: String,
            sizeBytes: Long,
            lastModified: Long,
            createdMs: Long?,
        ) = VectorProperties(
            name = name,
            path = path,
            sizeBytes = sizeBytes,
            lastModified = lastModified,
            createdMs = createdMs,
            width = j.getDouble("width").toFloat(),
            height = j.getDouble("height").toFloat(),
            viewportW = j.getDouble("viewportW").toFloat(),
            viewportH = j.getDouble("viewportH").toFloat(),
            pathCount = j.getInt("pathCount"),
            groupCount = j.getInt("groupCount"),
            usesPaths = j.getBoolean("usesPaths"),
            usesGradients = j.getBoolean("usesGradients"),
            usesSolidColors = j.getBoolean("usesSolidColors"),
            usesStrokes = j.getBoolean("usesStrokes"),
            singleColorTintable = j.getBoolean("singleColorTintable"),
            tintColor = if (j.isNull("tintColor")) null else j.getString("tintColor"),
            isAnimated = j.getBoolean("isAnimated"),
        )
    }
}
