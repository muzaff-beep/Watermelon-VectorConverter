// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.watermelon.converter.Routes
import com.watermelon.converter.data.model.HistoryStore
import com.watermelon.converter.util.ShareUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(nav: NavController) {
    val items by HistoryStore.items.collectAsState()
    val ctx = LocalContext.current
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { TextButton(onClick = { nav.navigate(Routes.PAGER) { popUpTo(Routes.PAGER) { inclusive = false } } }) { Text("Back") } },
                actions = { if (items.isNotEmpty()) TextButton(onClick = { HistoryStore.clear() }) { Text("Clear") } },
            )
        }
    ) { pad ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("No conversions yet this session.")
            }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            items(items, key = { it.id }) { rec ->
                ListItem(
                    headlineContent = { Text(rec.sourceName) },
                    supportingContent = {
                        Text(if (rec.ok) "Converted · ${fmt.format(Date(rec.timestampMillis))}"
                             else "Failed · ${rec.errorMessage ?: ""}")
                    },
                    trailingContent = {
                        if (rec.ok) TextButton(onClick = {
                            ShareUtils.copyToClipboard(ctx, "VectorDrawable", rec.vdXml)
                        }) { Text("Copy") }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
