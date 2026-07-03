// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.OffWhite
import com.watermelon.converter.ui.theme.WatermelonRed
import kotlin.math.cos
import kotlin.math.sin

/**
 * A simple drawn watermelon that spins, used as the app's loading indicator.
 * A circular watermelon: green rind ring, red flesh, a few dark seeds. Spins
 * continuously while shown. No image assets — pure Canvas, themed colors.
 */
@Composable
fun WatermelonLoader(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val transition = rememberInfiniteTransition(label = "watermelon")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val d = this.size.minDimension
            val r = d / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val rindColor = FreshTeal
            val fleshColor = WatermelonRed
            val seedColor = Color(0xFF1A1A2E)
            val pithColor = OffWhite

            rotate(degrees = angle, pivot = center) {
                // green rind (outer disc)
                drawCircle(color = rindColor, radius = r, center = center)
                // thin pale pith ring
                drawCircle(color = pithColor, radius = r * 0.86f, center = center)
                // red flesh
                drawCircle(color = fleshColor, radius = r * 0.78f, center = center)
                // seeds arranged around an inner ring
                val seedRingR = r * 0.45f
                val seedRadius = r * 0.07f
                val seedCount = 7
                for (i in 0 until seedCount) {
                    val a = (2.0 * Math.PI * i / seedCount)
                    val sx = center.x + seedRingR * cos(a).toFloat()
                    val sy = center.y + seedRingR * sin(a).toFloat()
                    drawCircle(color = seedColor, radius = seedRadius, center = Offset(sx, sy))
                }
            }
        }
    }
}