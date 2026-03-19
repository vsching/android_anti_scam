/**
 * Room entity for persisting share analytics events locally.
 * Events are queued offline and synced to the backend periodically.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "share_events")
data class ShareEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "share_type")
    val shareType: String,

    @ColumnInfo(name = "content_id")
    val contentId: String,

    @ColumnInfo(name = "platform")
    val platform: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,
)
