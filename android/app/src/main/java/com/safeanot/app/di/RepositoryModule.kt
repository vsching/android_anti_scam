/**
 * Hilt module binding repository interfaces to their concrete implementations.
 */
package com.safeanot.app.di

import com.safeanot.app.data.repository.AlertsRepositoryImpl
import com.safeanot.app.data.repository.AuditRepositoryImpl
import com.safeanot.app.data.repository.BadgeRepositoryImpl
import com.safeanot.app.data.repository.GuardianRepositoryImpl
import com.safeanot.app.data.repository.LinkCheckRepositoryImpl
import com.safeanot.app.data.repository.QuizRepositoryImpl
import com.safeanot.app.data.repository.ShareEventRepositoryImpl
import com.safeanot.app.data.repository.StreakRepositoryImpl
import com.safeanot.app.data.repository.SyncRepositoryImpl
import com.safeanot.app.data.repository.UserPreferencesRepositoryImpl
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.BadgeRepository
import com.safeanot.app.domain.repository.GuardianRepository
import com.safeanot.app.domain.repository.LinkCheckRepository
import com.safeanot.app.domain.repository.QuizRepository
import com.safeanot.app.domain.repository.ShareEventRepository
import com.safeanot.app.domain.repository.StreakRepository
import com.safeanot.app.domain.repository.SyncRepository
import com.safeanot.app.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuditRepository(impl: AuditRepositoryImpl): AuditRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindLinkCheckRepository(impl: LinkCheckRepositoryImpl): LinkCheckRepository

    @Binds
    @Singleton
    abstract fun bindAlertsRepository(impl: AlertsRepositoryImpl): AlertsRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindShareEventRepository(impl: ShareEventRepositoryImpl): ShareEventRepository

    @Binds
    @Singleton
    abstract fun bindGuardianRepository(impl: GuardianRepositoryImpl): GuardianRepository

    @Binds
    @Singleton
    abstract fun bindStreakRepository(impl: StreakRepositoryImpl): StreakRepository

    @Binds
    @Singleton
    abstract fun bindBadgeRepository(impl: BadgeRepositoryImpl): BadgeRepository

    @Binds
    @Singleton
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository
}
