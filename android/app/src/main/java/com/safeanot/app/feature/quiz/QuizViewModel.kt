/**
 * ViewModel for the "Spot the Scam" quiz feature.
 * Manages quiz state machine: question -> answer -> next -> results.
 * 5 questions per session, badge unlock at 100%.
 * No Context dependency -- share event emitted as domain data via Channel.
 */
package com.safeanot.app.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.QuizQuestion
import com.safeanot.app.domain.model.QuizQuestionBank
import com.safeanot.app.domain.model.ShareType
import com.safeanot.app.domain.repository.QuizRepository
import com.safeanot.app.domain.usecase.UnlockBadgeUseCase
import com.safeanot.app.feature.check.ShareEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val isComplete: Boolean = false,
    val scorePercent: Int = 0,
    val correctCount: Int = 0,
    val sessionId: String = "",
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val unlockBadgeUseCase: UnlockBadgeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _shareEvents = Channel<ShareEvent>(Channel.BUFFERED)
    val shareEvents = _shareEvents.receiveAsFlow()

    private val _badgeUnlockEvent = Channel<BadgeType>(Channel.BUFFERED)
    val badgeUnlockEvent = _badgeUnlockEvent.receiveAsFlow()

    fun startQuiz() {
        val questions = QuizQuestionBank.getRandomQuiz(count = 5)
        _uiState.value = QuizUiState(
            questions = questions,
            sessionId = UUID.randomUUID().toString(),
        )
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _uiState.value
        if (current.isComplete) return
        _uiState.value = current.copy(
            selectedAnswers = current.selectedAnswers + (questionIndex to optionIndex),
        )
    }

    fun nextQuestion() {
        val current = _uiState.value
        if (current.isComplete) return

        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.questions.size) {
            completeQuiz()
        } else {
            _uiState.value = current.copy(currentIndex = nextIndex)
        }
    }

    private fun completeQuiz() {
        val current = _uiState.value
        val correctCount = current.questions.indices.count { index ->
            current.selectedAnswers[index] == current.questions[index].correctIndex
        }
        val questionCount = current.questions.size
        val scorePercent = if (questionCount > 0) {
            (correctCount * 100) / questionCount
        } else {
            0
        }

        _uiState.value = current.copy(
            isComplete = true,
            scorePercent = scorePercent,
            correctCount = correctCount,
        )

        viewModelScope.launch {
            quizRepository.saveResult(
                sessionId = current.sessionId,
                scorePercent = scorePercent,
                correctCount = correctCount,
                questionCount = questionCount,
            )

            if (scorePercent == 100) {
                val newlyUnlocked = unlockBadgeUseCase(BadgeType.SCAM_SPOTTER)
                if (newlyUnlocked) {
                    _badgeUnlockEvent.send(BadgeType.SCAM_SPOTTER)
                }
            }
        }
    }

    fun onShare() {
        val state = _uiState.value
        if (!state.isComplete) return

        viewModelScope.launch {
            _shareEvents.send(
                ShareEvent.TextOnly(
                    text = "I scored ${state.scorePercent}% on SafeAnot's Spot the Scam quiz! Can you beat my score? Download: https://safeanot.com",
                    shareType = ShareType.SCORE,
                    contentId = "quiz_${state.scorePercent}",
                )
            )
        }
    }
}
