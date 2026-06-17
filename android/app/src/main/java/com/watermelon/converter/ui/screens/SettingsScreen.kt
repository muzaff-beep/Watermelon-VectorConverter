// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.watermelon.converter.logging.AppLogger
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.watermelon.converter.data.prefs.ThemeMode
import com.watermelon.converter.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = { nav.popBackStack() }) { Text("Back") } },
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Preview resolution", style = MaterialTheme.typography.titleLarge)
            Text("${settings.previewPx} px")
            Slider(
                value = settings.previewPx.toFloat(),
                onValueChange = { vm.setPreviewPx(it.roundToInt()) },
                valueRange = 64f..1024f,
                steps = 0,
            )

            HorizontalDivider()

            Text("Theme", style = MaterialTheme.typography.titleLarge)
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = settings.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                    ).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = settings.themeMode == mode, onClick = { vm.setThemeMode(mode) })
                    Spacer(Modifier.width(8.dp))
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }

            HorizontalDivider()

            Text("Diagnostics", style = MaterialTheme.typography.titleLarge)
            val ctx = LocalContext.current
            Button(
                onClick = {
                    val saved = AppLogger.dump("manual save from Settings")
                    Toast.makeText(
                        ctx,
                        if (saved != null) "Saved to Downloads/WVGC/$saved" else "Could not save logs",
                        Toast.LENGTH_LONG,
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save logs to Downloads") }
        }
    }
}
