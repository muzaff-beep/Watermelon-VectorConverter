// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.SlateGray
import com.watermelon.converter.ui.theme.WatermelonRed
import kotlin.math.roundToInt

/**
 * Watermelon-seed progress bar: a fixed row of [seedCount] dots that "fill"
 * left-to-right as progress advances. Filled seeds use watermelon red (the
 * seeds); the remainder are muted slate dots. A percentage sits to the right.
 */
@Composable
fun SeedBar(
    progress: Float,           // 0f..1f
    label: String,
    modifier: Modifier = Modifier,
    seedCount: Int = 20,
) {
    val p = progress.coerceIn(0f, 1f)
    val filled = (p * seedCount).roundToInt()
    val empty = (seedCount - filled).coerceAtLeast(0)
    val pct = (p * 100).roundToInt()

    val seeds: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = WatermelonRed)) {
            append("\u25CF".repeat(filled))   // filled seed
        }
        withStyle(SpanStyle(color = SlateGray)) {
            append("\u00B7".repeat(empty))     // unfilled
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = FreshTeal,
                modifier = Modifier.weight(1f),
            )
            Text(pct.toString() + "%", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        Text(seeds, style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
}
