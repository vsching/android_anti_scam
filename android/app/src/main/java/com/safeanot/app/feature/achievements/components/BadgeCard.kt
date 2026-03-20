/**
 * Composable card for displaying a single badge with locked/unlocked state.
 */
package com.safeanot.app.feature.achievements.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeanot.app.domain.model.BadgeProgress
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.TextSecondary
import com.safeanot.app.ui.theme.TextTertiary

@Composable
fun BadgeCard(
    badgeProgress: BadgeProgress,
    modifier: Modifier = Modifier,
) {
    val icon = badgeIcon(badgeProgress.badge.icon)
    val isUnlocked = badgeProgress.isUnlocked

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Icon(
                imageVector = if (isUnlocked) icon else Icons.Default.Lock,
                contentDescription = badgeProgress.badge.title,
                tint = if (isUnlocked) GreenAccent else TextTertiary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = badgeProgress.badge.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) BlueAccent else TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUnlocked) {
                    badgeProgress.badge.description
                } else {
                    badgeProgress.badge.conditionHint
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Maps Material Symbol icon names to Compose ImageVector instances.
 */
fun badgeIcon(name: String): ImageVector = when (name) {
    "shield" -> Icons.Default.Shield
    "search" -> Icons.Default.Search
    "local_fire_department" -> Icons.Default.LocalFireDepartment
    "school" -> Icons.Default.School
    "link" -> Icons.Default.Link
    "share" -> Icons.Default.Share
    "people" -> Icons.Default.People
    else -> Icons.Default.Shield
}
