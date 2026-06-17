// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wvgc_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val previewPx: Int = 256,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val PREVIEW_PX = intPreferencesKey("preview_px")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            previewPx = (p[Keys.PREVIEW_PX] ?: 256).coerceIn(16, 2048),
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME_MODE] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
        )
    }

    suspend fun setPreviewPx(px: Int) {
        context.dataStore.edit { it[Keys.PREVIEW_PX] = px.coerceIn(16, 2048) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }
}
