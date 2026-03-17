/**
 * Animated circular progress indicator displaying the security score percentage.
 * Color transitions are driven by the ScoreBand from the domain model.
 */
package com.safeanot.app.feature.shield.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeanot.app.domain.model.ScoreBand
import com.safeanot.app.ui.theme.DarkBorder
import com.safeanot.app.ui.theme.ScoreAmber
import com.safeanot.app.ui.theme.ScoreGreen
import com.safeanot.app.ui.theme.ScoreRed
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun SecurityScoreRing(
    scorePercent: Int,
    securedCount: Int,
    totalCount: Int,
    scoreBand: ScoreBand = ScoreBand.fromPercent(scorePercent),
    modifier: Modifier = Modifier,
) {
    val scoreColor = when (scoreBand) {
        ScoreBand.GREEN -> ScoreGreen
        ScoreBand.AMBER -> ScoreAmber
        ScoreBand.RED -> ScoreRed
    }

    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "score_animation",
    )

    LaunchedEffect(scorePercent) {
        targetProgress = scorePercent / 100f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(180.dp),
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            // Background track
            drawArc(
                color = DarkBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )

            // Progress arc
            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${scorePercent}%",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                ),
                color = scoreColor,
            )
            Text(
                text = "$securedCount of $totalCount secured",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}
