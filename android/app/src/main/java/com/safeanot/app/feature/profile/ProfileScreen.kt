/**
 * Profile screen with user stats, guardian setup placeholder, settings toggles, and share button.
 */
package com.safeanot.app.feature.profile

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.AlertRegionFilter
import com.safeanot.app.feature.profile.components.AboutSection
import com.safeanot.app.feature.profile.components.EmergencyContactCard
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.util.Constants

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect {
            try {
                val intent = ShareHelper.createShareIntent()
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // No activity available to handle share intent
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Your Stats",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total audits completed: ${uiState.totalAudits}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.lastAuditDate ?: "No audits yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Security score: ${uiState.securityScore}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
        }

        // Guardian setup placeholder
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Guardian Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Help protect your family members. Coming soon in a future update.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // Region preference
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Region",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Affects which scam alerts you see first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        listOf(
                            AlertRegionFilter.MALAYSIA,
                            AlertRegionFilter.SINGAPORE,
                        ).forEach { region ->
                            FilterChip(
                                selected = uiState.selectedRegion == region,
                                onClick = { viewModel.setRegion(region) },
                                label = { Text(region.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BlueAccent.copy(alpha = 0.2f),
                                    selectedLabelColor = BlueAccent,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(
                                text = "Audit Reminders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Every ${uiState.reminderIntervalDays} days",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = uiState.remindersEnabled,
                            onCheckedChange = { viewModel.toggleReminders(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GreenAccent,
                                checkedTrackColor = GreenAccent.copy(alpha = 0.3f),
                            ),
                        )
                    }

                    // Interval selector (only visible when reminders are enabled)
                    if (uiState.remindersEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Reminder Interval",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Constants.REMINDER_INTERVAL_OPTIONS.forEach { days ->
                                FilterChip(
                                    selected = uiState.reminderIntervalDays == days,
                                    onClick = { viewModel.setReminderInterval(days) },
                                    label = { Text("${days}d") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlueAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = BlueAccent,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scam Alert Notifications toggle
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(
                                text = "Scam Alert Notifications",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                            )
                            Text(
                                text = "Get notified about new scam alerts",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Switch(
                            checked = uiState.scamAlertsEnabled,
                            onCheckedChange = { viewModel.toggleScamAlerts(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GreenAccent,
                                checkedTrackColor = GreenAccent.copy(alpha = 0.3f),
                            ),
                        )
                    }
                }
            }
        }

        // Report a Scam / Get Help section
        if (uiState.emergencyContacts.isNotEmpty()) {
            item {
                Text(
                    text = "Report a Scam / Get Help",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(
                count = uiState.emergencyContacts.size,
                key = { uiState.emergencyContacts[it].name },
            ) { index ->
                EmergencyContactCard(contact = uiState.emergencyContacts[index])
            }
        }

        // About section
        item {
            AboutSection(
                appVersion = uiState.appVersion,
                buildNumber = uiState.buildNumber,
                legalLinks = uiState.legalLinks,
            )
        }

        // Share button
        item {
            Button(
                onClick = { viewModel.shareApp() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = "Share Safe Anot? with Friends",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
