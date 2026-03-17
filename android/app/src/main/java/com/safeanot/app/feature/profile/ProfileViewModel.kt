/**
 * ViewModel for the Profile/Settings screen.
 * Manages notification preferences, reminder settings, and app info state.
 * Reminder settings are persisted via Room and trigger scheduler updates.
 */
package com.safeanot.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.data.local.ReminderConfigDao
import com.safeanot.app.data.local.entity.ReminderConfigEntity
import com.safeanot.app.worker.AuditReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val remindersEnabled: Boolean = true,
    val reminderIntervalDays: Int = 7,
    val totalAudits: Int = 0,
    val appVersion: String = "1.0.0",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val reminderConfigDao: ReminderConfigDao,
    private val reminderScheduler: AuditReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderConfigDao.getConfig().collect { config ->
                val enabled = config?.enabled ?: true
                val interval = config?.intervalDays ?: 7
                _uiState.value = _uiState.value.copy(
                    remindersEnabled = enabled,
                    reminderIntervalDays = interval,
                )
            }
        }
    }

    fun toggleReminders(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(remindersEnabled = enabled)
        viewModelScope.launch {
            val interval = _uiState.value.reminderIntervalDays
            reminderConfigDao.upsert(
                ReminderConfigEntity(
                    enabled = enabled,
                    intervalDays = interval,
                )
            )
            reminderScheduler.updateSchedule(enabled, interval)
        }
    }

    fun setReminderInterval(days: Int) {
        _uiState.value = _uiState.value.copy(reminderIntervalDays = days)
        viewModelScope.launch {
            val enabled = _uiState.value.remindersEnabled
            reminderConfigDao.upsert(
                ReminderConfigEntity(
                    enabled = enabled,
                    intervalDays = days,
                )
            )
            if (enabled) {
                reminderScheduler.schedule(days)
            }
        }
    }
}
