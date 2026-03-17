/**
 * Room entity storing the user's audit reminder configuration as a singleton row.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_config")
data class ReminderConfigEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Int = 7,

    @ColumnInfo(name = "last_reminder_date")
    val lastReminderDate: Long = 0L,
)
