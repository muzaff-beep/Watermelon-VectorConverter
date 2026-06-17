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
import com.watermelon.converter.viewmodel.ConversionViewModel
import com.watermelon.converter.viewmodel.ConvertUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(nav: NavController, vm: ConversionViewModel = nav.sharedGraphViewModel()) {
    val state by vm.state.collectAsState()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.convert(uri) }

    LaunchedEffect(state) {
        if (state is ConvertUiState.Done) nav.navigate(Routes.PREVIEW)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Import SVG") }) }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            when (val s = state) {
                is ConvertUiState.Working -> CircularProgressIndicator()
                is ConvertUiState.Error -> {
                    Text("Conversion failed", style = MaterialTheme.typography.titleLarge)
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { vm.reset() }) { Text("Try again") }
                }
                else -> {
                    Text("Pick an .svg file to convert.")
                    Button(
                        onClick = { picker.launch(arrayOf("image/svg+xml", "text/xml", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose SVG file") }
                }
            }
        }
    }
}
