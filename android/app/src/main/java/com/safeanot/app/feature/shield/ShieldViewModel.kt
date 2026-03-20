/**
 * ViewModel for the Shield (audit dashboard) screen.
 * Manages audit state, triggers scans, and exposes UI state via StateFlow.
 * Emits share events as domain data — NO Context dependency.
 */
package com.safeanot.app.feature.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.CardFormat
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.model.ShareEventModel
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.GuardianRepository
import com.safeanot.app.domain.usecase.GenerateScoreCardUseCase
import com.safeanot.app.domain.model.Streak
import com.safeanot.app.domain.usecase.GetCurrentStreakUseCase
import com.safeanot.app.domain.usecase.GetSecurityScoreUseCase
import com.safeanot.app.domain.usecase.RunAuditUseCase
import com.safeanot.app.domain.usecase.SendHelpRequestUseCase
import com.safeanot.app.domain.usecase.TrackShareEventUseCase
import com.safeanot.app.domain.usecase.UnlockBadgeUseCase
import com.safeanot.app.domain.usecase.UpdateStreakUseCase
import com.safeanot.app.feature.check.ShareEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShieldUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasScanned: Boolean = false,
    val error: String? = null,
    val helpRequestSent: Boolean = false,
    val helpRequestError: String? = null,
    val isSendingHelpRequest: Boolean = false,
)

@HiltViewModel
class ShieldViewModel @Inject constructor(
    private val runAuditUseCase: RunAuditUseCase,
    private val getSecurityScoreUseCase: GetSecurityScoreUseCase,
    private val generateScoreCardUseCase: GenerateScoreCardUseCase,
    private val repository: AuditRepository,
    private val trackShareEventUseCase: TrackShareEventUseCase,
    private val sendHelpRequestUseCase: SendHelpRequestUseCase,
    private val guardianRepository: GuardianRepository,
    private val updateStreakUseCase: UpdateStreakUseCase,
    private val getCurrentStreakUseCase: GetCurrentStreakUseCase,
    private val unlockBadgeUseCase: UnlockBadgeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShieldUiState())
    val uiState: StateFlow<ShieldUiState> = _uiState.asStateFlow()

    val auditItems: StateFlow<List<AuditItem>> = repository.getAllAuditItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityScore: StateFlow<SecurityScore> = getSecurityScoreUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityScore())

    val hasGuardians: StateFlow<Boolean> = guardianRepository.getGuardianCount()
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val streak: StateFlow<Streak> = getCurrentStreakUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Streak())

    private val _shareEvents = Channel<ShareEvent>(Channel.BUFFERED)
    val shareEvents = _shareEvents.receiveAsFlow()

    init {
        runScan()
    }

    fun runScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                runAuditUseCase()
                _uiState.value = _uiState.value.copy(isLoading = false, hasScanned = true)
                updateStreakUseCase(securityScore.value.scorePercent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Audit failed",
                )
            }
        }
    }

    /** Pull-to-refresh handler. */
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                runAuditUseCase()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    hasScanned = true,
                )
                updateStreakUseCase(securityScore.value.scorePercent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Audit failed",
                )
            }
        }
    }

    fun markAsSecured(itemId: Int) {
        viewModelScope.launch {
            repository.updateItemStatus(itemId, AuditStatus.SECURED)
        }
    }

    fun markAsSkipped(itemId: Int) {
        viewModelScope.launch {
            repository.updateItemStatus(itemId, AuditStatus.SKIPPED)
        }
    }

    /**
     * Generates a security score card bitmap and emits a ShareEvent.BitmapOnly.
     * The UI layer handles saving the bitmap to cache and starting the share intent.
     */
    fun shareScore(format: CardFormat) {
        viewModelScope.launch {
            try {
                val score = securityScore.value
                val bitmap = generateScoreCardUseCase(score, format)
                _shareEvents.send(ShareEvent.BitmapOnly(
                    bitmap = bitmap,
                    shareType = ShareType.SCORE,
                    contentId = "${score.securedItems}/${score.totalItems}",
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
            unlockBadgeUseCase(BadgeType.SHARE_GUARDIAN)
        }
    }

    /** Send a "Help Me Fix This" request to all guardians. */
    fun sendHelpRequest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSendingHelpRequest = true,
                helpRequestSent = false,
                helpRequestError = null,
            )
            try {
                sendHelpRequestUseCase()
                _uiState.value = _uiState.value.copy(
                    isSendingHelpRequest = false,
                    helpRequestSent = true,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingHelpRequest = false,
                    helpRequestError = e.message ?: "Failed to send help request",
                )
            }
        }
    }

    /** Clear the help request sent/error status. */
    fun clearHelpRequestStatus() {
        _uiState.value = _uiState.value.copy(
            helpRequestSent = false,
            helpRequestError = null,
        )
    }
}
