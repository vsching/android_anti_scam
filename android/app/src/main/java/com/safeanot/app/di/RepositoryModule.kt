/**
 * Hilt module binding the AuditRepository interface to its concrete implementation.
 */
package com.safeanot.app.di

import com.safeanot.app.data.repository.AuditRepositoryImpl
import com.safeanot.app.domain.repository.AuditRepository
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
}
