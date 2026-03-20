/**
 * Composable card for displaying a single quiz question with selectable options.
 */
package com.safeanot.app.feature.quiz.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeanot.app.domain.model.QuizQuestion
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.ui.theme.DarkCard
import com.safeanot.app.ui.theme.TextSecondary

@Composable
fun QuizQuestionCard(
    question: QuizQuestion,
    questionNumber: Int,
    totalQuestions: Int,
    selectedAnswer: Int?,
    onSelectAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "Question $questionNumber of $totalQuestions",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = question.scenario,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(16.dp))

            question.options.forEachIndexed { index, option ->
                FilterChip(
                    selected = selectedAnswer == index,
                    onClick = { onSelectAnswer(index) },
                    label = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BlueAccent.copy(alpha = 0.2f),
                        selectedLabelColor = BlueAccent,
                    ),
                )
            }
        }
    }
}
