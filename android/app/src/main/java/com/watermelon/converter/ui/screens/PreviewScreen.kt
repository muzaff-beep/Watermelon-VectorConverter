// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.ui.components.VectorPropertiesPanel
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.ui.theme.*
import com.watermelon.converter.util.ShareUtils
import com.watermelon.converter.viewmodel.ConversionViewModel
import com.watermelon.converter.viewmodel.ConvertUiState
import com.watermelon.converter.viewmodel.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    nav: NavController,
    vm: ConversionViewModel = nav.sharedGraphViewModel(),
    settingsVm: SettingsViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val settings by settingsVm.settings.collectAsState()
    val done = state as? ConvertUiState.Done
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        done?.sourceName ?: "Preview",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                },
                actions = {
                    if (done != null) {
                        TextButton(onClick = { ShareUtils.copyToClipboard(ctx, "VectorDrawable", done.vdXml) }) {
                            Text("Copy", color = FreshTeal)
                        }
                        TextButton(onClick = { ShareUtils.shareText(ctx, done.sourceName, done.vdXml) }) {
                            Text("Share", color = FreshTeal)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (done != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { vm.reset(); nav.navigate(Routes.PAGER) { popUpTo(Routes.PAGER) { inclusive = false } } },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f),
                    ) { Text("New") }
                    Button(
                        onClick = { nav.navigate(Routes.EXPORT) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
                        modifier = Modifier.weight(1f),
                    ) { Text("Export", color = PureWhite, fontWeight = FontWeight.Bold) }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        if (done == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Nothing to preview yet.", color = SlateGray)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Conversion report ────────────────────────────────────────────
            ConversionReport(done.sourceName, done.vdXml)

            // ── Preview tiles: Original SVG vs Generated VD ──────────────────
            Text(
                "Both previews are approximate (rendered via resvg, not Android's pipeline).",
                style = MaterialTheme.typography.labelLarge,
                color = SlateGray,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewTile("Original SVG", done.svgPreviewPng, Modifier.weight(1f))
                PreviewTile("Generated VD",  done.vdPreviewPng,  Modifier.weight(1f))
            }

            // ── Properties panel (if analysis available + setting enabled) ───
            if (settings.showFileProperties && done.analysisJson != null) {
                HorizontalDivider(color = Color(0xFFE2E8F0))
                val props = remember(done.analysisJson) {
                    runCatching {
                        // We don't have a File object here (the source was a URI),
                        // so we build a minimal VectorProperties from the JSON only.
                        com.watermelon.converter.data.model.VectorProperties.fromJson(
                            name = done.sourceName,
                            json = done.analysisJson,
                        )
                    }.getOrNull()
                }
                if (props != null) {
                    VectorPropertiesPanel(props)
                }
            }

            // ── VectorDrawable XML (collapsible card) ────────────────────────
            var xmlExpanded by remember { mutableStateOf(false) }
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "VectorDrawable XML",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepNavy,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { xmlExpanded = !xmlExpanded }) {
                            Text(if (xmlExpanded) "Collapse" else "Expand", color = FreshTeal, fontSize = 12.sp)
                        }
                    }
                    if (xmlExpanded) {
                        Spacer(Modifier.height(8.dp))
                        Text(done.vdXml, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversionReport(sourceName: String, vdXml: String) {
    val lineCount = vdXml.lines().size
    val sizeKb    = "%.1f KB".format(vdXml.toByteArray().size / 1024.0)

    ElevatedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = FreshTeal.copy(alpha = 0.08f),
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "✓  Conversion successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = FreshTeal,
                    modifier = Modifier.weight(1f),
                )
            }
            ReportRow("Source file", sourceName)
            ReportRow("Output size", sizeKb)
            ReportRow("XML lines",   lineCount.toString())
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SlateGray, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun PreviewTile(label: String, png: ByteArray?, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = SlateGray)
            Spacer(Modifier.height(8.dp))
            if (png != null) {
                val bmp = remember(png) { BitmapFactory.decodeByteArray(png, 0, png.size) }
                if (bmp != null) {
                    Box(
                        Modifier.size(120.dp).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(bmp.asImageBitmap(), contentDescription = label, modifier = Modifier.size(120.dp))
                    }
                } else {
                    Text("—", textAlign = TextAlign.Center, color = SlateGray)
                }
            } else {
                Text("preview unavailable", textAlign = TextAlign.Center, color = SlateGray)
            }
        }
    }
}
