/**
 * Small chip showing the user's current streak with a flame icon.
 * Displayed below the security score ring on the Shield screen.
 */
package com.safeanot.app.feature.shield.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safeanot.app.ui.theme.GreenAccent

@Composable
fun StreakChip(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = { },
        label = {
            Text(text = "$streakDays day streak")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = GreenAccent,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = GreenAccent.copy(alpha = 0.15f),
            labelColor = GreenAccent,
        ),
        modifier = modifier,
    )
}
