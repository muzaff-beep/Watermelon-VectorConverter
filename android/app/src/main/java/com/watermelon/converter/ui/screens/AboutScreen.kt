// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.ui.components.WatermelonSlice
import com.watermelon.converter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(nav: NavController) {
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.SemiBold, color = DeepNavy) },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) {
                        Text("‹ Back", color = FreshTeal, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OffWhite),
            )
        },
        containerColor = OffWhite,
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Digital Raven full logo ──
            // TODO: drop digital_raven_logo.png into android/app/src/main/res/drawable/
            // then replace this placeholder with:
            //   Image(painterResource(R.drawable.digital_raven_logo), ...)
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepNavy),
                contentAlignment = Alignment.Center,
            ) {
                com.watermelon.converter.ui.components.WatermelonSlice(size = 120.dp)
            }

            Spacer(Modifier.height(32.dp))

            // ── Developer credit ──
            Text(
                "Developed by",
                fontSize = 13.sp,
                color = SlateGray,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Suhail Muzaffari",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Digital Raven",
                fontSize = 14.sp,
                color = FreshTeal,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(16.dp))

            // ── Tappable email ──
            Text(
                "so.muzaff@gmail.com",
                fontSize = 15.sp,
                color = FreshTeal,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:so.muzaff@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Watermelon Vector Converter")
                    }
                    ctx.startActivity(intent)
                },
            )

            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(Modifier.height(32.dp))

            // ── App info ──
            Text(
                "Watermelon Vector Converter",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Version ${"0.1.0"}",
                fontSize = 13.sp,
                color = SlateGray,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "An offline-first SVG to Android VectorDrawable converter. " +
                "No internet, no cloud, no tracking. " +
                "Converts on-device using a native Rust engine.",
                fontSize = 14.sp,
                color = SlateGray,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(40.dp))

            // ── Small watermelon as a decorative closer ──
            WatermelonSlice(size = 64.dp)

            Spacer(Modifier.height(16.dp))
            Text(
                "© 2026 Suhail Muzaffari. All rights reserved.",
                fontSize = 11.sp,
                color = SlateGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Proprietary and source-available.",
                fontSize = 11.sp,
                color = SlateGray.copy(alpha = 0.6f),
            )
        }
    }
}