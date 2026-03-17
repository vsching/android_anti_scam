/**
 * ViewModel for the guided Fix flow screen.
 * Handles the remediation process for a specific app: risk explanation, settings deep-link,
 * and user confirmation of securing the install permission.
 */
package com.safeanot.app.feature.fix

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FixUiState(
    val packageName: String = "",
    val appName: String = "",
    val riskDescription: String = "",
    val itemId: Int = 0,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val hasOpenedSettings: Boolean = false,
    val showConfirmation: Boolean = false,
)

@HiltViewModel
class FixViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AuditRepository,
) : ViewModel() {

    private val packageName: String = savedStateHandle["packageName"] ?: ""

    private val _uiState = MutableStateFlow(
        FixUiState(packageName = packageName)
    )
    val uiState: StateFlow<FixUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = repository.getAllAuditItems().first()
            val item = items.find { it.packageName == packageName }
            if (item != null) {
                _uiState.value = _uiState.value.copy(
                    itemId = item.id,
                    appName = item.appName,
                    riskDescription = item.riskDescription,
                )
            }
        }
    }

    fun onSettingsOpened() {
        _uiState.value = _uiState.value.copy(hasOpenedSettings = true)
    }

    /** Called when user returns from settings to show confirmation prompt. */
    fun onReturnedFromSettings() {
        if (_uiState.value.hasOpenedSettings) {
            _uiState.value = _uiState.value.copy(showConfirmation = true)
        }
    }

    fun dismissConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmation = false)
    }

    fun markAsSecured() {
        val pkg = _uiState.value.packageName
        if (pkg.isNotEmpty()) {
            viewModelScope.launch {
                repository.updateItemStatusByPackage(pkg, AuditStatus.SECURED)
                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }

    fun markAsSkipped() {
        val pkg = _uiState.value.packageName
        if (pkg.isNotEmpty()) {
            viewModelScope.launch {
                repository.updateItemStatusByPackage(pkg, AuditStatus.SKIPPED)
                _uiState.value = _uiState.value.copy(isSkipped = true)
            }
        }
    }
}
