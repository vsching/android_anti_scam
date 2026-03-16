/**
 * Composable card displaying a URL verdict result with color-coded status
 * (DANGEROUS=red, SUSPICIOUS=orange, SAFE=green) and a share button.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safeanot.app.feature.check.Verdict
import com.safeanot.app.feature.check.VerdictResult
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.GreenAccent
import com.safeanot.app.ui.theme.OrangeAccent
import com.safeanot.app.ui.theme.RedAccent
import com.safeanot.app.ui.theme.TextPrimary
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun VerdictCard(
    result: VerdictResult,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (verdictColor, verdictLabel) = when (result.verdict) {
        Verdict.DANGEROUS -> RedAccent to "DANGEROUS"
        Verdict.SUSPICIOUS -> OrangeAccent to "SUSPICIOUS"
        Verdict.SAFE -> GreenAccent to "SAFE"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Verdict badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(verdictColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = verdictLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = verdictColor,
                        fontWeight = FontWeight.Bold,
                    )
                }

                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share verdict",
                        tint = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.url,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
