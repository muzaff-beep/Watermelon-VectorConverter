// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watermelon.converter.ui.theme.DeepNavy
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.PureWhite
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

/**
 * Document icon with a colored type-badge at the bottom.
 * Matches the mockup: white paper with folded corner, colored strip
 * at the bottom bearing the type label (SVG = teal, XML = dark navy).
 */
@Composable
fun FileTypeIcon(
    label: String,        // "SVG" or "XML"
    badgeColor: Color,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = w * 0.12f
        val foldSize = w * 0.26f
        val badgeH = h * 0.30f
        val paperColor = Color(0xFFFFFFFF)
        val paperStroke = Color(0xFFE2E8F0)

        // ── paper body (rounded rect with top-right corner cut) ──
        val paper = Path().apply {
            moveTo(corner, 0f)
            lineTo(w - foldSize, 0f)
            lineTo(w, foldSize)
            lineTo(w, h)
            quadraticBezierTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticBezierTo(0f, h, 0f, h - corner)
            lineTo(0f, corner)
            quadraticBezierTo(0f, 0f, corner, 0f)
            close()
        }
        drawPath(paper, paperColor)
        drawPath(paper, paperStroke, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))

        // ── folded corner triangle ──
        val fold = Path().apply {
            moveTo(w - foldSize, 0f)
            lineTo(w, foldSize)
            lineTo(w - foldSize, foldSize)
            close()
        }
        drawPath(fold, Color(0xFFE2E8F0))

        // ── colored badge strip at bottom ──
        val badgePath = Path().apply {
            moveTo(0f, h - badgeH)
            lineTo(w, h - badgeH)
            lineTo(w, h - corner)
            quadraticBezierTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticBezierTo(0f, h, 0f, h - corner)
            close()
        }
        drawPath(badgePath, badgeColor)
    }

    // Label overlay — we need a Box to position text over the canvas
    // Use a separate Box approach instead
}

/**
 * Full file icon with badge label — combines Canvas icon with text overlay.
 */
@Composable
fun FileIconWithLabel(
    label: String,
    badgeColor: Color,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size), contentAlignment = Alignment.BottomCenter) {
        FileTypeIcon(label = label, badgeColor = badgeColor, size = size)
        Box(
            Modifier
                .fillMaxWidth()
                .height(size * 0.30f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite,
            )
        }
    }
}

/** Yellow folder icon using Canvas. */
@Composable
fun FolderIcon(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val folderYellow = Color(0xFFFBBC05)
    val folderDark = Color(0xFFF9A825)
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val tabH = h * 0.18f
        val tabW = w * 0.42f
        val bodyTop = h * 0.22f
        val corner = w * 0.1f

        // tab
        val tab = Path().apply {
            moveTo(corner, tabH)
            lineTo(tabW, tabH)
            lineTo(tabW + corner, bodyTop)
            lineTo(corner, bodyTop)
            close()
        }
        drawPath(tab, folderDark)

        // body
        drawRoundRect(
            color = folderYellow,
            topLeft = Offset(0f, bodyTop),
            size = Size(w, h - bodyTop),
            cornerRadius = CornerRadius(corner),
        )
    }
}
