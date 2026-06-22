// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.ui.components.WatermelonSlice
import com.watermelon.converter.ui.theme.DeepNavy
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.OffWhite
import com.watermelon.converter.ui.theme.PureWhite
import com.watermelon.converter.ui.theme.WatermelonRed

@Composable
fun HomeScreen(nav: NavController) {
    Box(
        Modifier
            .fillMaxSize()
            .background(OffWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── "we stand with" wordmark ──
            Text(
                text = "we\nstand\nwith",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    lineHeight = 60.sp,
                    color = DeepNavy,
                    textAlign = TextAlign.Center,
                ),
            )

            Spacer(Modifier.height(8.dp))

            // ── watermelon slice illustration ──
            WatermelonSlice(size = 200.dp)

            Spacer(Modifier.height(56.dp))

            // ── primary: Convert a single SVG ──
            Button(
                onClick = { nav.navigate(Routes.IMPORT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WatermelonRed,
                    contentColor = PureWhite,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    "Convert a single SVG",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── secondary: Batch convert a ZIP ──
            Button(
                onClick = { nav.navigate(Routes.BATCH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshTeal,
                    contentColor = PureWhite,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    "Batch convert a ZIP",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── history shortcut ──
            TextButton(onClick = { nav.navigate(Routes.HISTORY) }) {
                Text(
                    "View history",
                    color = DeepNavy.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.height(24.dp))

            // About link — logo will show here once digital_raven_logo.png is in res/drawable/
            TextButton(onClick = { nav.navigate(Routes.ABOUT) }) {
                Text(
                    "About this app",
                    color = DeepNavy.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}