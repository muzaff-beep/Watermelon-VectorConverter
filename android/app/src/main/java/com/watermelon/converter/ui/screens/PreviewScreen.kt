// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.viewmodel.ConversionViewModel
import com.watermelon.converter.viewmodel.ConvertUiState
import com.watermelon.converter.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(nav: NavController, vm: ConversionViewModel = nav.sharedGraphViewModel()) {
    val state by vm.state.collectAsState()
    val done = state as? ConvertUiState.Done

    Scaffold(
        topBar = {
            val ctx = LocalContext.current
            TopAppBar(
                title = { Text("Preview") },
                actions = {
                    val d = state as? ConvertUiState.Done
                    if (d != null) {
                        TextButton(onClick = { ShareUtils.copyToClipboard(ctx, "VectorDrawable", d.vdXml) }) { Text("Copy") }
                        TextButton(onClick = { ShareUtils.shareText(ctx, d.sourceName, d.vdXml) }) { Text("Share") }
                    }
                },
            )
        },
        bottomBar = {
            if (done != null) {
                BottomAppBar {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.reset(); nav.navigate(Routes.IMPORT) }) { Text("New") }
                    Button(onClick = { nav.navigate(Routes.EXPORT) }, modifier = Modifier.padding(end = 12.dp)) {
                        Text("Export")
                    }
                }
            }
        }
    ) { pad ->
        if (done == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Nothing to preview yet.")
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(done.sourceName, style = MaterialTheme.typography.titleLarge)
            Text(
                "Both previews are approximate (rendered via resvg, not Android's pipeline).",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewTile("Original SVG", done.svgPreviewPng, Modifier.weight(1f))
                PreviewTile("Generated VD", done.vdPreviewPng, Modifier.weight(1f))
            }
            ElevatedCard(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("VectorDrawable XML", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(done.vdXml, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(label: String, png: ByteArray?, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (png != null) {
                val bmp = remember(png) { BitmapFactory.decodeByteArray(png, 0, png.size) }
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = label, modifier = Modifier.size(120.dp))
                } else {
                    Text("—", textAlign = TextAlign.Center)
                }
            } else {
                Text("preview unavailable", textAlign = TextAlign.Center)
            }
        }
    }
}
