/**
 * Room entity for caching API check results with TTL, stored in the check_result_cache table.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_result_cache")
data class CheckResultCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "verdict")
    val verdict: String,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long,

    @ColumnInfo(name = "expires_at")
    val expiresAt: Long,
)
