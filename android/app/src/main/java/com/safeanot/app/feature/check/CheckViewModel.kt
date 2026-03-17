/**
 * ViewModel for the Link Checker screen.
 * Delegates URL checking to CheckLinkUseCase and manages UI state.
 */
package com.safeanot.app.feature.check

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.usecase.CheckLinkUseCase
import com.safeanot.app.util.VerdictCardGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed class CheckUiState {
    object Idle : CheckUiState()
    object Loading : CheckUiState()
    data class Result(val verdict: LinkVerdict) : CheckUiState()
    data class Error(val message: String) : CheckUiState()
}

@HiltViewModel
class CheckViewModel @Inject constructor(
    private val checkLinkUseCase: CheckLinkUseCase,
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _checkState = MutableStateFlow<CheckUiState>(CheckUiState.Idle)
    val checkState: StateFlow<CheckUiState> = _checkState.asStateFlow()

    fun onUrlChanged(url: String) {
        _urlInput.value = url
    }

    fun checkLink(url: String = _urlInput.value) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        _checkState.value = CheckUiState.Loading

        viewModelScope.launch {
            try {
                val verdict = checkLinkUseCase(trimmed)
                _checkState.value = CheckUiState.Result(verdict)
            } catch (e: Exception) {
                _checkState.value = CheckUiState.Error(
                    e.message ?: "An unexpected error occurred."
                )
            }
        }
    }

    fun clearResults() {
        _checkState.value = CheckUiState.Idle
        _urlInput.value = ""
    }

    fun prefillUrl(url: String) {
        _urlInput.value = url
    }

    fun shareResult(context: Context) {
        val state = _checkState.value
        if (state !is CheckUiState.Result) return

        viewModelScope.launch {
            try {
                val verdict = state.verdict
                val bitmap = VerdictCardGenerator.generate(verdict)

                val sharedDir = File(context.cacheDir, "shared_verdicts")
                sharedDir.mkdirs()
                val file = File(sharedDir, "verdict_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

                val deepLink = "https://safeanot.com/result?domain=${verdict.domain}"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "I checked \"${verdict.domain}\" on Safe Anot? " +
                            "Verdict: ${verdict.verdict.name}. $deepLink"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Verdict")
                )
            } catch (_: Exception) {
                // Silently handle share failures
            }
        }
    }
}
