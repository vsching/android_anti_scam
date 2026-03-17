/**
 * Room database definition for Safe Anot? with audit items, security score, scam domain,
 * alerts, and reminder config tables.
 */
package com.safeanot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safeanot.app.data.local.entity.AlertEntity
import com.safeanot.app.data.local.entity.AuditItemEntity
import com.safeanot.app.data.local.entity.CheckResultCacheEntity
import com.safeanot.app.data.local.entity.ReminderConfigEntity
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
        AlertEntity::class,
        ReminderConfigEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class SafeAnotDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun scamDomainDao(): ScamDomainDao
    abstract fun alertsDao(): AlertsDao
    abstract fun reminderConfigDao(): ReminderConfigDao
}
