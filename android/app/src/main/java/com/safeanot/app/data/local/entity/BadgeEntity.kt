/**
 * Room entity storing badge unlock state.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "badge_id")
    val badgeId: String,

    @ColumnInfo(name = "unlocked")
    val unlocked: Boolean = false,

    @ColumnInfo(name = "unlocked_at")
    val unlockedAt: Long? = null,
)
