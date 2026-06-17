// Watermelon Vector Converter
// Copyright (c) 2026 Suhail Muzaffari. All rights reserved.
// Proprietary and source-available. Reuse prohibited without written permission.
// See LICENSE for terms.

package com.watermelon.converter.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.watermelon.converter.data.repository.FileRepository
import com.watermelon.converter.jni.ConversionException
import com.watermelon.converter.jni.ProgressCallback
import com.watermelon.converter.jni.RealSvgConverter
import com.watermelon.converter.jni.SvgConverter
import com.watermelon.converter.jni.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BatchProgress(val done: Int, val total: Int, val currentName: String)

sealed interface BatchUiState {
    data object Idle : BatchUiState
    data class Working(val progress: BatchProgress?) : BatchUiState
    @Suppress("ArrayInDataClass")
    data class Done(val outputZip: ByteArray) : BatchUiState
    data class Error(val message: String) : BatchUiState
}

class BatchViewModel(
    app: Application,
    private val native: SvgConverter = RealSvgConverter,
) : AndroidViewModel(app) {

    private val repo = FileRepository(app.applicationContext)
    private val _state = MutableStateFlow<BatchUiState>(BatchUiState.Idle)
    val state: StateFlow<BatchUiState> = _state.asStateFlow()

    private var lastZip: ByteArray? = null

    fun convertZip(zipUri: Uri) {
        _state.value = BatchUiState.Working(null)
        viewModelScope.launch {
            try {
                val out = withContext(Dispatchers.IO) {
                    val bytes = repo.readBytes(zipUri)
                    val cb = object : ProgressCallback {
                        override fun onProgress(done: Int, total: Int, currentName: String) {
                            _state.value = BatchUiState.Working(BatchProgress(done, total, currentName))
                        }
                    }
                    native.convertZip(bytes, cb)
                }
                lastZip = out
                _state.value = BatchUiState.Done(out)
            } catch (e: ConversionException) {
                _state.value = BatchUiState.Error(e.userMessage(getApplication()))
            } catch (e: Exception) {
                _state.value = BatchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun cancel() { native.cancel() }

    fun export(treeUri: Uri, fileName: String = "watermelon_vectors.zip") {
        val zip = lastZip ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.writeToTree(treeUri, fileName, "application/zip", zip) }
        }
    }

    fun reset() { _state.value = BatchUiState.Idle }
}
