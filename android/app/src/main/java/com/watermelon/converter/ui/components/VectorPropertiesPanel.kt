// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.watermelon.converter.data.model.VectorProperties
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.SlateGray
import com.watermelon.converter.ui.theme.WatermelonRed
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Properties panel shown below the preview image. Always-visible once a file
 * is previewed. Two sections: file metadata (name, location, size, dates),
 * then graphical structure (paths, gradients, colors, tintable, animated).
 */
@Composable
fun VectorPropertiesPanel(props: VectorProperties, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        SectionHeader("File")
        PropRow("Name", props.name)
        PropRow("Location", props.path)
        PropRow("Size", humanSize(props.sizeBytes))
        PropRow("Modified", formatDate(props.lastModified))
        props.createdMs?.let { PropRow("Created", formatDate(it)) }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        SectionHeader("Dimensions")

        val wStr = formatDim(props.width)
        val hStr = formatDim(props.height)
        PropRow("Size", "$wStr \u00D7 $hStr")
        if (props.viewportW != props.width || props.viewportH != props.height) {
            PropRow("Viewport", "${formatDim(props.viewportW)} \u00D7 ${formatDim(props.viewportH)}")
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        SectionHeader("Structure")

        PropRow("Paths", props.pathCount.toString())
        PropRow("Groups", props.groupCount.toString())
        StructFlag("Uses gradients", props.usesGradients)
        StructFlag("Uses solid colors", props.usesSolidColors)
        StructFlag("Uses strokes", props.usesStrokes)
        StructFlag(
            label = if (props.singleColorTintable) "Single color / tintable" else "Single color / tintable",
            value = props.singleColorTintable,
        )
        if (props.singleColorTintable && props.tintColor != null) {
            PropRow("Tint color", props.tintColor)
        }
        StructFlag(
            label = "Animated",
            value = props.isAnimated,
            trueColor = WatermelonRed,  // animated = notable, not a problem
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = FreshTeal,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun PropRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGray,
            modifier = Modifier.width(100.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StructFlag(
    label: String,
    value: Boolean,
    trueColor: androidx.compose.ui.graphics.Color = FreshTeal,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGray,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (value) "\u2713" else "\u2013",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value) trueColor else SlateGray,
            fontWeight = if (value) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private val df = DecimalFormat("#.##")
private val dateFmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

private fun formatDim(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else df.format(v)
private fun formatDate(ms: Long): String = dateFmt.format(Date(ms))
private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    return String.format("%.2f MB", kb / 1024.0)
}