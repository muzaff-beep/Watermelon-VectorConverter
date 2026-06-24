// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.R
import com.watermelon.converter.ui.components.WatermelonLoader
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.ui.theme.*
import com.watermelon.converter.viewmodel.BatchUiState
import com.watermelon.converter.viewmodel.BatchViewModel
import com.watermelon.converter.viewmodel.ConvertUiState
import com.watermelon.converter.viewmodel.ConversionViewModel

@Composable
fun HomeScreen(nav: NavController) {
    val convVm: ConversionViewModel = nav.sharedGraphViewModel()
    val batchVm: BatchViewModel     = nav.sharedGraphViewModel()

    val convState  by convVm.state.collectAsState()
    val batchState by batchVm.state.collectAsState()

    // SAF launcher — single SVG
    val svgPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) convVm.convert(uri)
        // if null (cancelled) → stay on Home, no navigation
    }

    // SAF launcher — ZIP batch
    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) batchVm.convertZip(uri)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Wordmark ─────────────────────────────────────────────────
            Text(
                text = "WE\nSTAND\nWITH",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    lineHeight = 60.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(Modifier.height(8.dp))

            // ── Watermelon illustration (your actual SVG, rendered as PNG) ──
            Image(
                painter = painterResource(id = R.drawable.we_stand_with_watermelon),
                contentDescription = "Watermelon",
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1.6f),
                contentScale = ContentScale.Fit,
            )

            Spacer(Modifier.height(48.dp))

            // ── Inline state — single SVG ─────────────────────────────────
            when (val s = convState) {
                is ConvertUiState.Working -> {
                    WatermelonLoader(size = 48.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Converting…", color = SlateGray)
                    Spacer(Modifier.height(16.dp))
                }
                is ConvertUiState.Done -> {
                    Text(
                        "✓  ${s.sourceName} converted",
                        color = FreshTeal,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { convVm.reset() },
                            shape = RoundedCornerShape(50),
                        ) { Text("Clear") }
                        Button(
                            onClick = { nav.navigate(Routes.PREVIEW) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
                        ) { Text("View result", color = PureWhite) }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                is ConvertUiState.Error -> {
                    Text(s.message, color = WatermelonRed, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { convVm.reset() }) { Text("Dismiss") }
                    Spacer(Modifier.height(16.dp))
                }
                else -> {}
            }

            // ── Inline state — batch ZIP ──────────────────────────────────
            when (val s = batchState) {
                is BatchUiState.Working -> {
                    WatermelonLoader(size = 48.dp)
                    Spacer(Modifier.height(12.dp))
                    val p = s.progress
                    if (p != null) Text("${p.done}/${p.total}  ${p.currentName}", color = SlateGray, textAlign = TextAlign.Center)
                    else Text("Reading archive…", color = SlateGray)
                    Spacer(Modifier.height(16.dp))
                }
                is BatchUiState.Done -> {
                    Text(
                        "✓  Batch complete — ${s.report.succeeded} converted",
                        color = FreshTeal,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { batchVm.dismissReport() },
                            shape = RoundedCornerShape(50),
                        ) { Text("Clear") }
                        Button(
                            onClick = { nav.navigate(Routes.EXPORT) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
                        ) { Text("Export ZIP", color = PureWhite) }
                    }
                    Spacer(Modifier.height(24.dp))
                }
                is BatchUiState.Error -> {
                    Text(s.message, color = WatermelonRed, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { batchVm.reset() }) { Text("Dismiss") }
                    Spacer(Modifier.height(16.dp))
                }
                else -> {}
            }

            // ── Primary button: Convert a single SVG ──────────────────────
            val convBusy = convState is ConvertUiState.Working
            Button(
                onClick = {
                    svgPicker.launch(arrayOf("image/svg+xml", "text/xml"))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WatermelonRed,
                    contentColor = PureWhite,
                    disabledContainerColor = WatermelonRed.copy(alpha = 0.4f),
                    disabledContentColor = PureWhite.copy(alpha = 0.6f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = !convBusy,
            ) {
                Text("Convert a single SVG", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            // ── Secondary button: Batch convert a ZIP ─────────────────────
            val batchBusy = batchState is BatchUiState.Working
            Button(
                onClick = {
                    zipPicker.launch(arrayOf("application/zip"))
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshTeal,
                    contentColor = PureWhite,
                    disabledContainerColor = FreshTeal.copy(alpha = 0.4f),
                    disabledContentColor = PureWhite.copy(alpha = 0.6f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                enabled = !batchBusy,
            ) {
                Text("Batch convert a ZIP", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = { nav.navigate(Routes.HISTORY) }) {
                Text("View history", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 14.sp)
            }

            TextButton(onClick = { nav.navigate(Routes.ABOUT) }) {
                Text("About this app", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f), fontSize = 12.sp)
            }
        }
    }
}