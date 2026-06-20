// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.watermelon.converter.data.files.FileKind
import com.watermelon.converter.data.files.FileNode
import com.watermelon.converter.ui.theme.FreshTeal
import com.watermelon.converter.ui.theme.SlateGray
import com.watermelon.converter.ui.theme.WatermelonRed
import com.watermelon.converter.util.StoragePermission
import com.watermelon.converter.viewmodel.FileManagerViewModel
import com.watermelon.converter.viewmodel.PreviewState
import com.watermelon.converter.viewmodel.TreeRow
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(nav: NavController, vm: FileManagerViewModel = viewModel()) {
    val hasPermission by vm.hasPermission.collectAsState()
    val currentDir by vm.currentDir.collectAsState()
    val rows by vm.rows.collectAsState()
    val filter by vm.filter.collectAsState()
    val marked by vm.marked.collectAsState()
    val preview by vm.preview.collectAsState()
    val previewedFile by vm.previewedFile.collectAsState()
    val query by vm.query.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val converting by vm.converting.collectAsState()
    val convertResult by vm.convertResult.collectAsState()
    var fullScreen by remember { mutableStateOf(false) }

    // Re-check permission when returning from the system Settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.recheckPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!hasPermission) {
        PermissionGate()
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Files") })
                SearchBar(query = query, onQueryChange = { vm.setQuery(it) })
                FilterBar(
                    showSvg = filter.showSvg,
                    showXml = filter.showXml,
                    onChange = { svg, xml -> vm.setFilter(svg, xml) },
                )
                HorizontalDivider()
            }
        },
        bottomBar = {
            if (marked.isNotEmpty()) {
                MarkedActionBar(
                    count = marked.size,
                    converting = converting,
                    onClear = { vm.clearMarks() },
                    onConvert = { vm.convertMarked() },
                )
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            val showingSearch = query.isNotBlank()
            val displayRows = if (showingSearch) {
                searchResults.map { TreeRow(it, 0, false) }
            } else rows

            if (displayRows.isEmpty()) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        if (showingSearch) "No matches in this folder." else "Empty folder.",
                        color = SlateGray,
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(displayRows, key = { it.node.file.absolutePath }) { row ->
                        TreeRowItem(
                            row = row,
                            isMarked = vm.isMarked(row.node),
                            onTapDir = { vm.toggleDir(row.node) },
                            onTapFile = { vm.preview(row.node) },
                        )
                    }
                }
                if (preview !is PreviewState.Empty) {
                    HorizontalDivider()
                    PreviewPane(
                        state = preview,
                        isMarkable = previewedFile?.kind == com.watermelon.converter.data.files.FileKind.Svg,
                        isMarked = previewedFile?.let { marked.contains(it.file.absolutePath) } ?: false,
                        onToggleMark = { vm.toggleMarkPreviewed() },
                        onExpand = { fullScreen = true },
                        onClose = { vm.closePreview() },
                    )
                }
            }
        }
    }

    if (fullScreen && preview !is PreviewState.Empty) {
        FullScreenPreview(state = preview, onClose = { fullScreen = false })
    }

    convertResult?.let { result ->
        ConvertResultDialog(result = result, onDismiss = { vm.dismissConvertResult() })
    }
}

@Composable
private fun PermissionGate() {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* handled by ON_RESUME recheck */ }

    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Full storage access needed",
                style = MaterialTheme.typography.titleLarge,
                color = WatermelonRed,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "The file manager browses your whole device storage so you can " +
                    "find SVG and XML files anywhere, not just in one picked folder.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { launcher.launch(StoragePermission.requestIntent(ctx)) }) {
                Text("Grant access")
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search this folder…") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun FilterBar(showSvg: Boolean, showXml: Boolean, onChange: (Boolean, Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Show:", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showSvg, onCheckedChange = { onChange(it, showXml) })
            Text("SVG")
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showXml, onCheckedChange = { onChange(showSvg, it) })
            Text("XML")
        }
    }
}

