/**
 * Main Shield screen showing the security score ring and audit item cards grouped by category.
 * This is the primary/first screen of the app -- the phone audit dashboard.
 * Supports pull-to-refresh and includes a Play Protect advisory card.
 */
package com.safeanot.app.feature.shield

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.AppCategory
import com.safeanot.app.domain.model.CardFormat
import com.safeanot.app.domain.model.SharePlatform
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.feature.check.ShareEvent
import com.safeanot.app.feature.shield.components.AppCard
import com.safeanot.app.feature.shield.components.PlayProtectCard
import com.safeanot.app.feature.shield.components.SecurityScoreRing
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.util.ShareImageCache
import com.safeanot.app.util.ShareIntentFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScreen(
    onFixClick: (packageName: String, appName: String) -> Unit,
    viewModel: ShieldViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val auditItems by viewModel.auditItems.collectAsStateWithLifecycle()
    val score by viewModel.securityScore.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Collect share events from the ViewModel and handle intent creation in UI
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { event ->
            try {
                when (event) {
                    is ShareEvent.ImageWithText -> {
                        val uri = ShareImageCache.saveBitmap(context, event.bitmap, "score")
                        val intent = ShareIntentFactory.createImageShare(uri, event.text)
                        context.startActivity(intent)
                    }
                    is ShareEvent.BitmapOnly -> {
                        val uri = ShareImageCache.saveBitmap(context, event.bitmap, "score")
                        val intent = ShareIntentFactory.createImageShare(uri)
                        context.startActivity(intent)
                    }
                    is ShareEvent.TextOnly -> {
                        val intent = ShareIntentFactory.createTextShare(event.text)
                        context.startActivity(intent)
                    }
                }
                viewModel.onShareCompleted(
                    ShareType.SCORE,
                    "security_score",
                    SharePlatform.GENERIC,
                )
            } catch (e: Exception) {
                Log.e("ShieldScreen", "Failed to share", e)
            }
        }
    }

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

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.onRefresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    SecurityScoreRing(
                        scorePercent = score.scorePercent,
                        securedCount = score.securedItems,
                        totalCount = score.totalItems,
                        scoreBand = score.band,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Card format toggle + share button
                    var selectedFormat by remember { mutableStateOf(CardFormat.SQUARE) }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = selectedFormat == CardFormat.SQUARE,
                            onClick = { selectedFormat = CardFormat.SQUARE },
                            label = { Text("Square") },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedFormat == CardFormat.VERTICAL,
                            onClick = { selectedFormat = CardFormat.VERTICAL },
                            label = { Text("Vertical") },
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.shareScore(selectedFormat) },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "Share My Score",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Error banner (non-blocking)
            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedAccent,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            // Play Protect card
            item {
                PlayProtectCard()
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
                        onRecheckClick = { viewModel.onRefresh() },
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
