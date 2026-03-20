/**
 * Scam Alerts feed screen with region filtering, pull-to-refresh,
 * loading skeletons, and empty state.
 */
package com.safeanot.app.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.feature.alerts.components.AlertFeedCard
import com.safeanot.app.feature.alerts.components.AlertRegionChips
import com.safeanot.app.feature.alerts.components.AlertsEmptyState
import com.safeanot.app.feature.alerts.components.AlertsLoadingSkeleton
import com.safeanot.app.feature.alerts.components.FeaturedAlertCard
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Collect navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is AlertsNavigationEvent.NavigateToDetail -> {
                    onNavigateToDetail(event.alertId)
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scam Alerts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stay informed about the latest scam tactics in MY/SG.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Featured "Scam of the Week" card
            if (uiState.featuredAlert != null && !uiState.isFeaturedDismissed) {
                item {
                    FeaturedAlertCard(
                        alert = uiState.featuredAlert!!,
                        onDismiss = viewModel::onDismissFeatured,
                        onClick = { viewModel.onAlertClick(uiState.featuredAlert!!) },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Filter chips
            item {
                AlertRegionChips(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::onFilterChanged,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Loading skeleton
            if (uiState.isInitialLoading) {
                item {
                    AlertsLoadingSkeleton()
                }
            }

            // Error state
            if (uiState.errorMessage != null && !uiState.isInitialLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Something went wrong",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RedAccent,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = viewModel::onRefresh) {
                            Text("Retry")
                        }
                    }
                }
            }

            // Empty state
            if (uiState.isEmpty && !uiState.isInitialLoading) {
                item {
                    AlertsEmptyState(onRefresh = viewModel::onRefresh)
                }
            }

            // Alert cards
            if (!uiState.isInitialLoading && !uiState.isEmpty) {
                items(uiState.alerts, key = { it.id }) { alert ->
                    AlertFeedCard(
                        alert = alert,
                        onClick = { viewModel.onAlertClick(alert) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
