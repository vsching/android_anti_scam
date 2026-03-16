/**
 * ViewModel for the Profile/Settings screen.
 * Manages notification preferences, reminder settings, and app info state.
 */
package com.safeanot.app.feature.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ProfileUiState(
    val remindersEnabled: Boolean = true,
    val reminderIntervalDays: Int = 7,
    val totalAudits: Int = 0,
    val appVersion: String = "1.0.0",
)

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun toggleReminders(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(remindersEnabled = enabled)
    }

    fun setReminderInterval(days: Int) {
        _uiState.value = _uiState.value.copy(reminderIntervalDays = days)
    }
}
