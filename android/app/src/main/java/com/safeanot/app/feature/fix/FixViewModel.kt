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
    val itemId: Int = 0,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val hasOpenedSettings: Boolean = false,
)

@HiltViewModel
class FixViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AuditRepository,
) : ViewModel() {

    private val packageName: String = savedStateHandle["packageName"] ?: ""
    private val appName: String = savedStateHandle["appName"] ?: ""

    private val _uiState = MutableStateFlow(
        FixUiState(packageName = packageName, appName = appName)
    )
    val uiState: StateFlow<FixUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = repository.getAllAuditItems().first()
            val item = items.find { it.packageName == packageName }
            if (item != null) {
                _uiState.value = _uiState.value.copy(itemId = item.id)
            }
        }
    }

    fun onSettingsOpened() {
        _uiState.value = _uiState.value.copy(hasOpenedSettings = true)
    }

    fun markAsSecured() {
        val id = _uiState.value.itemId
        if (id > 0) {
            viewModelScope.launch {
                repository.updateItemStatus(id, AuditStatus.SECURED)
                _uiState.value = _uiState.value.copy(isCompleted = true)
            }
        }
    }

    fun markAsSkipped() {
        val id = _uiState.value.itemId
        if (id > 0) {
            viewModelScope.launch {
                repository.updateItemStatus(id, AuditStatus.SKIPPED)
                _uiState.value = _uiState.value.copy(isSkipped = true)
            }
        }
    }
}