@Composable
private fun MarkedActionBar(count: Int, converting: Boolean, onClear: () -> Unit, onConvert: () -> Unit) {
    BottomAppBar {
        Text("  $count marked", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onClear, enabled = !converting) { Text("Clear") }
        Button(onClick = onConvert, enabled = !converting, modifier = Modifier.padding(end = 12.dp)) {
            if (converting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Convert")
            }
        }
    }
}

/**
 * Redesigned tree row: more vertical breathing room, a depth "rail" (a thin
 * colored line per nesting level instead of bare indentation) so depth reads
 * at a glance, and larger glyphs for visual hierarchy between folders/files.
 */
@Composable
private fun TreeRowItem(
    row: TreeRow,
    isMarked: Boolean,
    onTapDir: () -> Unit,
    onTapFile: () -> Unit,
) {
    val node = row.node
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { if (node.isDirectory) onTapDir() else onTapFile() }
            .background(if (isMarked) WatermelonRed.copy(alpha = 0.08f) else Color.Transparent)
            .padding(start = 4.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // depth rail: one thin vertical tick per nesting level
        repeat(row.depth) {
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .width(2.dp)
                    .height(20.dp)
                    .background(SlateGray.copy(alpha = 0.3f)),
            )
        }
        val glyph = when (node.kind) {
            FileKind.Directory -> if (row.expanded) "\u25BC" else "\u25B6"
            FileKind.Svg -> "\u25C6"
            FileKind.Xml -> "\u25C7"
            FileKind.Other -> "\u00B7"
        }
        val glyphColor = when (node.kind) {
            FileKind.Directory -> FreshTeal
            FileKind.Svg -> WatermelonRed
            FileKind.Xml -> SlateGray
            FileKind.Other -> SlateGray
        }
        Text(glyph, color = glyphColor, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                node.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!node.isDirectory) {
                Text(
                    humanSize(node.sizeBytes),
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateGray,
                )
            }
        }
        // Marked files show a red bookmark, pinned on the row. Marking itself
        // happens from the preview panel, not here.
        if (isMarked) {
            Text("\uD83D\uDD16", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    return String.format("%.2f MB", kb / 1024.0)
}

@Composable
private fun PreviewPane(
    state: PreviewState,
    isMarkable: Boolean,
    isMarked: Boolean,
    onToggleMark: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (state) {
                    is PreviewState.SvgImage -> state.name
                    is PreviewState.Failed -> state.name
                    else -> "Preview"
                },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Bookmark = mark this file for conversion. Only SVGs are markable.
            if (isMarkable) {
                TextButton(onClick = onToggleMark) {
                    Text(if (isMarked) "\uD83D\uDD16 Marked" else "\uD83D\uDD16 Mark")
                }
            }
            TextButton(onClick = onExpand) { Text("Expand") }
            TextButton(onClick = onClose) { Text("Close") }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Crossfade(targetState = state, label = "preview") { PreviewContent(it) }
        }
    }
}

@Composable
private fun FullScreenPreview(state: PreviewState, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preview", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text("Close") }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 600.dp), contentAlignment = Alignment.Center) {
                    PreviewContent(state)
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(state: PreviewState) {
    when (state) {
        is PreviewState.Loading -> CircularProgressIndicator()
        is PreviewState.SvgImage -> {
            val bmp = remember(state.png) { BitmapFactory.decodeByteArray(state.png, 0, state.png.size) }
            if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = state.name)
            else Text("Could not decode preview")
        }
        is PreviewState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
        PreviewState.Empty -> {}
    }
}

@Composable
private fun ConvertResultDialog(
    result: com.watermelon.converter.viewmodel.ConvertMarkedResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conversion complete") },
        text = {
            Column {
                Text("${result.succeeded} succeeded, ${result.failed} failed.")
                Spacer(Modifier.height(8.dp))
                Text("Saved to:\n${result.outputZip.path}", style = MaterialTheme.typography.labelLarge)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
