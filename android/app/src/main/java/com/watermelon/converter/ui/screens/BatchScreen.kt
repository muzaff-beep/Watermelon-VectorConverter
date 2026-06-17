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
import com.watermelon.converter.Routes
import com.watermelon.converter.ui.sharedGraphViewModel
import com.watermelon.converter.viewmodel.BatchUiState
import com.watermelon.converter.viewmodel.BatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(nav: NavController, vm: BatchViewModel = nav.sharedGraphViewModel()) {
    val state by vm.state.collectAsState()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.convertZip(uri) }

    LaunchedEffect(state) {
        if (state is BatchUiState.Done) nav.navigate(Routes.EXPORT)
    }

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
                        LinearProgressIndicator(
                            progress = { p.done.toFloat() / p.total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${p.done} / ${p.total}  ·  ${p.currentName}")
                    } else {
                        CircularProgressIndicator()
                        Text("Reading archive…")
                    }
                    OutlinedButton(onClick = { vm.cancel() }) { Text("Cancel") }
                }
                is BatchUiState.Error -> {
                    Text("Batch failed", style = MaterialTheme.typography.titleLarge)
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { vm.reset() }) { Text("Try again") }
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
