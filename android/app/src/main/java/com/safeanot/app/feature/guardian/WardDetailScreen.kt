/**
 * Ward Detail screen showing detailed security information for a single ward.
 * Displays large score ring, heartbeat history chart, Play Protect status, and unlink button.
 */
package com.safeanot.app.feature.guardian

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.HeartbeatEntry
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.feature.shield.components.SecurityScoreRing
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.ScoreAmber
import com.safeanot.app.ui.theme.ScoreGreen
import com.safeanot.app.ui.theme.ScoreRed
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.util.RelativeTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: WardDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnlinkDialog by remember { mutableStateOf(false) }

    // Navigate back on successful unlink
    LaunchedEffect(uiState.unlinkSuccess) {
        if (uiState.unlinkSuccess) {
            onNavigateBack()
        }
    }

    if (showUnlinkDialog) {
        UnlinkConfirmationDialog(
            wardName = uiState.displayName,
            onConfirm = {
                showUnlinkDialog = false
                viewModel.unlink()
            },
            onDismiss = { showUnlinkDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.displayName.ifEmpty { "Ward Details" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BlueAccent)
                }
            }

            uiState.error != null && uiState.securityScore == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = RedAccent,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Large security score ring
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        ) {
                            SecurityScoreRing(
                                scorePercent = uiState.securityScore ?: 0,
                                securedCount = 0,
                                totalCount = 0,
                                scoreBand = uiState.scoreBand ?: ScoreBand.RED,
                            )
                        }
                    }

                    // Status cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Last heartbeat card
                            StatusCard(
                                title = "Last Check-in",
                                value = uiState.lastHeartbeat?.let {
                                    RelativeTimeFormatter.format(it)
                                } ?: "Never",
                                modifier = Modifier.weight(1f),
                            )

                            // Play Protect card
                            StatusCard(
                                title = "Play Protect",
                                value = when (uiState.playProtectEnabled) {
                                    true -> "Enabled"
                                    false -> "Disabled"
                                    null -> "Unknown"
                                },
                                valueColor = when (uiState.playProtectEnabled) {
                                    true -> GreenAccent
                                    false -> RedAccent
                                    null -> TextSecondary
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = when (uiState.playProtectEnabled) {
                                            true -> GreenAccent
                                            false -> RedAccent
                                            null -> TextSecondary
                                        },
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Score history chart (7-day)
                    if (uiState.heartbeatHistory.isNotEmpty()) {
                        item {
                            Text(
                                text = "Score History (7 days)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        item {
                            ScoreHistoryChart(
                                heartbeats = uiState.heartbeatHistory,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                            )
                        }
                    }

                    // Error banner (non-blocking)
                    if (uiState.error != null && uiState.securityScore != null) {
                        item {
                            Text(
                                text = uiState.error ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = RedAccent,
                            )
                        }
                    }

                    // Unlink button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showUnlinkDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isUnlinking,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = RedAccent,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            if (uiState.isUnlinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = RedAccent,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Unlink This Device")
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
    icon: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor,
                )
            }
        }
    }
}

/**
 * Simple Canvas-based bar chart showing security scores over time.
 */
@Composable
private fun ScoreHistoryChart(
    heartbeats: List<HeartbeatEntry>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            if (heartbeats.isEmpty()) return@Canvas

            val barCount = heartbeats.size
            val barSpacing = 4.dp.toPx()
            val totalSpacing = barSpacing * (barCount - 1)
            val barWidth = (size.width - totalSpacing) / barCount
            val maxHeight = size.height

            heartbeats.forEachIndexed { index, entry ->
                val barHeight = (entry.securityScore / 100f) * maxHeight
                val x = index * (barWidth + barSpacing)
                val y = maxHeight - barHeight

                val barColor = when {
                    entry.securityScore >= 80 -> ScoreGreen
                    entry.securityScore >= 50 -> ScoreAmber
                    else -> ScoreRed
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun UnlinkConfirmationDialog(
    wardName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlink Device") },
        text = {
            Text("Are you sure you want to stop monitoring ${wardName.ifEmpty { "this device" }}? You will no longer receive security alerts.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Unlink", color = RedAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
