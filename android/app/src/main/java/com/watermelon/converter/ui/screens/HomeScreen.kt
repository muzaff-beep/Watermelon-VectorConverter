// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.ui.theme.*
import com.watermelon.converter.viewmodel.ConversionViewModel
import com.watermelon.converter.viewmodel.ReverseConversionViewModel

/**
 * Home / landing screen. Two conversion directions, each with Single and
 * Batch entry points directly beneath it — everything fits on one screen,
 * no scrolling, on a typical phone (compact spacing throughout; the
 * wordmark + illustration are shrunk rather than removed to keep branding).
 */
@Composable
fun HomeScreen(
    nav: NavController,
    convVm: ConversionViewModel = nav.sharedGraphViewModel(),
    revConvVm: ReverseConversionViewModel = nav.sharedGraphViewModel(),
) {
    val svgPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            convVm.convert(uri)
            nav.navigate(Routes.PREVIEW)
        }
    }
    val xmlPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            revConvVm.convert(uri)
            nav.navigate(Routes.PREVIEW)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Compact wordmark + mark ──────────────────────────────────────
        Text(
            "\uD83C\uDF49",
            fontSize = 34.sp,
        )
        Text(
            "Watermelon Vector Converter",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        // ── SVG -> XML ────────────────────────────────────────────────────
        ConversionOption(
            title = "Convert SVG to XML",
            subtitle = "Vector image \u2192 Android VectorDrawable",
            onSingle = { svgPicker.launch(arrayOf("image/svg+xml", "text/xml", "*/*")) },
            onBatch = { nav.navigate(Routes.BATCH) },
        )

        Spacer(Modifier.height(14.dp))

        // ── XML -> SVG ────────────────────────────────────────────────────
        ConversionOption(
            title = "Convert XML to SVG",
            subtitle = "Android VectorDrawable \u2192 Vector image",
            onSingle = { xmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*")) },
            onBatch = { nav.navigate(Routes.BATCH_REVERSE) },
        )

        Spacer(Modifier.weight(1f, fill = true))
    }
}

@Composable
private fun ConversionOption(
    title: String,
    subtitle: String,
    onSingle: () -> Unit,
    onBatch: () -> Unit,
) {
    ElevatedCard(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = SlateGray,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSingle,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f),
                ) { Text("Single") }
                Button(
                    onClick = onBatch,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
                    modifier = Modifier.weight(1f),
                ) { Text("Batch", color = PureWhite, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
