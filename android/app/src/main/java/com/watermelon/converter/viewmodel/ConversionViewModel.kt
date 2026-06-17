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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ConvertUiState {
    data object Idle : ConvertUiState
    data object Working : ConvertUiState
    @Suppress("ArrayInDataClass")
    data class Done(
        val sourceName: String,
        val vdXml: String,
        val svgPreviewPng: ByteArray?,
        val vdPreviewPng: ByteArray?,
    ) : ConvertUiState
    data class Error(val message: String) : ConvertUiState
}

class ConversionViewModel(
    app: Application,
    private val native: SvgConverter = RealSvgConverter,
) : AndroidViewModel(app) {

    private val repo = FileRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)

    private val _state = MutableStateFlow<ConvertUiState>(ConvertUiState.Idle)
    val state: StateFlow<ConvertUiState> = _state.asStateFlow()

    private var lastVdXml: String? = null
    private var lastSourceName: String? = null

    fun convert(uri: Uri) {
        _state.value = ConvertUiState.Working
        viewModelScope.launch {
            val px = runCatching { settingsRepo.settings.first().previewPx }.getOrDefault(256)
            var name = "image.svg"
            try {
                val result = withContext(Dispatchers.IO) {
                    name = repo.displayName(uri)
                    val svgBytes = repo.readBytes(uri)
                    require(svgBytes.isNotEmpty()) { "empty file" }
                    val xml = native.convertSvg(svgBytes)
                    val svgPng = runCatching { native.renderSvgPreview(svgBytes, px) }.getOrNull()
                    val vdPng = runCatching { native.renderVdPreview(xml, px) }.getOrNull()
                    lastVdXml = xml
                    lastSourceName = name
                    ConvertUiState.Done(name, xml, svgPng, vdPng)
                }
                HistoryStore.add(result.sourceName, result.vdXml, ok = true)
                _state.value = result
            } catch (e: ConversionException) {
                val msg = e.userMessage(getApplication())
                HistoryStore.add(name, "", ok = false, error = msg)
                _state.value = ConvertUiState.Error(msg)
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                HistoryStore.add(name, "", ok = false, error = msg)
                _state.value = ConvertUiState.Error(msg)
            }
        }
    }

    fun export(treeUri: Uri) {
        val xml = lastVdXml ?: return
        val name = lastSourceName ?: "image.svg"
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repo.writeToTree(treeUri, repo.xmlNameFor(name), "application/xml", xml.toByteArray())
            }
        }
    }

    fun currentXml(): String? = lastVdXml

    fun reset() {
        _state.value = ConvertUiState.Idle
        lastVdXml = null
        lastSourceName = null
    }
}
