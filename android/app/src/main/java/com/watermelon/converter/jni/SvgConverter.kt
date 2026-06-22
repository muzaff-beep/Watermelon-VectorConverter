// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.jni

/** Abstraction over the native bridge so ViewModels can be unit-tested with a fake.
 *  The production implementation delegates to [SvgConverterNative] (the real .so). */
interface SvgConverter {
    fun convertSvg(svg: ByteArray): String
    fun convertZip(zip: ByteArray, cb: ProgressCallback): ByteArray
    fun renderSvgPreview(svg: ByteArray, px: Int): ByteArray
    fun renderVdPreview(vdXml: String, px: Int): ByteArray
    fun cancel()
    /** Analyze a vector file's structure. Returns a JSON string (Contract C-5.0). */
    fun analyzeVector(bytes: ByteArray): String
}

/** Production implementation — calls into libsvg_converter_core.so (Contract C-3). */
object RealSvgConverter : SvgConverter {
    override fun convertSvg(svg: ByteArray) = SvgConverterNative.nativeConvertSvg(svg)
    override fun convertZip(zip: ByteArray, cb: ProgressCallback) =
        SvgConverterNative.nativeConvertZip(zip, cb)
    override fun renderSvgPreview(svg: ByteArray, px: Int) =
        SvgConverterNative.nativeRenderSvgPreview(svg, px)
    override fun renderVdPreview(vdXml: String, px: Int) =
        SvgConverterNative.nativeRenderVdPreview(vdXml, px)
    override fun cancel() = SvgConverterNative.nativeCancel()
    override fun analyzeVector(bytes: ByteArray) =
        SvgConverterNative.nativeAnalyzeVector(bytes)
}