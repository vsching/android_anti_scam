package com.safeanot.app.feature.quiz

import com.safeanot.app.domain.model.BadgeType
import com.safeanot.app.domain.model.QuizResult
import com.safeanot.app.domain.repository.QuizRepository
import com.safeanot.app.domain.usecase.SaveQuizResultUseCase
import com.safeanot.app.domain.usecase.UnlockBadgeUseCase
import com.safeanot.app.feature.check.ShareEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeQuizRepository
    private lateinit var fakeSaveQuizResult: SaveQuizResultUseCase
    private lateinit var fakeUnlockBadge: FakeUnlockBadgeUseCase
    private lateinit var viewModel: QuizViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeQuizRepository()
        fakeSaveQuizResult = SaveQuizResultUseCase(fakeRepository)
        fakeUnlockBadge = FakeUnlockBadgeUseCase()
        viewModel = QuizViewModel(fakeSaveQuizResult, fakeUnlockBadge)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startQuiz loads 5 questions`() {
        viewModel.startQuiz()
        val state = viewModel.uiState.value
        assertEquals(5, state.questions.size)
        assertEquals(0, state.currentIndex)
        assertFalse(state.isComplete)
        assertTrue(state.sessionId.isNotEmpty())
    }

    @Test
    fun `selectAnswer records answer for current question`() {
        viewModel.startQuiz()
        viewModel.selectAnswer(0, 2)
        assertEquals(2, viewModel.uiState.value.selectedAnswers[0])
    }

    @Test
    fun `nextQuestion advances index`() {
        viewModel.startQuiz()
        viewModel.selectAnswer(0, 0)
        viewModel.nextQuestion()
        assertEquals(1, viewModel.uiState.value.currentIndex)
    }

    @Test
    fun `completing quiz calculates score and saves result`() = runTest {
        viewModel.startQuiz()
        val questions = viewModel.uiState.value.questions

        // Answer all questions with correct answers
        questions.forEachIndexed { index, question ->
            viewModel.selectAnswer(index, question.correctIndex)
            viewModel.nextQuestion()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(100, state.scorePercent)
        assertEquals(5, state.correctCount)
        assertEquals(1, fakeRepository.savedResults.size)
    }

    @Test
    fun `100 percent score triggers scam spotter badge unlock`() = runTest {
        viewModel.startQuiz()
        val questions = viewModel.uiState.value.questions

        questions.forEachIndexed { index, question ->
            viewModel.selectAnswer(index, question.correctIndex)
            viewModel.nextQuestion()
        }

        advanceUntilIdle()

        assertTrue(fakeUnlockBadge.unlockedBadges.contains(BadgeType.SCAM_SPOTTER))
    }

    @Test
    fun `less than 100 percent does not unlock badge`() = runTest {
        viewModel.startQuiz()
        val questions = viewModel.uiState.value.questions

        // Answer first question wrong, rest correct
        viewModel.selectAnswer(0, (questions[0].correctIndex + 1) % questions[0].options.size)
        viewModel.nextQuestion()

        for (i in 1 until questions.size) {
            viewModel.selectAnswer(i, questions[i].correctIndex)
            viewModel.nextQuestion()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertTrue(state.scorePercent < 100)
        assertFalse(fakeUnlockBadge.unlockedBadges.contains(BadgeType.SCAM_SPOTTER))
    }

    @Test
    fun `score calculation is correct for partial answers`() = runTest {
        viewModel.startQuiz()
        val questions = viewModel.uiState.value.questions

        // Answer 3 out of 5 correctly
        for (i in questions.indices) {
            val answer = if (i < 3) {
                questions[i].correctIndex
            } else {
                (questions[i].correctIndex + 1) % questions[i].options.size
            }
            viewModel.selectAnswer(i, answer)
            viewModel.nextQuestion()
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(60, state.scorePercent)
        assertEquals(3, state.correctCount)
    }

    @Test
    fun `onShare emits text share event with score`() = runTest {
        viewModel.startQuiz()
        val questions = viewModel.uiState.value.questions

        questions.forEachIndexed { index, question ->
            viewModel.selectAnswer(index, question.correctIndex)
            viewModel.nextQuestion()
        }
        advanceUntilIdle()

        var shareEvent: ShareEvent? = null
        val job = launch {
            shareEvent = viewModel.shareEvents.first()
        }

        viewModel.onShare()
        advanceUntilIdle()
        job.join()

        assertTrue(shareEvent is ShareEvent.TextOnly)
        val text = (shareEvent as ShareEvent.TextOnly).text
        assertTrue(text.contains("100%"))
    }

    // --- Fakes ---

    private class FakeQuizRepository : QuizRepository {
        val savedResults = mutableListOf<SavedResult>()

        data class SavedResult(
            val sessionId: String,
            val scorePercent: Int,
            val correctCount: Int,
            val questionCount: Int,
        )

        override suspend fun saveResult(
            sessionId: String,
            scorePercent: Int,
            correctCount: Int,
            questionCount: Int,
        ) {
            savedResults.add(SavedResult(sessionId, scorePercent, correctCount, questionCount))
        }

        override fun observeResults(): Flow<List<QuizResult>> = MutableStateFlow(emptyList())
        override suspend fun getBestScore(): Int? = null
    }

    private class FakeUnlockBadgeUseCase : UnlockBadgeUseCase(
        object : com.safeanot.app.domain.repository.BadgeRepository {
            override fun observeAllBadges() = MutableStateFlow(emptyList<com.safeanot.app.domain.model.BadgeProgress>())
            override fun observeUnlockedCount() = MutableStateFlow(0)
            override suspend fun unlockBadge(type: BadgeType) = true
        }
    ) {
        val unlockedBadges = mutableListOf<BadgeType>()

        override suspend fun invoke(type: BadgeType): Boolean {
            unlockedBadges.add(type)
            return true
        }
    }
}
