// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.model

/** One in-session conversion result, shown in history. */
data class ConversionRecord(
    val id: Long,
    val sourceName: String,
    val vdXml: String,
    val timestampMillis: Long,
    val ok: Boolean,
    val errorMessage: String? = null,
)
