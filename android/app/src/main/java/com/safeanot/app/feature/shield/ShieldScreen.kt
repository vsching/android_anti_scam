/**
 * Main Shield screen showing the security score ring and audit item cards grouped by category.
 * This is the primary/first screen of the app — the phone audit dashboard.
 */
package com.safeanot.app.feature.shield

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.AppCategory
import com.safeanot.app.feature.shield.components.AppCard
import com.safeanot.app.feature.shield.components.SecurityScoreRing
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun ShieldScreen(
    onFixClick: (packageName: String, appName: String) -> Unit,
    viewModel: ShieldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auditItems by viewModel.auditItems.collectAsStateWithLifecycle()
    val score by viewModel.securityScore.collectAsStateWithLifecycle()

    if (uiState.isLoading && !uiState.hasScanned) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = BlueAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scanning your device...", color = TextSecondary)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    text = "Safe Anot?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Check first before you click.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }

        // Security Score
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                SecurityScoreRing(
                    scorePercent = score.scorePercent,
                    securedCount = score.securedItems,
                    totalCount = score.totalItems,
                )
            }
        }

        // Rescan button
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = { viewModel.runScan() }) {
                    Text("Rescan Device", color = BlueAccent)
                }
            }
        }

        // Grouped audit items by category
        val groupedItems = auditItems.groupBy { it.category }
        AppCategory.entries.forEach { category ->
            val items = groupedItems[category] ?: return@forEach

            item {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(items, key = { it.id }) { auditItem ->
                AppCard(
                    item = auditItem,
                    onFixClick = onFixClick,
                    onRecheckClick = { viewModel.runScan() },
                )
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
