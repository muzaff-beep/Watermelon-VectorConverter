// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.util

/**
 * MANAGE_EXTERNAL_STORAGE has been removed. The app now uses its own
 * scoped external storage (Android/data/<package>/files/) which requires
 * no special permission on any Android version.
 *
 * This stub is retained to avoid breaking references at call sites;
 * isGranted() always returns true.
 */
object StoragePermission {
    /** Always true — no permission required for app-scoped storage. */
    fun isGranted(): Boolean = true
}
