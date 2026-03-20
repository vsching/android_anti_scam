/**
 * ViewModel for the Scam Alerts feed screen.
 * Driven by AlertsRepository with region filtering, pull-to-refresh, and navigation events.
 * Reactively observes the persisted region preference so the feed updates live
 * when the user changes region in Profile settings.
 */
package com.safeanot.app.feature.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.domain.model.ScamAlert
import com.safeanot.app.domain.usecase.GetFeaturedAlertUseCase
import com.safeanot.app.domain.usecase.GetPreferredRegionUseCase
import com.safeanot.app.domain.usecase.ObserveAlertsUseCase
import com.safeanot.app.domain.usecase.RefreshAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
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
    val featuredAlert: ScamAlert? = null,
    val isFeaturedDismissed: Boolean = false,
)

sealed class AlertsNavigationEvent {
    data class NavigateToDetail(val alertId: String) : AlertsNavigationEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val observeAlertsUseCase: ObserveAlertsUseCase,
    private val refreshAlertsUseCase: RefreshAlertsUseCase,
    private val getPreferredRegionUseCase: GetPreferredRegionUseCase,
    private val getFeaturedAlertUseCase: GetFeaturedAlertUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<AlertsNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    /**
     * Holds the current active filter, driven by either user preference or manual filter change.
     * Initialized to null so the first refresh waits until the persisted region arrives.
     */
    private val activeFilter = MutableStateFlow<AlertRegionFilter?>(null)

    init {
        // Seed the active filter from the persisted region, then keep observing preference changes.
        // Manual filter changes via onFilterChanged() update activeFilter directly;
        // preference changes also update activeFilter so Profile region changes always propagate.
        viewModelScope.launch {
            getPreferredRegionUseCase().collect { region ->
                activeFilter.value = region
                _uiState.update { it.copy(selectedFilter = region) }
            }
        }

        // Observe the featured "Scam of the Week" alert
        viewModelScope.launch {
            getFeaturedAlertUseCase().collect { featured ->
                _uiState.update { it.copy(featuredAlert = featured) }
            }
        }

        // Reactively observe alerts once the first region value arrives
        viewModelScope.launch {
            // Wait for the initial region preference before starting observation
            activeFilter.first { it != null }

            activeFilter.flatMapLatest { filter ->
                // filter is guaranteed non-null after the first { it != null } gate
                val resolvedFilter = filter ?: AlertRegionFilter.ALL
                _uiState.update { it.copy(isInitialLoading = true, errorMessage = null) }
                refresh(resolvedFilter, isInitial = true)
                observeAlertsUseCase(resolvedFilter)
            }.collectLatest { alerts ->
                _uiState.update {
                    it.copy(
                        alerts = alerts,
                        isEmpty = alerts.isEmpty() && !it.isInitialLoading,
                    )
                }
            }
        }
    }

    fun onFilterChanged(filter: AlertRegionFilter) {
        if (filter == _uiState.value.selectedFilter) return
        activeFilter.value = filter
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onRefresh() {
        val filter = _uiState.value.selectedFilter
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        refresh(filter, isInitial = false)
    }

    fun onDismissFeatured() {
        _uiState.update { it.copy(isFeaturedDismissed = true) }
    }

    fun onAlertClick(alert: ScamAlert) {
        viewModelScope.launch {
            _navigationEvents.send(AlertsNavigationEvent.NavigateToDetail(alert.id))
        }
    }

    /** Active refresh job -- cancelled on new refresh to prevent stale overwrites. */
    private var refreshJob: Job? = null

    private fun refresh(filter: AlertRegionFilter, isInitial: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
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
