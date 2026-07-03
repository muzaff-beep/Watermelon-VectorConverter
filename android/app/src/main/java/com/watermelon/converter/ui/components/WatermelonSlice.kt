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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Watermelon half-slice — flat edge at top, rind arc curving downward,
 * like a smile / right-side-up slice sitting on a table.
 * Teal rind → white pith ring → red flesh → dark teardrop seeds.
 */
@Composable
fun WatermelonSlice(size: Dp = 200.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Centre at top-centre; arc opens downward
        val cx = w / 2f
        val cy = h * 0.06f   // flat edge near the top
        val r  = w * 0.47f
        drawSlice(cx, cy, r)
    }
}

private fun DrawScope.drawSlice(cx: Float, cy: Float, r: Float) {
    val rindColor  = Color(0xFF2A9D8F)
    val pithColor  = Color(0xFFFFFFFF)
    val fleshColor = Color(0xFFE63946)
    val seedColor  = Color(0xFF1A1A2E)

    val rindThick = r * 0.10f
    val pithThick = r * 0.035f

    // ── green rind: bottom semicircle (arc opens downward, 0→180) ──
    drawArc(
        color = rindColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r, cy),
        size = Size(r * 2, r * 2),
    )
    // flat top strip of rind
    drawRect(
        color = rindColor,
        topLeft = Offset(cx - r, cy),
        size = Size(r * 2, rindThick),
    )

    // ── white pith ──
    val r2 = r - rindThick
    drawArc(
        color = pithColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r2, cy),
        size = Size(r2 * 2, r2 * 2),
    )

    // ── red flesh ──
    val r3 = r2 - pithThick
    drawArc(
        color = fleshColor,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r3, cy),
        size = Size(r3 * 2, r3 * 2),
    )
    // flat top of flesh
    drawRect(
        color = fleshColor,
        topLeft = Offset(cx - r3, cy),
        size = Size(r3 * 2, r3 * 0.07f),
    )

    // ── seeds: two staggered rows, arc opens downward so seeds below cy ──
    val seedPositions = listOf(
        // upper row (5 seeds, closer to flat edge)
        Pair(-0.36f, 0.28f), Pair(-0.18f, 0.38f), Pair(0.0f, 0.42f),
        Pair(0.18f, 0.38f),  Pair(0.36f, 0.28f),
        // lower row (4 seeds, deeper in flesh)
        Pair(-0.27f, 0.58f), Pair(-0.09f, 0.64f),
        Pair(0.09f, 0.64f),  Pair(0.27f, 0.58f),
    )

    val seedW = r3 * 0.075f
    val seedH = r3 * 0.14f

    for ((px, py) in seedPositions) {
        val sx = cx + px * r3
        val sy = cy + py * r3
        drawSeed(sx, sy, seedW, seedH, seedColor)
    }
}

private fun DrawScope.drawSeed(cx: Float, cy: Float, w: Float, h: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - h * 0.5f)
        cubicTo(cx - w * 0.6f, cy - h * 0.1f, cx - w * 0.5f, cy + h * 0.3f, cx, cy + h * 0.5f)
        cubicTo(cx + w * 0.5f, cy + h * 0.3f, cx + w * 0.6f, cy - h * 0.1f, cx, cy - h * 0.5f)
        close()
    }
    drawPath(path, color)
}