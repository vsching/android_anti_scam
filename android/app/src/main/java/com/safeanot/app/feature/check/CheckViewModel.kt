/**
 * ViewModel for the Link Checker screen.
 * Handles URL checking against local scam pattern database (v1: local regex matching).
 */
package com.safeanot.app.feature.check

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class Verdict {
    SAFE, SUSPICIOUS, DANGEROUS
}

data class VerdictResult(
    val url: String,
    val verdict: Verdict,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class CheckUiState(
    val urlInput: String = "",
    val isChecking: Boolean = false,
    val currentVerdict: VerdictResult? = null,
    val recentResults: List<VerdictResult> = emptyList(),
)

@HiltViewModel
class CheckViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CheckUiState())
    val uiState: StateFlow<CheckUiState> = _uiState.asStateFlow()

    /** Suspicious URL patterns — local regex matching for v1. */
    private val suspiciousPatterns = listOf(
        Regex("""\.apk$""", RegexOption.IGNORE_CASE),
        Regex("""bit\.ly/""", RegexOption.IGNORE_CASE),
        Regex("""tinyurl\.com/""", RegexOption.IGNORE_CASE),
        Regex("""t\.me/""", RegexOption.IGNORE_CASE),
        Regex("""goo\.gl/""", RegexOption.IGNORE_CASE),
        Regex("""shorturl\.at/""", RegexOption.IGNORE_CASE),
    )

    private val dangerousPatterns = listOf(
        Regex("""\.apk\?""", RegexOption.IGNORE_CASE),
        Regex("""download.*\.apk""", RegexOption.IGNORE_CASE),
        Regex("""install.*update""", RegexOption.IGNORE_CASE),
        Regex("""bank.*login""", RegexOption.IGNORE_CASE),
        Regex("""free.*reward""", RegexOption.IGNORE_CASE),
    )

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url, currentVerdict = null)
    }

    fun checkUrl() {
        val url = _uiState.value.urlInput.trim()
        if (url.isBlank()) return

        _uiState.value = _uiState.value.copy(isChecking = true)

        val verdict = analyzeUrl(url)

        _uiState.value = _uiState.value.copy(
            isChecking = false,
            currentVerdict = verdict,
            recentResults = listOf(verdict) + _uiState.value.recentResults.take(9),
        )
    }

    private fun analyzeUrl(url: String): VerdictResult {
        // Check dangerous patterns first
        for (pattern in dangerousPatterns) {
            if (pattern.containsMatchIn(url)) {
                return VerdictResult(
                    url = url,
                    verdict = Verdict.DANGEROUS,
                    reason = "URL matches known scam/malware patterns.",
                )
            }
        }

        // Check suspicious patterns
        for (pattern in suspiciousPatterns) {
            if (pattern.containsMatchIn(url)) {
                return VerdictResult(
                    url = url,
                    verdict = Verdict.SUSPICIOUS,
                    reason = "URL uses a shortened link or APK download. Proceed with caution.",
                )
            }
        }

        return VerdictResult(
            url = url,
            verdict = Verdict.SAFE,
            reason = "No known scam patterns detected. Stay vigilant.",
        )
    }

    fun clearResults() {
        _uiState.value = CheckUiState()
    }
}
