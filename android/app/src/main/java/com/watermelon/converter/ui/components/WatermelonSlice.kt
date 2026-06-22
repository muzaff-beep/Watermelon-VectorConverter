// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Watermelon half-slice illustration drawn entirely with Canvas.
 * Matches the mockup: teal rind, thin white pith ring, red flesh, dark
 * teardrop seeds arranged across the flesh in two staggered rows.
 */
@Composable
fun WatermelonSlice(size: Dp = 200.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // The slice sits in the lower half of the canvas — center X, bottom Y.
        val cx = w / 2f
        val cy = h * 0.94f   // anchor the flat edge near the bottom
        val r = w * 0.48f    // outer radius

        drawSlice(cx, cy, r)
    }
}

private fun DrawScope.drawSlice(cx: Float, cy: Float, r: Float) {
    val rindColor  = Color(0xFF2A9D8F)   // FreshTeal
    val pithColor  = Color(0xFFFFFFFF)
    val fleshColor = Color(0xFFE63946)   // WatermelonRed
    val seedColor  = Color(0xFF1A1A2E)   // Charcoal

    val rindThick  = r * 0.09f
    val pithThick  = r * 0.035f

    // ── outer rind (semicircle) ──
    drawArc(
        color = rindColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
    )

    // ── flat base of rind ──
    drawRect(
        color = rindColor,
        topLeft = Offset(cx - r, cy - rindThick),
        size = Size(r * 2, rindThick),
    )

    // ── white pith ring ──
    val r2 = r - rindThick
    drawArc(
        color = pithColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r2, cy - r2),
        size = Size(r2 * 2, r2 * 2),
    )

    // ── red flesh ──
    val r3 = r2 - pithThick
    drawArc(
        color = fleshColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r3, cy - r3),
        size = Size(r3 * 2, r3 * 2),
    )

    // ── flat bottom of flesh ──
    drawRect(
        color = fleshColor,
        topLeft = Offset(cx - r3, cy - r3 * 0.08f),
        size = Size(r3 * 2, r3 * 0.08f),
    )

    // ── seeds: teardrop shapes arranged in two staggered rows ──
    val seedPositions = listOf(
        // row 1 (higher up, 5 seeds)
        Pair(-0.36f, -0.52f), Pair(-0.18f, -0.62f), Pair(0.0f, -0.66f),
        Pair(0.18f, -0.62f), Pair(0.36f, -0.52f),
        // row 2 (lower, 4 seeds)
        Pair(-0.27f, -0.32f), Pair(-0.09f, -0.38f),
        Pair(0.09f, -0.38f), Pair(0.27f, -0.32f),
    )

    val seedW = r3 * 0.075f
    val seedH = r3 * 0.14f

    for ((px, py) in seedPositions) {
        val sx = cx + px * r3
        val sy = cy + py * r3
        drawSeed(sx, sy, seedW, seedH, seedColor)
    }
}

/** Draws a teardrop seed: a thin oval pointed at the top. */
private fun DrawScope.drawSeed(
    cx: Float, cy: Float,
    w: Float, h: Float,
    color: Color,
) {
    val path = Path().apply {
        // Start at top point
        moveTo(cx, cy - h * 0.5f)
        // Curve down-left to bottom
        cubicTo(
            cx - w * 0.6f, cy - h * 0.1f,
            cx - w * 0.5f, cy + h * 0.3f,
            cx, cy + h * 0.5f,
        )
        // Curve back up-right
        cubicTo(
            cx + w * 0.5f, cy + h * 0.3f,
            cx + w * 0.6f, cy - h * 0.1f,
            cx, cy - h * 0.5f,
        )
        close()
    }
    drawPath(path, color)
}
