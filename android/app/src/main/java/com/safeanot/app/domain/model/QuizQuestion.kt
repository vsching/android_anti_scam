/**
 * Domain model for a single quiz question in the "Spot the Scam" challenge.
 */
package com.safeanot.app.domain.model

data class QuizQuestion(
    val id: String,
    val scenario: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)
