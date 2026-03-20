/**
 * ViewModel for the Guardian Dashboard screen.
 * Displays a list of wards (devices being monitored) with their security status.
 * No Context dependency — uses only UseCases.
 */
package com.safeanot.app.feature.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.domain.usecase.GetWardsUseCase
import com.safeanot.app.domain.usecase.RefreshWardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val wards: List<WardUiModel> = emptyList(),
    val error: String? = null,
)

data class WardUiModel(
    val pairingId: String,
    val deviceId: String,
    val displayName: String,
    val securityScore: Int? = null,
    val scoreBand: ScoreBand? = null,
    val lastHeartbeat: Long? = null,
    val playProtectEnabled: Boolean? = null,
    val isStale: Boolean = false,
)

@HiltViewModel
class GuardianDashboardViewModel @Inject constructor(
    private val getWardsUseCase: GetWardsUseCase,
    private val refreshWardsUseCase: RefreshWardsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getWardsUseCase()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load wards",
                        )
                    }
                }
                .collect { pairings ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            wards = pairings.map { pairing -> pairing.toWardUiModel() },
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                refreshWardsUseCase()
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh",
                    )
                }
            }
        }
    }

    companion object {
        private const val STALE_THRESHOLD_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    private fun GuardianPairing.toWardUiModel(): WardUiModel {
        // Note: GuardianPairing doesn't carry heartbeat data directly.
        // The heartbeat data would need to be fetched separately or enriched.
        // For now, we create a basic WardUiModel from the pairing data.
        return WardUiModel(
            pairingId = id,
            deviceId = pairedDeviceId,
            displayName = label,
        )
    }
}
