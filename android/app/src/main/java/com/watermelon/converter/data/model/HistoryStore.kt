// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/** In-session conversion history. Intentionally NOT persisted to disk:
 *  converted XML can be large and the app is offline/ephemeral by design.
 *  Survives across screens (process-scoped), cleared on process death. */
object HistoryStore {
    private val nextId = AtomicLong(1)
    private val _items = MutableStateFlow<List<ConversionRecord>>(emptyList())
    val items: StateFlow<List<ConversionRecord>> = _items.asStateFlow()

    fun add(sourceName: String, vdXml: String, ok: Boolean, error: String? = null) {
        val rec = ConversionRecord(
            id = nextId.getAndIncrement(),
            sourceName = sourceName,
            vdXml = vdXml,
            timestampMillis = System.currentTimeMillis(),
            ok = ok,
            errorMessage = error,
        )
        _items.value = (listOf(rec) + _items.value).take(50)
    }

    fun clear() { _items.value = emptyList() }
}
