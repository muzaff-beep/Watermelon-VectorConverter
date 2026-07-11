// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.R
import com.watermelon.converter.Routes
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.ui.theme.*
import com.watermelon.converter.viewmodel.ConversionViewModel
import com.watermelon.converter.viewmodel.ReverseConversionViewModel

/**
 * Home / landing screen. "WE STAND WITH" banner + watermelon illustration
 * (both original assets, shrunk to fit), two conversion directions each with
 * Single/Batch beneath, and the About link at the bottom — all compact
 * enough to fit one screen without scrolling on a typical phone.
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
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // ── "WE STAND WITH" banner + illustration (shrunk to fit) ──────────
        Text(
            text = "WE\nSTAND\nWITH",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Image(
            painter = painterResource(id = R.drawable.we_stand_with_watermelon),
            contentDescription = "Watermelon",
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .aspectRatio(1.6f),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(12.dp))

        // ── SVG -> XML ────────────────────────────────────────────────────
        ConversionOption(
            title = "Convert SVG to XML",
            subtitle = "Vector image \u2192 Android VectorDrawable",
            onSingle = { svgPicker.launch(arrayOf("image/svg+xml", "text/xml", "*/*")) },
            onBatch = { nav.navigate(Routes.BATCH) },
        )

        Spacer(Modifier.height(10.dp))

        // ── XML -> SVG ────────────────────────────────────────────────────
        ConversionOption(
            title = "Convert XML to SVG",
            subtitle = "Android VectorDrawable \u2192 Vector image",
            onSingle = { xmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*")) },
            onBatch = { nav.navigate(Routes.BATCH_REVERSE) },
        )

        Spacer(Modifier.weight(1f, fill = true))

        // ── About (restored) ───────────────────────────────────────────────
        TextButton(onClick = { nav.navigate(Routes.ABOUT) }) {
            Text(
                "About this app",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                fontSize = 12.sp,
            )
        }
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
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = SlateGray,
            )
            Spacer(Modifier.height(10.dp))
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
