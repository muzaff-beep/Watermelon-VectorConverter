// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.viewmodel.BatchUiState
import com.watermelon.converter.viewmodel.BatchViewModel
import com.watermelon.converter.ui.components.SeedBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(nav: NavController, vm: BatchViewModel = nav.sharedGraphViewModel()) {
    val state by vm.state.collectAsState()
    val reportSaveState by vm.reportSaveState.collectAsState()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.convertZip(uri) }

    Scaffold(topBar = { TopAppBar(title = { Text("Batch convert") }) }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            when (val s = state) {
                is BatchUiState.Working -> {
                    val p = s.progress
                    if (p != null && p.total > 0) {
                        // current file (snaps 0->100% per file) then total batch
                        SeedBar(
                            progress = p.fileFraction,
                            label = "Current: ${p.currentName}",
                        )
                        Spacer(Modifier.height(20.dp))
                        SeedBar(
                            progress = p.totalFraction,
                            label = "Total batch",
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${p.done} of ${p.total} files",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    } else {
                        CircularProgressIndicator()
                        Text("Reading archive…")
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { vm.cancel() }) { Text("Cancel") }
                }
                is BatchUiState.Error -> {
                    Text("Batch failed", style = MaterialTheme.typography.titleLarge)
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { vm.reset() }) { Text("Try again") }
                }
                is BatchUiState.Done -> {
                    com.watermelon.converter.ui.components.ReportPanel(
                        report = s.report,
                        onSaveReport = { vm.saveReport() },
                        onDismiss = { vm.dismissReport(); nav.popBackStack() },
                        saveState = reportSaveState,
                    )
                }
                else -> {
                    Text("Pick a .zip of SVG files.")
                    Button(
                        onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose ZIP file") }
                }
            }
        }
    }
}