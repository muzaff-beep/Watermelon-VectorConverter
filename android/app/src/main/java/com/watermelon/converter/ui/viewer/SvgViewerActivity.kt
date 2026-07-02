// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Lightweight viewer Activity — opened via ACTION_VIEW on image/svg+xml.

package com.watermelon.converter.ui.viewer

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watermelon.converter.jni.SvgConverterNative

class SvgViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        setContent {
            MaterialTheme { SvgViewerScreen(uri = uri, onClose = { finish() }) }
        }
    }
}

@Composable
private fun SvgViewerScreen(uri: Uri?, onClose: () -> Unit) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var fileName by remember { mutableStateOf("") }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(uri) {
        if (uri == null) { loading = false; error = "No file provided."; return@LaunchedEffect }
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: throw Exception("Cannot read file")
            fileName = uri.lastPathSegment?.substringAfterLast('/') ?: ""
            val pngBytes = SvgConverterNative.nativeRenderSvgPreview(bytes, 1024)
            previewBitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF15181C))) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF2A9D8F)
            )
            error != null -> Text(
                text = "⚠ $error",
                color = Color(0xFFE63946),
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                fontSize = 14.sp
            )
            previewBitmap != null -> Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 8f)
                            offset += pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offset.x, translationY = offset.y
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC1C2128))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = fileName,
                color = Color(0xFF9AA5B1),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Text("✕", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
