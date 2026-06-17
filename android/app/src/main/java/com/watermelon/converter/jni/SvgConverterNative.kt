// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.jni

// FFI bridge — Kotlin side (Contract C-3).
// Signatures MUST match svg-converter-core/src/jni.rs byte-for-byte.
// Enforced by ci/verify_interfaces.py on every push.
//
// STATUS: write-only in CI containers (no JVM/device here). The real
// round-trip is validated by an on-device instrumented test (Doc 5 §6).

/** Thrown by native code on any ConversionError. `code` is the C-4 numeric code. */
class ConversionException(val code: Int, message: String) : RuntimeException(message)

/** Progress callback invoked by the native coordinator during batch conversion. */
interface ProgressCallback {
    fun onProgress(done: Int, total: Int, currentName: String)
}

object SvgConverterNative {
    init { System.loadLibrary("svg_converter_core") }

    external fun nativeConvertSvg(svg: ByteArray): String
    external fun nativeConvertZip(zip: ByteArray, cb: ProgressCallback): ByteArray
    external fun nativeRenderSvgPreview(svg: ByteArray, px: Int): ByteArray
    external fun nativeRenderVdPreview(vdXml: String, px: Int): ByteArray
    external fun nativeCancel()
}
