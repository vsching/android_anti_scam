/**
 * ViewModel for the Ward Detail screen.
 * Loads ward details and heartbeat history for a specific ward device.
 */
package com.safeanot.app.feature.guardian

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.HeartbeatEntry
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.domain.model.WardHeartbeatHistory
import com.safeanot.app.domain.repository.GuardianRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WardDetailUiState(
    val isLoading: Boolean = true,
    val deviceId: String = "",
    val displayName: String = "",
    val securityScore: Int? = null,
    val scoreBand: ScoreBand? = null,
    val lastHeartbeat: Long? = null,
    val playProtectEnabled: Boolean? = null,
    val heartbeatHistory: List<HeartbeatEntry> = emptyList(),
    val error: String? = null,
    val isUnlinking: Boolean = false,
    val unlinkSuccess: Boolean = false,
)

@HiltViewModel
class WardDetailViewModel @Inject constructor(
    private val guardianRepository: GuardianRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val wardDeviceId: String = checkNotNull(savedStateHandle["deviceId"]) {
        "deviceId is required for WardDetailScreen"
    }

    private val _uiState = MutableStateFlow(WardDetailUiState(deviceId = wardDeviceId))
    val uiState: StateFlow<WardDetailUiState> = _uiState.asStateFlow()

    init {
        loadWardDetails()
    }

    private fun loadWardDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val history = guardianRepository.getWardHeartbeatHistory(wardDeviceId)
                val latestHeartbeat = history.heartbeats.maxByOrNull { it.timestamp }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayName = history.displayName,
                        securityScore = latestHeartbeat?.securityScore,
                        scoreBand = latestHeartbeat?.securityScore?.let { score ->
                            ScoreBand.fromPercent(score)
                        },
                        lastHeartbeat = latestHeartbeat?.timestamp,
                        playProtectEnabled = latestHeartbeat?.playProtectEnabled,
                        heartbeatHistory = history.heartbeats.sortedBy { entry -> entry.timestamp },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load ward details",
                    )
                }
            }
        }
    }

    fun unlink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnlinking = true, error = null) }
            try {
                // Find the pairing ID for this ward and delete it
                guardianRepository.deletePairing(wardDeviceId)
                _uiState.update { it.copy(isUnlinking = false, unlinkSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUnlinking = false,
                        error = e.message ?: "Failed to unlink ward",
                    )
                }
            }
        }
    }

    fun refresh() {
        loadWardDetails()
    }
}
