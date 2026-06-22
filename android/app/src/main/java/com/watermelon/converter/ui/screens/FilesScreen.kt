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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
    val selectionMode by vm.selectionMode.collectAsState()
    val selected by vm.selected.collectAsState()
    val opStatus by vm.opStatus.collectAsState()
    val loading by vm.loading.collectAsState()
    var fullScreen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingMove by remember { mutableStateOf(false) } // true=move, false=copy

    // Folder picker for Move/Copy destination (SAF tree).
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) vm.copyOrMoveSelectedTo(uri, pendingMove) }

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
                if (selectionMode) {
                    SelectionTopBar(
                        count = selected.size,
                        onExit = { vm.exitSelection() },
                        onSelectAllSvg = { vm.selectAllSvg() },
                        onSelectAllXml = { vm.selectAllXml() },
                    )
                } else {
                    TopAppBar(title = { Text("Files") })
                    SearchBar(query = query, onQueryChange = { vm.setQuery(it) })
                    FilterBar(
                        showSvg = filter.showSvg,
                        showXml = filter.showXml,
                        onChange = { svg, xml -> vm.setFilter(svg, xml) },
                    )
                }
                HorizontalDivider()
            }
        },
        floatingActionButton = {
            if (selectionMode && selected.isNotEmpty()) {
                FileOpsFab(
                    onZipShip = { vm.zipAndShipSelected() },
                    onMove = { pendingMove = true; folderPicker.launch(null) },
                    onCopy = { pendingMove = false; folderPicker.launch(null) },
                    onRename = { showRenameDialog = true },
                    onDelete = { showDeleteConfirm = true },
                )
            }
        },
        bottomBar = {
            if (!selectionMode && marked.isNotEmpty()) {
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

            if (loading) {
                Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    com.watermelon.converter.ui.components.WatermelonLoader(size = 56.dp)
                }
            } else if (displayRows.isEmpty()) {
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
                            isMarked = marked.contains(row.node.file.absolutePath),
                            isSelected = selected.contains(row.node.file.absolutePath),
                            selectionMode = selectionMode,
                            onTapDir = { vm.toggleDir(row.node) },
                            onTapFile = {
                                if (selectionMode) vm.toggleSelect(row.node)
                                else vm.preview(row.node)
                            },
                            onLongPressFile = { vm.startSelection(row.node) },
                        )
                    }
                }
                if (!selectionMode && preview !is PreviewState.Empty) {
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

    if (showRenameDialog) {
        RenameDialog(
            count = selected.size,
            onConfirm = { base -> vm.renameSelected(base); showRenameDialog = false },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} file${if (selected.size == 1) "" else "s"}?") },
            text = { Text("This permanently deletes the selected file(s) from storage. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteSelected(); showDeleteConfirm = false }) {
                    Text("Delete", color = WatermelonRed)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    if (converting) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                com.watermelon.converter.ui.components.WatermelonLoader(size = 72.dp)
                Spacer(Modifier.height(12.dp))
                Text("Converting\u2026", color = Color.White)
            }
        }
    }

    opStatus?.let { status ->
        LaunchedEffect(status) {
            kotlinx.coroutines.delay(2500)
            vm.dismissOpStatus()
        }
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Snackbar { Text(status) }
        }
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
 * Long-press a file to enter selection mode (for file operations); tap behavior
 * depends on whether selection mode is active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TreeRowItem(
    row: TreeRow,
    isMarked: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    onTapDir: () -> Unit,
    onTapFile: () -> Unit,
    onLongPressFile: () -> Unit,
) {
    val node = row.node
    val bg = when {
        isSelected -> FreshTeal.copy(alpha = 0.22f)
        isMarked -> WatermelonRed.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (node.isDirectory) onTapDir() else onTapFile() },
                onLongClick = { if (!node.isDirectory) onLongPressFile() },
            )
            .background(bg)
            .padding(start = 4.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(row.depth) {
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .width(2.dp)
                    .height(20.dp)
                    .background(SlateGray.copy(alpha = 0.3f)),
            )
        }
        // In selection mode, files show a checkbox; directories never selectable.
        if (selectionMode && !node.isDirectory) {
            Checkbox(checked = isSelected, onCheckedChange = { onTapFile() })
            Spacer(Modifier.width(4.dp))
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
        // Marked files show a red bookmark, pinned on the row. Marking happens
        // from the preview panel, not here.
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
        is PreviewState.Loading -> com.watermelon.converter.ui.components.WatermelonLoader(size = 44.dp)
        is PreviewState.SvgImage -> {
            val bmp = remember(state.png) { BitmapFactory.decodeByteArray(state.png, 0, state.png.size) }
            if (bmp != null) {
                // Always white behind the vector preview, regardless of theme.
                Box(
                    Modifier.background(Color.White).padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(bmp.asImageBitmap(), contentDescription = state.name)
                }
            } else Text("Could not decode preview")
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
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onExit: () -> Unit,
    onSelectAllSvg: () -> Unit,
    onSelectAllXml: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = {
            TextButton(onClick = onExit) { Text("Cancel") }
        },
        actions = {
            Box {
                TextButton(onClick = { menuOpen = true }) { Text("Select all") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("All SVG in folder") },
                        onClick = { menuOpen = false; onSelectAllSvg() },
                    )
                    DropdownMenuItem(
                        text = { Text("All XML in folder") },
                        onClick = { menuOpen = false; onSelectAllXml() },
                    )
                }
            }
        },
    )
}

@Composable
private fun FileOpsFab(
    onZipShip: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { open = true }) {
            Text("\u22EE", style = MaterialTheme.typography.titleLarge) // vertical ellipsis
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Zip & Ship (convert)") },
                onClick = { open = false; onZipShip() })
            DropdownMenuItem(text = { Text("Move\u2026") },
                onClick = { open = false; onMove() })
            DropdownMenuItem(text = { Text("Copy\u2026") },
                onClick = { open = false; onCopy() })
            DropdownMenuItem(text = { Text("Rename\u2026") },
                onClick = { open = false; onRename() })
            DropdownMenuItem(text = { Text("Delete\u2026") },
                onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun RenameDialog(count: Int, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count > 1) "Rename $count files" else "Rename file") },
        text = {
            Column {
                if (count > 1) {
                    Text(
                        "The first file gets this name; the rest are numbered " +
                            "(name, name_1, name_2\u2026). Extensions are preserved.",
                        style = MaterialTheme.typography.labelLarge,
                        color = SlateGray,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("New name (no extension)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
