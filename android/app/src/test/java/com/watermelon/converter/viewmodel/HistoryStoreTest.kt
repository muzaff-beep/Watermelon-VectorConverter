// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

package com.watermelon.converter.viewmodel

import com.watermelon.converter.data.model.HistoryStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HistoryStoreTest {
    @Before fun clear() { HistoryStore.clear() }

    @Test fun add_prepends_and_caps() {
        repeat(60) { HistoryStore.add("f$it.svg", "<vector/>", ok = true) }
        val items = HistoryStore.items.value
        assertEquals(50, items.size)              // capped at 50
        assertEquals("f59.svg", items.first().sourceName) // newest first
    }

    @Test fun failed_record_keeps_error() {
        HistoryStore.add("bad.svg", "", ok = false, error = "boom")
        assertFalse(HistoryStore.items.value.first().ok)
        assertEquals("boom", HistoryStore.items.value.first().errorMessage)
    }
}
