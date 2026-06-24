// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.watermelon.converter.data.files.FileKind
import com.watermelon.converter.data.files.FileNode
import com.watermelon.converter.data.files.TypeFilter
import com.watermelon.converter.ui.components.FileIconWithLabel
import com.watermelon.converter.ui.components.FolderIcon
import com.watermelon.converter.ui.components.VectorPropertiesPanel
import com.watermelon.converter.ui.components.WatermelonLoader
import com.watermelon.converter.ui.theme.*
import com.watermelon.converter.util.StoragePermission
import com.watermelon.converter.viewmodel.FileManagerViewModel
import com.watermelon.converter.viewmodel.PreviewState
import com.watermelon.converter.viewmodel.TreeRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
private fun fmtDate(ms: Long) = dateFmt.format(Date(ms))
private fun fmtSize(b: Long) = when {
    b < 1024 -> "$b B"
    b < 1024 * 1024 -> "%.0f KB".format(b / 1024.0)
    else -> "%.1f MB".format(b / (1024.0 * 1024.0))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    nav: NavController,
    vm: FileManagerViewModel = viewModel(),
    settingsVm: com.watermelon.converter.viewmodel.SettingsViewModel = viewModel(),
) {
    val settings by settingsVm.settings.collectAsState()
    val hasPermission by vm.hasPermission.collectAsState()
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
    val properties by vm.properties.collectAsState()

    var fullScreen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingMove by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) vm.copyOrMoveSelectedTo(uri, pendingMove) }

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

    Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            // ── Header ──
            if (selectionMode) {
                SelectionTopBar(
                    count = selected.size,
                    onExit = { vm.exitSelection() },
                    onSelectAllSvg = { vm.selectAllSvg() },
                    onSelectAllXml = { vm.selectAllXml() },
                )
            } else {
                FilesTopBar(
                    query = query,
                    onQueryChange = { vm.setQuery(it) },
                    filter = filter,
                    onFilterChange = { svg, xml -> vm.setFilter(svg, xml) },
                )
            }

            // ── Content ──
            val showingSearch = query.isNotBlank()
            val displayRows = if (showingSearch) searchResults.map { TreeRow(it, 0, false) } else rows

            when {
                loading -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WatermelonLoader(size = 56.dp)
                    }
                }
                displayRows.isEmpty() -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (showingSearch) "No matches found." else "No SVG or XML files here.",
                            color = SlateGray,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    LazyColumn(Modifier.weight(1f)) {
                        items(displayRows, key = { it.node.file.absolutePath }) { row ->
                            FileRow(
                                row = row,
                                isMarked = marked.contains(row.node.file.absolutePath),
                                isSelected = selected.contains(row.node.file.absolutePath),
                                selectionMode = selectionMode,
                                onTap = {
                                    if (row.node.isDirectory) vm.toggleDir(row.node)
                                    else if (selectionMode) vm.toggleSelect(row.node)
                                    else vm.preview(row.node)
                                },
                                onLongPress = { if (!row.node.isDirectory) vm.startSelection(row.node) },
                                onMenuAction = { action ->
                                    when (action) {
                                        "rename" -> showRenameDialog = true.also { vm.startSelection(row.node) }
                                        "delete" -> showDeleteConfirm = true.also { vm.startSelection(row.node) }
                                        "copy" -> { vm.startSelection(row.node); pendingMove = false; folderPicker.launch(null) }
                                        "move" -> { vm.startSelection(row.node); pendingMove = true; folderPicker.launch(null) }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // ── Preview pane ──
            if (preview !is PreviewState.Empty && !selectionMode) {
                HorizontalDivider(color = Color(0xFFE2E8F0))
                PreviewPane(
                    state = preview,
                    isMarkable = previewedFile?.kind == FileKind.Svg,
                    isMarked = previewedFile?.let { marked.contains(it.file.absolutePath) } ?: false,
                    onToggleMark = { vm.toggleMarkPreviewed() },
                    onExpand = { fullScreen = true },
                    onClose = { vm.closePreview() },
                    properties = properties,
                    showProperties = settings.showFileProperties,
                )
            }

            // ── Marked action bar ──
            if (!selectionMode && marked.isNotEmpty()) {
                MarkedBar(
                    count = marked.size,
                    converting = converting,
                    onClear = { vm.clearMarks() },
                    onConvert = { vm.convertMarked() },
                )
            }
        }

        // ── FAB ──
        if (selectionMode && selected.isNotEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                FileOpsFab(
                    onZipShip = { vm.zipAndShipSelected() },
                    onMove = { pendingMove = true; folderPicker.launch(null) },
                    onCopy = { pendingMove = false; folderPicker.launch(null) },
                    onRename = { showRenameDialog = true },
                    onDelete = { showDeleteConfirm = true },
                )
            }
        }

        // ── Converting overlay ──
        if (converting) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WatermelonLoader(size = 72.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Converting…", color = PureWhite, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ── Fullscreen preview ──
    if (fullScreen && preview !is PreviewState.Empty) {
        FullScreenPreview(state = preview, onClose = { fullScreen = false })
    }

    // ── Dialogs ──
    convertResult?.let { result ->
        AlertDialog(
            onDismissRequest = { vm.dismissConvertResult() },
            title = { Text("Conversion complete") },
            text = {
                Column {
                    Text("${result.succeeded} succeeded, ${result.failed} failed.")
                    Spacer(Modifier.height(8.dp))
                    Text("Saved to:\n${result.savedTo}", style = MaterialTheme.typography.labelLarge, color = SlateGray)
                }
            },
            confirmButton = { TextButton(onClick = { vm.dismissConvertResult() }) { Text("OK") } },
        )
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

// ─────────────────────────────────────────────────────────────────────────────
// Top bars
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilesTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: TypeFilter,
    onFilterChange: (Boolean, Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            "Files",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        // Search pill
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search files and folders", color = SlateGray) },
            leadingIcon = { Text("⌕", fontSize = 18.sp, color = SlateGray) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FreshTeal,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Filter chips — SVG and XML
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterPill(
                label = "SVG",
                active = filter.showSvg,
                onClick = { onFilterChange(!filter.showSvg, filter.showXml) },
            )
            FilterPill(
                label = "XML",
                active = filter.showXml,
                onClick = { onFilterChange(filter.showSvg, !filter.showXml) },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
    HorizontalDivider(color = Color(0xFFE2E8F0))
}

@Composable
private fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    val border = if (active) null else androidx.compose.foundation.BorderStroke(1.5.dp, SlateGray.copy(alpha = 0.4f))
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) FreshTeal else Color.Transparent)
            .then(if (!active) Modifier.background(Color.Transparent) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (active) {
                Text("✓", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                label,
                color = if (active) PureWhite else SlateGray,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
            )
        }
    }
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
        title = { Text("$count selected", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            TextButton(onClick = onExit) { Text("Cancel", color = WatermelonRed) }
        },
        actions = {
            Box {
                TextButton(onClick = { menuOpen = true }) { Text("Select all", color = FreshTeal) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("All SVG in folder") }, onClick = { menuOpen = false; onSelectAllSvg() })
                    DropdownMenuItem(text = { Text("All XML in folder") }, onClick = { menuOpen = false; onSelectAllXml() })
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// File row
// ─────────────────────────────────────────────────────────────────────────────
