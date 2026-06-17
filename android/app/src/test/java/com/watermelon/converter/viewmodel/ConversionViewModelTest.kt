// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

package com.watermelon.converter.viewmodel

import com.watermelon.converter.jni.FakeSvgConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversionViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    // NOTE: ConversionViewModel extends AndroidViewModel and reads a SettingsRepository
    // from the Application context, so the full flow is exercised in the instrumented
    // test (ConversionFlowTest). This JVM test covers the FakeSvgConverter contract
    // directly to keep the unit layer dependency-free.

    @Test fun fake_convert_returns_vector_xml() {
        val fake = FakeSvgConverter()
        val xml = fake.convertSvg("<svg/>".toByteArray())
        assertTrue(xml.contains("android:viewportWidth"))
        assertEquals(1, fake.convertCalls)
    }

    @Test fun fake_error_code_propagates() {
        val fake = FakeSvgConverter(failWithCode = 1002)
        val e = runCatching { fake.convertSvg(ByteArray(0)) }.exceptionOrNull()
        assertTrue(e is com.watermelon.converter.jni.ConversionException)
        assertEquals(1002, (e as com.watermelon.converter.jni.ConversionException).code)
    }

    @Test fun fake_batch_emits_progress() {
        val fake = FakeSvgConverter()
        var last = 0
        val out = fake.convertZip(ByteArray(0), object : com.watermelon.converter.jni.ProgressCallback {
            override fun onProgress(done: Int, total: Int, currentName: String) { last = done; assertEquals(5, total) }
        })
        assertEquals(5, last)
        assertEquals(0x50.toByte(), out[0]) // PK
    }
}
