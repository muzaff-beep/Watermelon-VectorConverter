// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.jni

import android.content.Context
import com.watermelon.converter.R

/** Maps frozen C-4 error codes to localized string resources. */
fun ConversionException.userMessage(ctx: Context): String = when (code) {
    1001 -> ctx.getString(R.string.err_1001_invalid_svg)
    1002 -> ctx.getString(R.string.err_1002_unsupported)
    1003 -> ctx.getString(R.string.err_1003_zip_read)
    1004 -> ctx.getString(R.string.err_1004_zip_write)
    1005 -> ctx.getString(R.string.err_1005_io)
    1006 -> ctx.getString(R.string.err_1006_render)
    1007 -> ctx.getString(R.string.err_1007_cancelled)
    else -> ctx.getString(R.string.err_1099_internal)
}
