// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

package com.watermelon.converter.jni

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real JNI round-trip — requires the libsvg_converter_core.so built by cargo-ndk.
 *  This is the on-device validation the FFI bridge (Module B) could not get in CI
 *  containers. Runs on an emulator/device in the CI 'connectedCheck' stage. */
@RunWith(AndroidJUnit4::class)
class NativeRoundTripTest {

    private val triangle =
        "<svg viewBox=\"0 0 24 24\"><path d=\"M2 2 L22 2 L22 22 Z\" fill=\"#ff0000\"/></svg>"

    @Test fun convert_real_svg_returns_vectordrawable() {
        val xml = SvgConverterNative.nativeConvertSvg(triangle.toByteArray())
        assertTrue(xml.contains("android:pathData=\"M2,2 L22,2 L22,22 Z\""))
        assertTrue(xml.contains("android:fillColor=\"#FFFF0000\""))
    }

    @Test fun unsupported_feature_throws_1002() {
        val svg = "<svg viewBox=\"0 0 1 1\"><text>x</text></svg>"
        try {
            SvgConverterNative.nativeConvertSvg(svg.toByteArray())
            fail("expected ConversionException")
        } catch (e: ConversionException) {
            assertEquals(1002, e.code)
        }
    }

    @Test fun preview_returns_png_bytes() {
        val png = SvgConverterNative.nativeRenderSvgPreview(triangle.toByteArray(), 64)
        // PNG signature
        assertEquals(0x89.toByte(), png[0]); assertEquals(0x50.toByte(), png[1])
    }
}
