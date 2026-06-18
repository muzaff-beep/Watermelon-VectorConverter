// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.data.model.BatchReport
import com.watermelon.converter.ui.components.SeedBar
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.WatermelonRed
import com.watermelon.converter.viewmodel.BatchUiState
import com.watermelon.converter.viewmodel.BatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(nav: NavController, vm: BatchViewModel = nav.sharedGraphViewModel()) {
    val state by vm.state.collectAsState()
    val report = (state as? BatchUiState.Done)?.report

    Scaffold(
        topBar = { TopAppBar(title = { Text("Conversion report") }) },
        bottomBar = {
            if (report != null) {
                BottomAppBar {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        vm.reset()
                        nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    }) { Text("Done") }
                    Button(
                        onClick = { nav.navigate(Routes.EXPORT) },
                        modifier = Modifier.padding(end = 12.dp),
                    ) { Text("Export ZIP") }
                }
            }
        },
    ) { pad ->
        if (report == null) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("No report available.")
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val rate = if (report.total > 0) report.succeeded.toFloat() / report.total else 0f
                SeedBar(progress = rate, label = "Success rate")
                Spacer(Modifier.height(12.dp))
            }
            item { SummaryGrid(report) }
            if (report.rejected.isNotEmpty()) {
                item {
                    Text(
                        "Rejected files (${report.rejected.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = WatermelonRed,
                    )
                }
                items(report.rejected, key = { it.name }) { f ->
                    ListItem(
                        headlineContent = { Text(f.name) },
                        supportingContent = {
                            Text(buildString {
                                f.errorCode?.let { append("[$it] ") }
                                append(f.errorMessage ?: "unknown reason")
                            })
                        },
                    )
                    HorizontalDivider()
                }
            } else {
                item {
                    Text(
                        "All files converted successfully.",
                        style = MaterialTheme.typography.titleLarge,
                        color = FreshTeal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryGrid(report: BatchReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatRow("Total files", report.total.toString())
        StatRow("Succeeded", report.succeeded.toString())
        StatRow("Failed", report.failed.toString())
        StatRow("Input size", humanSize(report.inputBytes))
        StatRow("Output size", humanSize(report.outputBytes))
        StatRow("Time", formatDuration(report.durationMillis))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), color = FreshTeal)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    return String.format("%.2f MB", kb / 1024.0)
}

private fun formatDuration(ms: Long): String {
    if (ms < 1000) return "$ms ms"
    val s = ms / 1000.0
    return String.format("%.1f s", s)
}
