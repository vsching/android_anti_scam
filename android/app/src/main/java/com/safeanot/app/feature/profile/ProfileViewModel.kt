/**
 * ViewModel for the Profile/Settings screen.
 * Manages notification preferences, reminder settings, region preference, and app info state.
 * Reminder settings are persisted via Room and trigger scheduler updates.
 * Region and scam alert notification preferences are persisted via DataStore.
 * Audit stats are loaded via GetAuditStatsUseCase following clean architecture.
 */
package com.safeanot.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.BuildConfig
import com.safeanot.app.data.local.ReminderConfigDao
import com.safeanot.app.data.local.entity.ReminderConfigEntity
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.EmergencyContact
import com.safeanot.app.domain.model.EmergencyContacts
import com.safeanot.app.domain.repository.UserPreferencesRepository
import com.safeanot.app.util.Constants
import com.safeanot.app.domain.usecase.GetAuditStatsUseCase
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase
import com.safeanot.app.domain.usecase.SetPreferredRegionUseCase
import com.safeanot.app.worker.AuditReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val remindersEnabled: Boolean = true,
    val reminderIntervalDays: Int = 7,
    val totalAudits: Int = 0,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val lastAuditDate: String? = null,
    val securityScore: Int = 0,
    val selectedRegion: AlertRegionFilter = AlertRegionFilter.ALL,
    val scamAlertsEnabled: Boolean = true,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val legalLinks: Map<String, String> = mapOf(
        "Privacy Policy" to Constants.PRIVACY_POLICY_URL,
        "Terms of Service" to Constants.TERMS_OF_SERVICE_URL,
    ),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val reminderConfigDao: ReminderConfigDao,
    private val reminderScheduler: AuditReminderScheduler,
    private val getAuditStatsUseCase: GetAuditStatsUseCase,
    private val getPreferredRegionUseCase: GetPreferredRegionUseCase,
    private val setPreferredRegionUseCase: SetPreferredRegionUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _shareEvent = Channel<String>(Channel.BUFFERED)
    val shareEvent = _shareEvent.receiveAsFlow()

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

        viewModelScope.launch {
            getAuditStatsUseCase().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    totalAudits = stats.totalAudits,
                    securityScore = stats.securityScore,
                    lastAuditDate = formatLastAuditDate(stats.lastAuditTimestamp),
                )
            }
        }

        viewModelScope.launch {
            getPreferredRegionUseCase().collect { region ->
                _uiState.value = _uiState.value.copy(
                    selectedRegion = region,
                    emergencyContacts = EmergencyContacts.forRegion(region),
                )
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.getScamAlertsEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(scamAlertsEnabled = enabled)
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

    fun setRegion(region: AlertRegionFilter) {
        viewModelScope.launch {
            setPreferredRegionUseCase(region)
        }
    }

    fun toggleScamAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setScamAlertsEnabled(enabled)
        }
    }

    fun shareApp() {
        viewModelScope.launch {
            _shareEvent.send(ShareHelper.SHARE_TEXT)
        }
    }

    companion object {
        internal fun formatLastAuditDate(timestamp: Long?): String? {
            if (timestamp == null) return null

            val now = System.currentTimeMillis()
            val diffMs = now - timestamp
            val diffMinutes = diffMs / (1000 * 60)
            val diffHours = diffMs / (1000 * 60 * 60)
            val diffDays = diffMs / (1000 * 60 * 60 * 24)

            return when {
                diffMinutes < 1 -> "Last audit: just now"
                diffMinutes < 60 -> "Last audit: ${diffMinutes}m ago"
                diffHours < 24 -> "Last audit: ${diffHours}h ago"
                diffDays == 1L -> "Last audit: 1 day ago"
                else -> "Last audit: $diffDays days ago"
            }
        }
    }
}
