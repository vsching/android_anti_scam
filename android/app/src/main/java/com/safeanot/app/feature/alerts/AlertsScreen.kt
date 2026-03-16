/**
 * Scam Alerts feed screen displaying scam alert cards with expandable details.
 * Static data for v1; will integrate with a remote API in v2.
 */
package com.safeanot.app.feature.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.OrangeAccent
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(uiState.alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AlertCard(
    alert: ScamAlert,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val severityColor = when (alert.severity) {
        "HIGH" -> RedAccent
        "MEDIUM" -> OrangeAccent
        else -> TextSecondary
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(severityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = alert.severity,
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = alert.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = alert.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = alert.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}
