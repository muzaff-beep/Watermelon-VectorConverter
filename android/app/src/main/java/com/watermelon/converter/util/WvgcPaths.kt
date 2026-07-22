// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Unified output locations in the shared Documents folder, visible to the
 * user in any Files app: Documents/watermelon/wvgc/.
 *
 * Requires MANAGE_EXTERNAL_STORAGE (already granted elsewhere in this app
 * for RealFileRepository's file browser) since scoped storage blocks plain
 * java.io.File writes to shared public directories on API 29+ otherwise.
 *
 * Previously wrote to Android/data/<package>/files/VectorConverter/, which
 * is hidden from the user on Android 11+ (scoped storage) — fixed 2026-07-22.
 */
object WvgcPaths {

    /** Root: Documents/watermelon/wvgc/ */
    fun root(ctx: Context): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "watermelon/wvgc",
        )

    fun batchFilesDir(ctx: Context): File =
        File(root(ctx), "Batch_files").apply { mkdirs() }

    fun isolatedFilesDir(ctx: Context): File =
        File(root(ctx), "Isolated_files").apply { mkdirs() }

    /** Ensure both output directories exist. Call once on app start. */
    fun ensureDirs(ctx: Context) {
        batchFilesDir(ctx)
        isolatedFilesDir(ctx)
    }
}
