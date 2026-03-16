/**
 * Composable card for displaying an individual app's audit status.
 * Shows app icon placeholder, name, status badge, fix button, and expandable "why" text.
 */
package com.safeanot.app.feature.shield.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.StatusNeedsReview
import com.safeanot.app.ui.theme.StatusNotInstalled
import com.safeanot.app.ui.theme.StatusSecured
import com.safeanot.app.ui.theme.StatusSkipped
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun AppCard(
    item: AuditItem,
    onFixClick: (packageName: String, appName: String) -> Unit,
    onRecheckClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (item.status) {
        AuditStatus.SECURED -> StatusSecured
        AuditStatus.NEEDS_REVIEW -> StatusNeedsReview
        AuditStatus.NOT_INSTALLED -> StatusNotInstalled
        AuditStatus.SKIPPED -> StatusSkipped
    }

    val statusLabel = when (item.status) {
        AuditStatus.SECURED -> "Secured"
        AuditStatus.NEEDS_REVIEW -> "Needs Review"
        AuditStatus.NOT_INSTALLED -> "Not Installed"
        AuditStatus.SKIPPED -> "Skipped"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // App icon placeholder (colored circle with short code)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                ) {
                    Text(
                        text = item.appName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Expandable section
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = when (item.status) {
                            AuditStatus.NEEDS_REVIEW -> "This app can install APK files, which scammers may exploit to install malware on your device."
                            AuditStatus.SECURED -> "You have secured this app. Tap Re-check to verify."
                            AuditStatus.NOT_INSTALLED -> "This app is not installed on your device."
                            AuditStatus.SKIPPED -> "You skipped this app. Consider fixing it for better security."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        when (item.status) {
                            AuditStatus.NEEDS_REVIEW, AuditStatus.SKIPPED -> {
                                Button(
                                    onClick = { onFixClick(item.packageName, item.appName) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlueAccent,
                                    ),
                                ) {
                                    Text("Fix")
                                }
                            }
                            AuditStatus.SECURED -> {
                                Button(
                                    onClick = onRecheckClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusSecured.copy(alpha = 0.2f),
                                        contentColor = StatusSecured,
                                    ),
                                ) {
                                    Text("Re-check")
                                }
                            }
                            AuditStatus.NOT_INSTALLED -> {
                                // No action for uninstalled apps
                            }
                        }
                    }
                }
            }
        }
    }
}
