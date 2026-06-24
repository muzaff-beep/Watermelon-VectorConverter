// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.watermelon.converter.R
import com.watermelon.converter.ui.components.WatermelonSlice
import com.watermelon.converter.ui.theme.*

private val STACK = listOf(
    "Kotlin", "Jetpack Compose", "Material3",
    "Rust", "JNI", "resvg", "libsodium", "vodozemac",
)

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background),
            )
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── 1. Digital Raven logo ──────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.digital_raven_hq),
                contentDescription = "Digital Raven — High-Tech Solutions and Services",
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit,
            )

            Spacer(Modifier.height(36.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(Modifier.height(28.dp))

            // ── 2. Stack ───────────────────────────────────────────────────
            Text(
                "Built with",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateGray,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(14.dp))

            // Wrap chips in rows
            val rows = STACK.chunked(3)
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    row.forEach { tech ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DeepNavy.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                tech,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(Modifier.height(28.dp))

            // ── 3. Lead Programmer credit ──────────────────────────────────
            Text(
                "Lead Programmer",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateGray,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Suhail Muzaffari",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DeepNavy,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "so.muzaff@gmail.com",
                fontSize = 15.sp,
                color = FreshTeal,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable {
                    ctx.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:so.muzaff@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Watermelon Vector Converter")
                        }
                    )
                },
            )

            Spacer(Modifier.height(40.dp))

            // ── Decorative closer ──────────────────────────────────────────
            WatermelonSlice(size = 56.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "© 2026 Suhail Muzaffari · All rights reserved.",
                fontSize = 11.sp,
                color = SlateGray.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Proprietary and source-available.",
                fontSize = 11.sp,
                color = SlateGray.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}