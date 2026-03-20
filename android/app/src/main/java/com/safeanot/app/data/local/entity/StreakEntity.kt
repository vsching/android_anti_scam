/**
 * Room entity storing the user's streak data as a singleton row.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "current_streak")
    val currentStreak: Int = 0,

    @ColumnInfo(name = "longest_streak")
    val longestStreak: Int = 0,

    @ColumnInfo(name = "last_check_date")
    val lastCheckDate: Long = 0L,

    @ColumnInfo(name = "streak_start_date")
    val streakStartDate: Long = 0L,
)
