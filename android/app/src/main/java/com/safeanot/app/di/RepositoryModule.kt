/**
 * Hilt module binding repository interfaces to their concrete implementations.
 */
package com.safeanot.app.di

import com.safeanot.app.data.repository.AlertsRepositoryImpl
import com.safeanot.app.data.repository.AuditRepositoryImpl
import com.safeanot.app.data.repository.LinkCheckRepositoryImpl
import com.safeanot.app.data.repository.SyncRepositoryImpl
import com.safeanot.app.data.repository.UserPreferencesRepositoryImpl
import com.safeanot.app.domain.repository.AlertsRepository
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.domain.repository.LinkCheckRepository
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
}
