/**
 * Room entity representing a tracked app's audit status, stored in the audit_items table.
 */
package com.safeanot.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_items")
data class AuditItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "app_name")
    val appName: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "status")
    val status: String = "NEEDS_REVIEW",

    @ColumnInfo(name = "last_checked")
    val lastChecked: Long = System.currentTimeMillis(),
)
