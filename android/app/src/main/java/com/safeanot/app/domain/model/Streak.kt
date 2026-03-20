/**
 * Domain model representing the user's streak data.
 */
package com.safeanot.app.domain.model

data class Streak(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCheckDate: Long = 0L,
    val streakStartDate: Long = 0L,
)
