/**
 * Composable card displaying a LinkVerdict result with color-coded verdict badge,
 * confidence bar, reason text, and share/check-another buttons.
 */
package com.safeanot.app.feature.check.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safeanot.app.domain.model.LinkVerdict
import com.safeanot.app.domain.model.VerdictType
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

private val VerdictGreen = Color(0xFF4CAF50)
private val VerdictRed = Color(0xFFF44336)
private val VerdictAmber = Color(0xFFFF9800)
private val VerdictGray = Color(0xFF9E9E9E)

private fun verdictColor(type: VerdictType): Color = when (type) {
    VerdictType.SAFE -> VerdictGreen
    VerdictType.DANGEROUS -> VerdictRed
    VerdictType.SUSPICIOUS -> VerdictAmber
    VerdictType.UNKNOWN, VerdictType.NOT_IN_DATABASE -> VerdictGray
}

@Composable
fun VerdictCard(
    verdict: LinkVerdict,
    onShareClick: () -> Unit,
    onCheckAnotherClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWarnContactsClick: (() -> Unit)? = null,
    onProtectFamilyClick: (() -> Unit)? = null,
) {
    val color = verdictColor(verdict.verdict)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            // Color bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(color),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Domain name
                Text(
                    text = verdict.domain,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Verdict badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = verdict.verdict.name.replace('_', ' '),
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reason text
                Text(
                    text = verdict.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confidence bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Confidence",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(verdict.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { verdict.confidence },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons — vertical stack for DANGEROUS, horizontal for others
                if (verdict.verdict == VerdictType.DANGEROUS &&
                    onWarnContactsClick != null &&
                    onProtectFamilyClick != null
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onShareClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Share Result")
                        }

                        Button(
                            onClick = onWarnContactsClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color.copy(alpha = 0.15f),
                                contentColor = color,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Warn My Contacts",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Button(
                            onClick = onProtectFamilyClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF7043),
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "Protect Your Family",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        OutlinedButton(
                            onClick = onCheckAnotherClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Check Another")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onCheckAnotherClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Check Another")
                        }

                        Button(
                            onClick = onShareClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Share Result")
                        }
                    }
                }
            }
        }
    }
}
