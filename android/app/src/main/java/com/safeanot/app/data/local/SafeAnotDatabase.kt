/**
 * Room database definition for Safe Anot? with audit items and security score tables.
 */
package com.safeanot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safeanot.app.data.local.entity.AuditItemEntity
import com.safeanot.app.data.local.entity.SecurityScoreEntity

@Database(
    entities = [
        AuditItemEntity::class,
        SecurityScoreEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SafeAnotDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
}
