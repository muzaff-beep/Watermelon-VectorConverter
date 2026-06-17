// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = WatermelonRed,
    onPrimary = PureWhite,
    secondary = FreshTeal,
    onSecondary = PureWhite,
    tertiary = DeepNavy,
    background = OffWhite,
    onBackground = Charcoal,
    surface = OffWhite,
    onSurface = Charcoal,
    surfaceVariant = SlateGray,
    error = WatermelonRed,
)

private val DarkColors = darkColorScheme(
    primary = WatermelonRed,
    onPrimary = PureWhite,
    secondary = FreshTeal,
    onSecondary = OffWhite,
    tertiary = DeepNavy,
    background = DeepCharcoal,
    onBackground = OffWhite,
    surface = Charcoal,
    onSurface = OffWhite,
    surfaceVariant = SlateGray,
    error = WatermelonRed,
)

@Composable
fun WatermelonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WatermelonTypography,
        content = content
    )
}
