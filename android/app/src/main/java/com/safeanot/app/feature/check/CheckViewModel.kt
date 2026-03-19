/**
 * ViewModel for the Link Checker screen.
 * Delegates URL checking to CheckLinkUseCase and manages UI state.
 * Emits share events as domain data — NO Context dependency.
 */
package com.safeanot.app.feature.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.WarningTemplate
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.domain.usecase.CheckLinkUseCase
import com.safeanot.app.domain.usecase.GenerateRescueCardUseCase
import com.safeanot.app.domain.usecase.TrackShareEventUseCase
import com.safeanot.app.util.VerdictCardGenerator
import com.safeanot.app.util.WarningTemplateProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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
    private val trackShareEventUseCase: TrackShareEventUseCase,
    private val generateRescueCardUseCase: GenerateRescueCardUseCase,
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _checkState = MutableStateFlow<CheckUiState>(CheckUiState.Idle)
    val checkState: StateFlow<CheckUiState> = _checkState.asStateFlow()

    private val _shareEvents = Channel<ShareEvent>(Channel.BUFFERED)
    val shareEvents = _shareEvents.receiveAsFlow()

    /** Emits (formattedText, domain) pairs for warning shares. */
    private val _warningShareEvent = Channel<Pair<String, String>>(Channel.BUFFERED)
    val warningShareEvent = _warningShareEvent.receiveAsFlow()

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

    /**
     * Formats a warning template with verdict data and emits the text string.
     * The UI layer handles WhatsApp detection, intent creation, and startActivity.
     */
    fun shareWarning(template: WarningTemplate, localeTag: String) {
        val state = _checkState.value
        if (state !is CheckUiState.Result) return

        viewModelScope.launch {
            val verdict = state.verdict
            val formatted = WarningTemplateProvider.format(
                template = template,
                domain = verdict.domain,
                verdict = verdict.verdict.name,
                locale = localeTag,
            )
            _warningShareEvent.send(Pair(formatted, verdict.domain))
        }
    }

    /**
     * Generates a verdict card bitmap and emits a ShareEvent.
     * The UI layer handles saving the bitmap to cache and starting the share intent.
     */
    fun shareResult() {
        val state = _checkState.value
        if (state !is CheckUiState.Result) return

        viewModelScope.launch {
            try {
                val verdict = state.verdict
                val bitmap = VerdictCardGenerator.generate(verdict)

                val deepLink = "https://safeanot.com/result?domain=${verdict.domain}"
                val shareText = "I checked \"${verdict.domain}\" on Safe Anot? " +
                    "Verdict: ${verdict.verdict.name}. $deepLink"

                _shareEvents.send(ShareEvent.ImageWithText(
                    bitmap = bitmap,
                    text = shareText,
                    shareType = ShareType.VERDICT,
                    contentId = verdict.domain,
                ))
            } catch (_: Exception) {
                // Silently handle bitmap generation failures
            }
        }
    }

    /**
     * Generates a rescue card bitmap and emits an ImageWithText ShareEvent.
     * Includes domain info and download link so recipients get context.
     * The UI layer handles saving the bitmap to cache and starting the share intent.
     */
    fun shareRescueCard() {
        val state = _checkState.value
        if (state !is CheckUiState.Result) return

        viewModelScope.launch {
            try {
                val verdict = state.verdict
                val bitmap = generateRescueCardUseCase(verdict)
                val rescueShareText = "I checked \"${verdict.domain}\" on Safe Anot? " +
                    "It looks DANGEROUS. Protect yourself — download the app: " +
                    "https://play.google.com/store/apps/details?id=com.safeanot.app"
                _shareEvents.send(ShareEvent.ImageWithText(
                    bitmap = bitmap,
                    text = rescueShareText,
                    shareType = ShareType.RESCUE_CARD,
                    contentId = verdict.domain,
                ))
            } catch (_: Exception) {
                // Silently handle bitmap generation failures
            }
        }
    }

    /**
     * Called by the UI after a share intent has been successfully launched.
     * Tracks the share event for analytics.
     */
    fun onShareCompleted(shareType: ShareType, contentId: String, platform: SharePlatform) {
        viewModelScope.launch {
            trackShareEventUseCase(
                ShareEventModel(
                    shareType = shareType,
                    contentId = contentId,
                    platform = platform,
                )
            )
        }
    }
}
