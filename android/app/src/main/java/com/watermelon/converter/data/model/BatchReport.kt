// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.model

/** Outcome of a single file in a batch. */
data class FileOutcome(
    val name: String,
    val ok: Boolean,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
)

/** End-of-run summary built after a batch conversion completes. */
data class BatchReport(
    val total: Int,
    val succeeded: Int,
    val failed: Int,
    val inputBytes: Long,
    val outputBytes: Long,
    val durationMillis: Long,
    val outcomes: List<FileOutcome>,
) {
    val rejected: List<FileOutcome> get() = outcomes.filter { !it.ok }
}
