// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. See LICENSE.

package com.watermelon.converter.jni

/** Test double for the native bridge. No .so required. */
class FakeSvgConverter(
    private val failWithCode: Int? = null,
) : SvgConverter {
    var convertCalls = 0
    override fun convertSvg(svg: ByteArray): String {
        convertCalls++
        failWithCode?.let { throw ConversionException(it, "fake failure $it") }
        return "<vector android:viewportWidth=\"24\" android:viewportHeight=\"24\"/>"
    }
    override fun convertZip(zip: ByteArray, cb: ProgressCallback): ByteArray {
        for (i in 1..5) cb.onProgress(i, 5, "f$i.svg")
        return byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK.."
    }
    override fun renderSvgPreview(svg: ByteArray, px: Int): ByteArray =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
    override fun renderVdPreview(vdXml: String, px: Int): ByteArray =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
    override fun cancel() {}
    override fun analyzeVector(bytes: ByteArray): String =
        """{"width":24.0,"height":24.0,"viewportW":24.0,"viewportH":24.0,"pathCount":1,"groupCount":0,"usesPaths":true,"usesGradients":false,"usesSolidColors":true,"usesStrokes":false,"singleColorTintable":true,"tintColor":"#FF000000","isAnimated":false}"""
}