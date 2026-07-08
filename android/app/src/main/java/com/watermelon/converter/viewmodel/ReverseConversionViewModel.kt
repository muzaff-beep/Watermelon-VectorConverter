// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.converter.data.model.HistoryStore
import com.watermelon.converter.data.prefs.SettingsRepository
import com.watermelon.converter.data.repository.FileRepository
import com.watermelon.converter.jni.ConversionException
import com.watermelon.converter.jni.RealSvgConverter
import com.watermelon.converter.jni.SvgConverter
import com.watermelon.converter.jni.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reverse direction of ConversionViewModel: VectorDrawable XML -> SVG.
 * Mirrors ConversionViewModel's state machine exactly, but with honestly
 * named fields for this direction (svgXml holds the *output* SVG string,
 * not a VD XML string) — kept as a separate ViewModel, consistent with the
 * FFI layer's convert_vd/convert_vd_zip split, so the shipped SVG->XML path
 * is never touched by this addition.
 */
sealed interface ReverseConvertUiState {
    data object Idle : ReverseConvertUiState
    data object Working : ReverseConvertUiState
    @Suppress("ArrayInDataClass")
    data class Done(
        val sourceName: String,
        val svgXml: String,
        val vdPreviewPng: ByteArray?,   // preview of the ORIGINAL VD input
        val svgPreviewPng: ByteArray?,  // preview of the CONVERTED SVG output
        val analysisJson: String? = null,
    ) : ReverseConvertUiState
    data class Error(val message: String) : ReverseConvertUiState
}

class ReverseConversionViewModel(
    app: Application,
    private val native: SvgConverter,
) : AndroidViewModel(app) {

    constructor(app: Application) : this(app, RealSvgConverter)

    private val repo = FileRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)

    private val _state = MutableStateFlow<ReverseConvertUiState>(ReverseConvertUiState.Idle)
    val state: StateFlow<ReverseConvertUiState> = _state.asStateFlow()

    private var lastSvgXml: String? = null
    private var lastSourceName: String? = null

    fun convert(uri: Uri) {
        _state.value = ReverseConvertUiState.Working
        viewModelScope.launch {
            val px = runCatching { settingsRepo.settings.first().previewPx }.getOrDefault(256)
            var name = "image.xml"
            try {
                val result = withContext(Dispatchers.IO) {
                    name = repo.displayName(uri)
                    val vdBytes = repo.readBytes(uri)
                    require(vdBytes.isNotEmpty()) { "empty file" }
                    val svg = native.convertVd(vdBytes)
                    val vdPngDef = async { runCatching { native.renderVdPreview(String(vdBytes), px) }.getOrNull() }
                    val svgPngDef = async { runCatching { native.renderSvgPreview(svg.toByteArray(), px) }.getOrNull() }
                    val analysisDef = async { runCatching { native.analyzeVdVector(vdBytes) }.getOrNull() }
                    lastSvgXml = svg
                    lastSourceName = name
                    ReverseConvertUiState.Done(
                        sourceName = name,
                        svgXml = svg,
                        vdPreviewPng = vdPngDef.await(),
                        svgPreviewPng = svgPngDef.await(),
                        analysisJson = analysisDef.await(),
                    )
                }
                HistoryStore.add(result.sourceName, result.svgXml, ok = true)
                _state.value = result
            } catch (e: ConversionException) {
                val msg = e.userMessage(getApplication())
                HistoryStore.add(name, "", ok = false, error = msg)
                _state.value = ReverseConvertUiState.Error(msg)
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                HistoryStore.add(name, "", ok = false, error = msg)
                _state.value = ReverseConvertUiState.Error(msg)
            }
        }
    }

    /**
     * Export the converted SVG to the user's chosen output destination.
     * Mirrors ConversionViewModel.export exactly for this direction.
     */
    fun export(treeUri: Uri? = null) {
        val svg  = lastSvgXml ?: return
        val name = lastSourceName ?: "image.xml"
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val destUri = settingsRepo.settings.first().outputDestinationUri
                if (destUri != null) {
                    com.watermelon.converter.util.OutputDestination.write(
                        getApplication(), svg.toByteArray(),
                        repo.svgNameFor(name), destUri, mime = "image/svg+xml",
                    )
                } else if (treeUri != null) {
                    repo.writeToTree(treeUri, repo.svgNameFor(name), "image/svg+xml", svg.toByteArray())
                }
            }
        }
    }

    fun currentSvg(): String? = lastSvgXml

    fun reset() {
        _state.value = ReverseConvertUiState.Idle
        lastSvgXml = null
        lastSourceName = null
    }
}
