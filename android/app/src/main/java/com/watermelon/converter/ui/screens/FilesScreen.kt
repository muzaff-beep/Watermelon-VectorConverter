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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    row: TreeRow,
    isMarked: Boolean,
    isSelected: Boolean,
    selectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onMenuAction: (String) -> Unit,
) {
    val node = row.node
    var menuOpen by remember { mutableStateOf(false) }
    val indentDp = (row.depth * 16).dp

    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .background(
                when {
                    isSelected -> FreshTeal.copy(alpha = 0.10f)
                    isMarked -> WatermelonRed.copy(alpha = 0.06f)
                    else -> Color.Transparent
                }
            )
            .padding(start = 16.dp + indentDp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Selection checkbox
        if (selectionMode && !node.isDirectory) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onTap() },
                colors = CheckboxDefaults.colors(checkedColor = FreshTeal),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }

        // Icon
        when (node.kind) {
            FileKind.Directory -> FolderIcon(size = 18.dp)
            FileKind.Svg -> FileIconWithLabel("SVG", FreshTeal, size = 18.dp)
            FileKind.Xml -> FileIconWithLabel("XML", DeepNavy, size = 18.dp)
            FileKind.Other -> FileIconWithLabel("—", SlateGray, size = 18.dp)
        }

        Spacer(Modifier.width(14.dp))

        // Name + meta
        Column(Modifier.weight(1f)) {
            Text(
                node.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(fmtDate(node.lastModified))
                    if (!node.isDirectory && node.sizeBytes > 0) {
                        append(" • ")
                        append(fmtSize(node.sizeBytes))
                    }
                },
                fontSize = 12.sp,
                color = SlateGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Marked = red bookmark (queued for conversion).
        // Visually distinct from selection (teal checkbox).
        if (isMarked) {
            Spacer(Modifier.width(6.dp))
            Text(
                "\uD83D\uDD16",
                fontSize = 18.sp,
                color = WatermelonRed,
            )
        }

        // Three-dot menu
        if (!selectionMode) {
            Spacer(Modifier.width(4.dp))
            Box {
                TextButton(
                    onClick = { menuOpen = true },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(32.dp),
                ) {
                    Text("⋮", fontSize = 18.sp, color = SlateGray)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!node.isDirectory) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onMenuAction("rename") })
                        DropdownMenuItem(text = { Text("Copy") }, onClick = { menuOpen = false; onMenuAction("copy") })
                        DropdownMenuItem(text = { Text("Move") }, onClick = { menuOpen = false; onMenuAction("move") })
                        DropdownMenuItem(text = { Text("Delete", color = WatermelonRed) }, onClick = { menuOpen = false; onMenuAction("delete") })
                    }
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom bars
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarkedBar(count: Int, converting: Boolean, onClear: () -> Unit, onConvert: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$count marked for conversion",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear, enabled = !converting) {
            Text("Clear", color = SlateGray)
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onConvert,
            enabled = !converting,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = WatermelonRed),
        ) {
            if (converting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = PureWhite)
            else Text("Convert", color = PureWhite, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = Color(0xFFE2E8F0))
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB
// ─────────────────────────────────────────────────────────────────────────────

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
        FloatingActionButton(
            onClick = { open = true },
            containerColor = WatermelonRed,
            contentColor = PureWhite,
            shape = CircleShape,
        ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Light) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Zip & Ship (convert)") }, onClick = { open = false; onZipShip() })
            DropdownMenuItem(text = { Text("Move…") }, onClick = { open = false; onMove() })
            DropdownMenuItem(text = { Text("Copy…") }, onClick = { open = false; onCopy() })
            DropdownMenuItem(text = { Text("Rename…") }, onClick = { open = false; onRename() })
            DropdownMenuItem(text = { Text("Delete…") }, onClick = { open = false; onDelete() })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview pane
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PreviewPane(
    state: PreviewState,
    isMarkable: Boolean,
    isMarked: Boolean,
    onToggleMark: () -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    properties: com.watermelon.converter.data.model.VectorProperties? = null,
    showProperties: Boolean = true,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 520.dp)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (state) {
                    is PreviewState.SvgImage -> state.name
                    is PreviewState.Failed -> state.name
                    else -> "Preview"
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isMarkable) {
                TextButton(onClick = onToggleMark) {
                    Text(
                        if (isMarked) "🔖 Marked" else "🔖 Mark",
                        fontSize = 13.sp,
                        color = if (isMarked) WatermelonRed else SlateGray,
                    )
                }
            }
            TextButton(onClick = onExpand) { Text("Expand", fontSize = 13.sp, color = FreshTeal) }
            TextButton(onClick = onClose) { Text("Close", fontSize = 13.sp, color = SlateGray) }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = state, label = "preview") { s ->
                when (s) {
                    is PreviewState.Loading -> WatermelonLoader(size = 44.dp)
                    is PreviewState.SvgImage -> {
                        val bmp = remember(s.png) { BitmapFactory.decodeByteArray(s.png, 0, s.png.size) }
                        if (bmp != null) {
                            Box(Modifier.background(Color.White).padding(8.dp), contentAlignment = Alignment.Center) {
                                Image(bmp.asImageBitmap(), contentDescription = s.name)
                            }
                        }
                    }
                    is PreviewState.Failed -> Text(s.message, color = WatermelonRed, fontSize = 13.sp)
                    PreviewState.Empty -> {}
                }
            }
        }
        if (showProperties && properties != null) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(Modifier.height(8.dp))
            VectorPropertiesPanel(properties)
        }
    }
}

@Composable
private fun FullScreenPreview(state: PreviewState, onClose: () -> Unit) {
    // Full-screen overlay with white canvas, image fills the screen, close button at top.
    // Uses fillMaxSize so the image actually expands rather than opening at the same size.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            when (val s = state) {
                is PreviewState.SvgImage -> {
                    val bmp = remember(s.png) {
                        BitmapFactory.decodeByteArray(s.png, 0, s.png.size)
                    }
                    if (bmp != null) {
                        // Image fills available space, maintaining aspect ratio
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = s.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                is PreviewState.Failed -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(s.message, color = WatermelonRed, textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        WatermelonLoader(size = 56.dp)
                    }
                }
            }

            // Close button pinned top-right
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(DeepNavy.copy(alpha = 0.75f))
                    .clickable { onClose() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("✕  Close", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // File name pinned at bottom
            if (state is PreviewState.SvgImage) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(DeepNavy.copy(alpha = 0.55f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.name, color = PureWhite, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

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
                        "First file keeps this name; the rest are numbered. Extensions are preserved.",
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
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
            ) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SlateGray) } },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission gate
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionGate() {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Storage access needed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepNavy)
            Spacer(Modifier.height(12.dp))
            Text(
                "The file manager needs access to browse your storage for SVG and XML files.",
                textAlign = TextAlign.Center,
                color = SlateGray,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(StoragePermission.requestIntent(ctx)) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = FreshTeal),
            ) { Text("Grant access", fontWeight = FontWeight.Bold) }
        }
    }
}
