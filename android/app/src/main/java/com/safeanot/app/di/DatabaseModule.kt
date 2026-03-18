/**
 * Hilt module providing the Room database instance and DAO dependencies.
 */
package com.safeanot.app.di

import android.content.Context
import androidx.room.Room
import com.safeanot.app.data.local.AlertsDao
import com.safeanot.app.data.local.AuditDao
import com.safeanot.app.data.local.ReminderConfigDao
import com.safeanot.app.data.local.SafeAnotDatabase
import com.safeanot.app.data.local.ScamDomainDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SafeAnotDatabase {
        return Room.databaseBuilder(
            context,
            SafeAnotDatabase::class.java,
            "safeanot_database",
        )
            .addMigrations(SafeAnotDatabase.MIGRATION_4_5)
            .build()
    }

    @Provides
    @Singleton
    fun provideAuditDao(database: SafeAnotDatabase): AuditDao {
        return database.auditDao()
    }

    @Provides
    @Singleton
    fun provideScamDomainDao(database: SafeAnotDatabase): ScamDomainDao {
        return database.scamDomainDao()
    }

    @Provides
    @Singleton
    fun provideAlertsDao(database: SafeAnotDatabase): AlertsDao {
        return database.alertsDao()
    }

    @Provides
    @Singleton
    fun provideReminderConfigDao(database: SafeAnotDatabase): ReminderConfigDao {
        return database.reminderConfigDao()
    }
}
