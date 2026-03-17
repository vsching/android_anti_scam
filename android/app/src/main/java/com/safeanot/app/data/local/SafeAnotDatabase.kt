/**
 * Room database definition for Safe Anot? with audit items, security score, and scam domain tables.
 */
package com.safeanot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safeanot.app.data.local.entity.AuditItemEntity
import com.safeanot.app.data.local.entity.CheckResultCacheEntity
import com.safeanot.app.data.local.entity.ScamDomainEntity
import com.safeanot.app.data.local.entity.SecurityScoreEntity
import com.safeanot.app.data.local.entity.SyncMetadataEntity

@Database(
    entities = [
        AuditItemEntity::class,
        SecurityScoreEntity::class,
        ScamDomainEntity::class,
        SyncMetadataEntity::class,
        CheckResultCacheEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SafeAnotDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun scamDomainDao(): ScamDomainDao
}
