// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

/**
 * Render Android VectorDrawable XML (as raw text) to a Bitmap.
 *
 * IMPORTANT technical reality: Android's normal drawable inflater reads from
 * COMPILED resources (aapt-processed), not arbitrary runtime XML strings. There
 * is no fully public API to inflate a VectorDrawable from a raw string at
 * runtime. We use the semi-supported path: parse the XML into an XmlPullParser
 * and call Drawable.createFromXml with the app resources. This works for
 * standard VectorDrawable XML (the kind this app emits) but may fail on XML
 * that uses aapt:attr gradient blocks (those are an aapt compile-time feature
 * and are NOT supported by runtime inflation). For those we return null and the
 * UI shows a graceful "can't preview" message.
 */
object DrawableRenderer {

    fun renderVectorXml(ctx: Context, xml: String, sizePx: Int): Bitmap? {
        val drawable = inflate(ctx, xml) ?: return null
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else sizePx
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else sizePx
        // Scale to fit sizePx on the long edge.
        val scale = sizePx.toFloat() / maxOf(w, h).toFloat()
        val outW = (w * scale).toInt().coerceAtLeast(1)
        val outH = (h * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, outW, outH)
        drawable.draw(canvas)
        return bmp
    }

    private fun inflate(ctx: Context, xml: String): Drawable? {
        return try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setInput(xml.reader())
            // advance to first tag
            var event = parser.eventType
            while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
                event = parser.next()
            }
            val attrs = Xml.asAttributeSet(parser)
            @Suppress("DEPRECATION")
            Drawable.createFromXml(ctx.resources, parser)
        } catch (e: Exception) {
            null
        }
    }
}
