/**
 * Room entity for cached scam alerts.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "scam_type") val scamType: String,
    @ColumnInfo(name = "severity", index = true) val severity: String,
    @ColumnInfo(name = "region", index = true) val region: String,
    @ColumnInfo(name = "report_count") val reportCount: Int,
    @ColumnInfo(name = "created_at", index = true) val createdAt: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)
