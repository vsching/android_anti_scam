/**
 * Quiz screen for the "Spot the Scam" challenge.
 * Shows questions one at a time, then results with per-question review.
 */
package com.safeanot.app.feature.quiz

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.feature.check.ShareEvent
import com.safeanot.app.feature.quiz.components.QuizQuestionCard
import com.safeanot.app.feature.quiz.components.QuizResultCard
import com.safeanot.app.ui.theme.BlueAccent
import com.safeanot.app.util.ShareIntentFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Start quiz on first composition
    LaunchedEffect(Unit) {
        viewModel.startQuiz()
    }

    // Handle share events
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { event ->
            try {
                when (event) {
                    is ShareEvent.TextOnly -> {
                        val intent = ShareIntentFactory.createTextShare(event.text)
                        context.startActivity(intent)
                    }
                    else -> { /* Quiz only emits TextOnly events */ }
                }
            } catch (e: Exception) {
                Log.e("QuizScreen", "Failed to share", e)
            }
        }
    }

    // Handle badge unlock events
    LaunchedEffect(Unit) {
        viewModel.badgeUnlockEvent.collect { badgeType ->
            val badgeName = when (badgeType) {
                BadgeType.SCAM_SPOTTER -> "Scam Spotter"
                else -> badgeType.name
            }
            snackbarHostState.showSnackbar("Badge Unlocked: $badgeName!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spot the Scam") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!uiState.isComplete && uiState.questions.isNotEmpty()) {
                // Question view
                val currentQuestion = uiState.questions[uiState.currentIndex]
                val selectedAnswer = uiState.selectedAnswers[uiState.currentIndex]

                item {
                    QuizQuestionCard(
                        question = currentQuestion,
                        questionNumber = uiState.currentIndex + 1,
                        totalQuestions = uiState.questions.size,
                        selectedAnswer = selectedAnswer,
                        onSelectAnswer = { viewModel.selectAnswer(uiState.currentIndex, it) },
                    )
                }

                item {
                    val isLastQuestion = uiState.currentIndex == uiState.questions.size - 1
                    Button(
                        onClick = { viewModel.nextQuestion() },
                        enabled = selectedAnswer != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (isLastQuestion) "Submit" else "Next",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else if (uiState.isComplete) {
                // Results view
                item {
                    QuizResultCard(
                        scorePercent = uiState.scorePercent,
                        correctCount = uiState.correctCount,
                        totalCount = uiState.questions.size,
                        questions = uiState.questions,
                        selectedAnswers = uiState.selectedAnswers,
                        onShare = { viewModel.onShare() },
                        onRetry = { viewModel.startQuiz() },
                    )
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
