/**
 * Room entity storing quiz session results.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "score_percent")
    val scorePercent: Int,

    @ColumnInfo(name = "correct_count")
    val correctCount: Int,

    @ColumnInfo(name = "question_count")
    val questionCount: Int,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long,
)
