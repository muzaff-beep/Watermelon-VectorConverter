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
import com.watermelon.converter.viewmodel.ConversionViewModel
import com.watermelon.converter.viewmodel.ConvertUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    nav: NavController,
    convVm: ConversionViewModel = nav.sharedGraphViewModel(),
    batchVm: BatchViewModel = nav.sharedGraphViewModel(),
) {
    val convState by convVm.state.collectAsState()
    val batchState by batchVm.state.collectAsState()

    // Choose a destination tree (folder) via SAF.
    val treePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            (convState as? ConvertUiState.Done)?.let { convVm.export(treeUri, it.sourceName) }
            if (batchState is BatchUiState.Done) batchVm.export(treeUri)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Export") }) }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            val hasSingle = convState is ConvertUiState.Done
            val hasBatch = batchState is BatchUiState.Done
            Text(
                when {
                    hasBatch -> "Batch ready to export as ZIP."
                    hasSingle -> "VectorDrawable ready to export."
                    else -> "Nothing to export."
                },
                style = MaterialTheme.typography.titleLarge,
            )
            if (hasSingle || hasBatch) {
                Button(onClick = { treePicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose destination folder")
                }
            }
            OutlinedButton(onClick = { nav.navigate(Routes.HOME) }, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}
