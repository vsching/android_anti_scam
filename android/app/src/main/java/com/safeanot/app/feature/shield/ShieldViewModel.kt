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
import com.safeanot.app.domain.model.CardFormat
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.usecase.GenerateScoreCardUseCase
import com.safeanot.app.domain.usecase.GetSecurityScoreUseCase
import com.safeanot.app.domain.usecase.RunAuditUseCase
import com.safeanot.app.feature.check.ShareEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
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
)

@HiltViewModel
class ShieldViewModel @Inject constructor(
    private val runAuditUseCase: RunAuditUseCase,
    private val getSecurityScoreUseCase: GetSecurityScoreUseCase,
    private val generateScoreCardUseCase: GenerateScoreCardUseCase,
    private val repository: AuditRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShieldUiState())
    val uiState: StateFlow<ShieldUiState> = _uiState.asStateFlow()

    val auditItems: StateFlow<List<AuditItem>> = repository.getAllAuditItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val securityScore: StateFlow<SecurityScore> = getSecurityScoreUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityScore())

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
                _shareEvents.send(ShareEvent.BitmapOnly(bitmap))
            } catch (_: Exception) {
                // Silently handle bitmap generation failures
            }
        }
    }
}
