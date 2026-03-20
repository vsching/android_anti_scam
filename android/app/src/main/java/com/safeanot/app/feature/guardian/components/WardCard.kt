/**
 * Card composable displaying a ward's security status summary.
 * Shows display name, security score ring, relative heartbeat time, and status badges.
 */
package com.safeanot.app.feature.guardian.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.feature.guardian.WardUiModel
import com.safeanot.app.feature.shield.components.SecurityScoreRing
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.OrangeAccent
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.util.RelativeTimeFormatter

@Composable
fun WardCard(
    ward: WardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Security score ring (compact 48dp size)
            if (ward.securityScore != null) {
                SecurityScoreRing(
                    scorePercent = ward.securityScore,
                    securedCount = 0,
                    totalCount = 0,
                    scoreBand = ward.scoreBand ?: ScoreBand.fromPercent(ward.securityScore),
                    modifier = Modifier.size(48.dp),
                )
            } else {
                // Placeholder when no score is available
                SecurityScoreRing(
                    scorePercent = 0,
                    securedCount = 0,
                    totalCount = 0,
                    scoreBand = ScoreBand.RED,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Ward info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = ward.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Relative time
                if (ward.lastHeartbeat != null) {
                    Text(
                        text = RelativeTimeFormatter.format(ward.lastHeartbeat),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                } else {
                    Text(
                        text = "No heartbeat yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Status badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Play Protect badge
                    if (ward.playProtectEnabled != null) {
                        val protectColor = if (ward.playProtectEnabled) GreenAccent else RedAccent
                        val protectLabel = if (ward.playProtectEnabled) "Protected" else "Unprotected"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = protectLabel,
                                tint = protectColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = protectLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = protectColor,
                            )
                        }
                    }

                    // "Went Dark" amber badge if stale (>24h since last heartbeat)
                    if (ward.isStale) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Went Dark",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = OrangeAccent.copy(alpha = 0.2f),
                                labelColor = OrangeAccent,
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = OrangeAccent.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }
        }
    }
}
