package com.safeanot.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizQuestionBankTest {

    @Test
    fun `question bank has at least 15 questions`() {
        assertTrue(
            "Expected >= 15 questions, got ${QuizQuestionBank.questions.size}",
            QuizQuestionBank.questions.size >= 15,
        )
    }

    @Test
    fun `all questions have valid correctIndex within options range`() {
        QuizQuestionBank.questions.forEach { question ->
            assertTrue(
                "Question ${question.id}: correctIndex ${question.correctIndex} out of range for ${question.options.size} options",
                question.correctIndex in question.options.indices,
            )
        }
    }

    @Test
    fun `all questions have at least 2 options`() {
        QuizQuestionBank.questions.forEach { question ->
            assertTrue(
                "Question ${question.id}: only ${question.options.size} option(s)",
                question.options.size >= 2,
            )
        }
    }

    @Test
    fun `all questions have non-empty scenario and explanation`() {
        QuizQuestionBank.questions.forEach { question ->
            assertTrue("Question ${question.id}: empty scenario", question.scenario.isNotBlank())
            assertTrue("Question ${question.id}: empty explanation", question.explanation.isNotBlank())
        }
    }

    @Test
    fun `all question IDs are unique`() {
        val ids = QuizQuestionBank.questions.map { it.id }
        assertEquals("Duplicate question IDs found", ids.size, ids.toSet().size)
    }

    @Test
    fun `getRandomQuiz returns requested count`() {
        val quiz = QuizQuestionBank.getRandomQuiz(count = 5)
        assertEquals(5, quiz.size)
    }

    @Test
    fun `getRandomQuiz returns all questions when count exceeds total`() {
        val quiz = QuizQuestionBank.getRandomQuiz(count = 100)
        assertEquals(QuizQuestionBank.questions.size, quiz.size)
    }

    @Test
    fun `getRandomQuiz returns shuffled order`() {
        // Run multiple times; at least one should differ from original order
        val originalIds = QuizQuestionBank.questions.take(5).map { it.id }
        val shuffledResults = (1..10).map {
            QuizQuestionBank.getRandomQuiz(5).map { it.id }
        }
        val anyDifferent = shuffledResults.any { it != originalIds }
        assertTrue("10 calls to getRandomQuiz all returned same order -- unlikely", anyDifferent)
    }
}
