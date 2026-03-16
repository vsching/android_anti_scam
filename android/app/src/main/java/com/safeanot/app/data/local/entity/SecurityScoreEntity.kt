/**
 * Room entity storing the calculated security score as a singleton row.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_scores")
data class SecurityScoreEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "total_items")
    val totalItems: Int = 0,

    @ColumnInfo(name = "secured_items")
    val securedItems: Int = 0,

    @ColumnInfo(name = "score_percent")
    val scorePercent: Int = 0,

    @ColumnInfo(name = "last_audit_date")
    val lastAuditDate: Long = 0L,
)
