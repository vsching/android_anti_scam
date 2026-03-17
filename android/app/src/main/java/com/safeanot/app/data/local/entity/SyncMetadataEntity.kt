/**
 * Room entity for tracking domain list sync state, stored in the sync_metadata table.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "last_sync_version")
    val lastSyncVersion: String,

    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long,

    @ColumnInfo(name = "domain_count")
    val domainCount: Int,
)
