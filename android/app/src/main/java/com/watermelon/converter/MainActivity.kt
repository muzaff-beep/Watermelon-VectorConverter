// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter

import android.os.Bundle
import com.watermelon.converter.logging.AppLogger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.watermelon.converter.ui.screens.*
import com.watermelon.converter.ui.theme.WatermelonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log("MainActivity", "onCreate start")
        setContent {
            val settingsVm: com.watermelon.converter.viewmodel.SettingsViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val settings by settingsVm.settings.collectAsState()
            val dark = when (settings.themeMode) {
                com.watermelon.converter.data.prefs.ThemeMode.LIGHT -> false
                com.watermelon.converter.data.prefs.ThemeMode.DARK -> true
                com.watermelon.converter.data.prefs.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            WatermelonTheme(darkTheme = dark) {
                Surface(modifier = Modifier) {
                    AppNavHost()
                }
            }
        }
        AppLogger.log("MainActivity", "onCreate done — UI set")
    }

    override fun onResume() {
        super.onResume()
        AppLogger.log("MainActivity", "onResume")
    }

    override fun onPause() {
        super.onPause()
        AppLogger.log("MainActivity", "onPause")
    }
}

object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val PREVIEW = "preview"
    const val BATCH = "batch"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@androidx.compose.runtime.Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.IMPORT) { ImportScreen(nav) }
        composable(Routes.PREVIEW) { PreviewScreen(nav) }
        composable(Routes.BATCH) { BatchScreen(nav) }
        composable(Routes.EXPORT) { ExportScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.HISTORY) { HistoryScreen(nav) }
    }
}
