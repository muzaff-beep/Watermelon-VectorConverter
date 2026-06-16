// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.jni

// FFI bridge — Kotlin side (Contract C-3). Loads libsvg_converter_core.so.
// Signatures must match jni.rs byte-for-byte (verified in CI).

class ConversionException(val code: Int, message: String) : RuntimeException(message)

interface ProgressCallback { fun onProgress(done: Int, total: Int, currentName: String) }

object SvgConverterNative {
    init { System.loadLibrary("svg_converter_core") }

    external fun nativeConvertSvg(svg: ByteArray): String
    external fun nativeConvertZip(zip: ByteArray, cb: ProgressCallback): ByteArray
    external fun nativeRenderSvgPreview(svg: ByteArray, px: Int): ByteArray
    external fun nativeRenderVdPreview(vdXml: String, px: Int): ByteArray
    external fun nativeCancel()
}
