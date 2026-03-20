/**
 * Domain model representing the result of a completed quiz session.
 */
package com.safeanot.app.domain.model

data class QuizResult(
    val id: Long,
    val sessionId: String,
    val scorePercent: Int,
    val correctCount: Int,
    val questionCount: Int,
    val completedAt: Long,
)
