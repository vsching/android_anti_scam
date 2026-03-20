/**
 * ViewModel for the Guardian Pairing screen.
 * Manages pairing code generation, claiming, deletion, and listing.
 * No Context dependency — uses only UseCases.
 */
package com.safeanot.app.feature.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.GuardianPairing
import com.safeanot.app.domain.model.PairingCode
import com.safeanot.app.domain.usecase.ClaimPairingCodeUseCase
import com.safeanot.app.domain.usecase.DeletePairingUseCase
import com.safeanot.app.domain.usecase.GeneratePairingCodeUseCase
import com.safeanot.app.domain.usecase.GetPairingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuardianUiState(
    val pairings: List<GuardianPairing> = emptyList(),
    val pairingCode: PairingCode? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val claimSuccess: Boolean = false,
    val selectedTab: Int = 0,
)

@HiltViewModel
class GuardianPairingViewModel @Inject constructor(
    private val generatePairingCodeUseCase: GeneratePairingCodeUseCase,
    private val claimPairingCodeUseCase: ClaimPairingCodeUseCase,
    private val deletePairingUseCase: DeletePairingUseCase,
    private val getPairingsUseCase: GetPairingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuardianUiState())
    val uiState: StateFlow<GuardianUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getPairingsUseCase().collect { pairings ->
                _uiState.update { it.copy(pairings = pairings, isLoading = false) }
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun generateCode(role: String, label: String) {
        _uiState.update { it.copy(isLoading = true, error = null, pairingCode = null) }
        viewModelScope.launch {
            try {
                val code = generatePairingCodeUseCase(role, label)
                _uiState.update { it.copy(pairingCode = code, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to generate code", isLoading = false) }
            }
        }
    }

    fun claimCode(code: String, label: String) {
        _uiState.update { it.copy(isLoading = true, error = null, claimSuccess = false) }
        viewModelScope.launch {
            try {
                claimPairingCodeUseCase(code, label)
                _uiState.update { it.copy(claimSuccess = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to claim code", isLoading = false) }
            }
        }
    }

    fun deletePairing(pairingId: String) {
        viewModelScope.launch {
            try {
                deletePairingUseCase(pairingId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete pairing") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearClaimSuccess() {
        _uiState.update { it.copy(claimSuccess = false) }
    }
}
