/**
 * Room entity representing a known scam domain, stored in the scam_domains table.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scam_domains")
data class ScamDomainEntity(
    @PrimaryKey
    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "verdict")
    val verdict: String,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,
)
