/**
 * ViewModel for the Scam Alerts feed screen.
 * Driven by AlertsRepository with region filtering, pull-to-refresh, and navigation events.
 */
package com.safeanot.app.feature.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.usecase.GetDefaultAlertRegionUseCase
import com.safeanot.app.domain.usecase.ObserveAlertsUseCase
import com.safeanot.app.domain.usecase.RefreshAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<ScamAlert> = emptyList(),
    val selectedFilter: AlertRegionFilter = AlertRegionFilter.ALL,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
)

sealed class AlertsNavigationEvent {
    data class NavigateToDetail(val alertId: String) : AlertsNavigationEvent()
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val observeAlertsUseCase: ObserveAlertsUseCase,
    private val refreshAlertsUseCase: RefreshAlertsUseCase,
    private val getDefaultAlertRegionUseCase: GetDefaultAlertRegionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<AlertsNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    init {
        val defaultFilter = getDefaultAlertRegionUseCase()
        _uiState.update { it.copy(selectedFilter = defaultFilter) }
        observeAlerts(defaultFilter)
        refresh(defaultFilter, isInitial = true)
    }

    fun onFilterChanged(filter: AlertRegionFilter) {
        if (filter == _uiState.value.selectedFilter) return
        _uiState.update { it.copy(selectedFilter = filter, isInitialLoading = true, errorMessage = null) }
        observeAlerts(filter)
        refresh(filter, isInitial = true)
    }

    fun onRefresh() {
        val filter = _uiState.value.selectedFilter
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        refresh(filter, isInitial = false)
    }

    fun onAlertClick(alert: ScamAlert) {
        viewModelScope.launch {
            _navigationEvents.send(AlertsNavigationEvent.NavigateToDetail(alert.id))
        }
    }

    /** Active observer job — cancelled when filter changes to avoid stale collectors. */
    private var observeJob: Job? = null

    private fun observeAlerts(filter: AlertRegionFilter) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeAlertsUseCase(filter).collect { alerts ->
                _uiState.update {
                    it.copy(
                        alerts = alerts,
                        isEmpty = alerts.isEmpty() && !it.isInitialLoading,
                    )
                }
            }
        }
    }

    private fun refresh(filter: AlertRegionFilter, isInitial: Boolean) {
        viewModelScope.launch {
            try {
                refreshAlertsUseCase(filter)
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        isEmpty = it.alerts.isEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        errorMessage = if (it.alerts.isEmpty()) {
                            e.message ?: "Failed to load alerts"
                        } else {
                            null // Silently fail if we have cached data
                        },
                        isEmpty = it.alerts.isEmpty(),
                    )
                }
            }
        }
    }
}
