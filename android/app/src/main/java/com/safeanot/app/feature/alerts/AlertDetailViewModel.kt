/**
 * ViewModel for the alert detail screen.
 */
package com.safeanot.app.feature.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.AlertSeverity
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.domain.usecase.GetAlertByIdUseCase
import com.safeanot.app.domain.usecase.RefreshAlertsUseCase
import com.safeanot.app.domain.usecase.TrackShareEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertDetailUiState(
    val alert: ScamAlert? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val shareText: String? = null,
    val safetyTips: List<String> = emptyList(),
)

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlertByIdUseCase: GetAlertByIdUseCase,
    private val refreshAlertsUseCase: RefreshAlertsUseCase,
    private val trackShareEventUseCase: TrackShareEventUseCase,
) : ViewModel() {

    private val alertId: String = savedStateHandle.get<String>("alertId") ?: ""

    private val _uiState = MutableStateFlow(AlertDetailUiState())
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlert()
    }

    private fun loadAlert() {
        viewModelScope.launch {
            try {
                val alert = getAlertByIdUseCase(alertId)
                if (alert != null) {
                    _uiState.update {
                        it.copy(
                            alert = alert,
                            isLoading = false,
                            shareText = AlertShareFormatter.format(alert),
                            safetyTips = generateSafetyTips(alert),
                        )
                    }
                } else {
                    // Not in cache — try fetching from API first (cold start / deep link)
                    try {
                        refreshAlertsUseCase(AlertRegionFilter.ALL)
                        val retried = getAlertByIdUseCase(alertId)
                        if (retried != null) {
                            _uiState.update {
                                it.copy(
                                    alert = retried,
                                    isLoading = false,
                                    shareText = AlertShareFormatter.format(retried),
                                    safetyTips = generateSafetyTips(retried),
                                )
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        // API fetch failed — show network error, not "not found"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to load alert. Check your connection.",
                            )
                        }
                        return@launch
                    }
                    // API succeeded but alert still not found — genuinely missing
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Alert not found",
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load alert",
                    )
                }
            }
        }
    }

    private fun generateSafetyTips(alert: ScamAlert): List<String> {
        val tips = mutableListOf<String>()

        // Tips based on scam type
        when (alert.scamType.lowercase()) {
            "phishing" -> {
                tips.add("Never click on links from unknown senders.")
                tips.add("Verify the sender's email address or phone number carefully.")
                tips.add("Look for misspellings and grammatical errors in messages.")
            }
            "malware" -> {
                tips.add("Only install apps from the official Google Play Store.")
                tips.add("Keep Google Play Protect enabled on your device.")
                tips.add("Never download APK files from messaging apps or unknown sources.")
            }
            "investment" -> {
                tips.add("Be skeptical of guaranteed or unusually high returns.")
                tips.add("Verify that the investment platform is licensed by the relevant authority.")
                tips.add("Never send money to someone you have only met online.")
            }
            "impersonation" -> {
                tips.add("Government agencies will never ask for payment via phone or messaging apps.")
                tips.add("Hang up and call the official number to verify any claims.")
                tips.add("Do not share personal information with unsolicited callers.")
            }
            else -> {
                tips.add("Be cautious of unsolicited messages asking for personal information.")
                tips.add("Verify claims through official channels before taking action.")
            }
        }

        // Tips based on severity
        if (alert.severity == AlertSeverity.HIGH) {
            tips.add("This is a high-severity alert. Share this warning with friends and family.")
        }

        tips.add("Report suspicious activity to the relevant authorities in your country.")

        return tips
    }

    /**
     * Called by the UI after a share intent has been successfully launched.
     * Tracks the share event for analytics.
     */
    fun onShareCompleted(platform: SharePlatform) {
        viewModelScope.launch {
            trackShareEventUseCase(
                ShareEventModel(
                    shareType = ShareType.ALERT,
                    contentId = alertId,
                    platform = platform,
                )
            )
        }
    }
}
