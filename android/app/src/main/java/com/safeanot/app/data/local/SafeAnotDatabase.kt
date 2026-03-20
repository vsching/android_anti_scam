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
import com.safeanot.app.data.local.entity.GuardianPairingEntity
import com.safeanot.app.data.local.entity.ShareEventEntity
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
        ShareEventEntity::class,
        GuardianPairingEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
abstract class SafeAnotDatabase : RoomDatabase() {
    abstract fun auditDao(): AuditDao
    abstract fun scamDomainDao(): ScamDomainDao
    abstract fun alertsDao(): AlertsDao
    abstract fun reminderConfigDao(): ReminderConfigDao
    abstract fun shareEventDao(): ShareEventDao
    abstract fun guardianDao(): GuardianDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE security_scores ADD COLUMN audit_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE security_scores ADD COLUMN last_full_audit_at INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS share_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        share_type TEXT NOT NULL,
                        content_id TEXT NOT NULL,
                        platform TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        synced INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS guardian_pairings (
                        id TEXT NOT NULL PRIMARY KEY,
                        device_id TEXT NOT NULL,
                        paired_device_id TEXT NOT NULL,
                        role TEXT NOT NULL,
                        label TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE guardian_pairings ADD COLUMN last_security_score INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE guardian_pairings ADD COLUMN last_heartbeat_at INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE guardian_pairings ADD COLUMN play_protect_enabled INTEGER DEFAULT NULL")
            }
        }
    }
}
