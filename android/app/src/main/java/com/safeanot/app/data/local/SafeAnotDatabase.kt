/**
 * Room database definition for Safe Anot? with audit items, security score, scam domain,
 * alerts, and reminder config tables.
 */
package com.safeanot.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
    exportSchema = true,
)
abstract class SafeAnotDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun scamDomainDao(): ScamDomainDao
    abstract fun alertsDao(): AlertsDao
    abstract fun reminderConfigDao(): ReminderConfigDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE security_scores ADD COLUMN audit_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE security_scores ADD COLUMN last_full_audit_at INTEGER")
            }
        }
    }
}
