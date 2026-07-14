// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.jni

import org.json.JSONObject
import android.util.Base64

/**
 * Kotlin mirror of animation.rs's AnimationKind (Contract C-5.1). Ordinal
 * values (0=None, 1=Avd, 2=SvgSmil, 3=SvgCss) must match
 * jni.rs's nativeDetectAnimation return values exactly.
 */
enum class AnimationKind {
    NONE,
    AVD,
    SVG_SMIL,
    SVG_CSS;

    companion object {
        fun fromOrdinal(value: Int): AnimationKind = when (value) {
            0 -> NONE
            1 -> AVD
            2 -> SVG_SMIL
            3 -> SVG_CSS
            else -> NONE // unrecognized ordinal: degrade to None rather than crash
        }
    }
}

/** Kotlin mirror of animation_engine.rs's LoopMode (Contract C-5.2). */
enum class LoopMode {
    ONCE,
    REPEAT,
    REVERSE;

    companion object {
        fun fromWireName(name: String): LoopMode = when (name) {
            "Once" -> ONCE
            "Repeat" -> REPEAT
            "Reverse" -> REVERSE
            else -> REPEAT // unrecognized value: default to the contract's own default
        }
    }
}

/**
 * Decoded result of nativeRenderAvdFrames (Contract C-5.2). The native side
 * returns a JSON string (see jni.rs's avd_frames_json) rather than a
 * constructed Java object or a custom binary format, for consistency with
 * the existing nativeAnalyzeVector/nativeAnalyzeVdVector JSON-string
 * convention already used elsewhere in this file's sibling native calls.
 * PNG frame bytes are base64-encoded inside the JSON.
 */
data class AvdFramesResult(
    val width: Int,
    val height: Int,
    val loopMode: LoopMode,
    val frameDurationsMs: List<Int>,
    val frames: List<ByteArray>,
) {
    companion object {
        /**
         * Parse the JSON string returned by nativeRenderAvdFrames.
         * Throws org.json.JSONException on malformed input — callers should
         * only ever receive well-formed JSON here since the native side
         * only returns this string on success (errors are thrown as
         * ConversionException instead, never returned as a string).
         */
        fun decode(json: String): AvdFramesResult {
            val obj = JSONObject(json)
            val durationsArr = obj.getJSONArray("frameDurationsMs")
            val durations = (0 until durationsArr.length()).map { durationsArr.getInt(it) }

            val framesArr = obj.getJSONArray("framesBase64")
            val frames = (0 until framesArr.length()).map { i ->
                Base64.decode(framesArr.getString(i), Base64.DEFAULT)
            }

            return AvdFramesResult(
                width = obj.getInt("width"),
                height = obj.getInt("height"),
                loopMode = LoopMode.fromWireName(obj.getString("loopMode")),
                frameDurationsMs = durations,
                frames = frames,
            )
        }
    }
}
