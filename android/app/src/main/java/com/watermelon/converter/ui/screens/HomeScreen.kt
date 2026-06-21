// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.watermelon.converter.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Watermelon Vector Converter") },
            actions = {
                TextButton(onClick = { nav.navigate(Routes.HISTORY) }) { Text("History") }
            },
        )
    }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Convert SVG to Android VectorDrawable", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
            Text("Fully offline. No network.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { nav.navigate(Routes.IMPORT) }, modifier = Modifier.fillMaxWidth()) {
                Text("Convert a single SVG")
            }
            OutlinedButton(onClick = { nav.navigate(Routes.BATCH) }, modifier = Modifier.fillMaxWidth()) {
                Text("Batch convert a ZIP")
            }
        }
    }
}
