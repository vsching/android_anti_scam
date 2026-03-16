/**
 * Repository implementation that bridges Room database operations with the domain layer.
 * Uses PackageChecker to determine which tracked apps are installed on the device.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.AuditDao
import com.safeanot.app.data.local.entity.AuditItemEntity
import com.safeanot.app.data.local.entity.SecurityScoreEntity
import com.safeanot.app.domain.model.AppCategory
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.util.Constants
import com.safeanot.app.util.PackageChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepositoryImpl @Inject constructor(
    private val auditDao: AuditDao,
    private val packageChecker: PackageChecker,
) : AuditRepository {

    override fun getAllAuditItems(): Flow<List<AuditItem>> {
        return auditDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAuditItemsByCategory(category: String): Flow<List<AuditItem>> {
        return auditDao.getByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSecurityScore(): Flow<SecurityScore?> {
        return auditDao.getSecurityScore().map { entity ->
            entity?.let {
                SecurityScore(
                    totalItems = it.totalItems,
                    securedItems = it.securedItems,
                    scorePercent = it.scorePercent,
                    lastAuditDate = it.lastAuditDate,
                )
            }
        }
    }

    override suspend fun runAudit() {
        val allTrackedApps = Constants.TrackedApps.ALL
        val now = System.currentTimeMillis()

        val auditEntities = allTrackedApps.map { trackedApp ->
            val existing = auditDao.getByPackageName(trackedApp.packageName)
            val isInstalled = packageChecker.isInstalled(trackedApp.packageName)

            val status = when {
                !isInstalled -> AuditStatus.NOT_INSTALLED.name
                existing != null && existing.status == AuditStatus.SECURED.name -> AuditStatus.SECURED.name
                existing != null && existing.status == AuditStatus.SKIPPED.name -> AuditStatus.SKIPPED.name
                else -> AuditStatus.NEEDS_REVIEW.name
            }

            AuditItemEntity(
                id = existing?.id ?: 0,
                packageName = trackedApp.packageName,
                appName = trackedApp.appName,
                category = trackedApp.category,
                status = status,
                lastChecked = now,
            )
        }

        auditDao.insertAll(auditEntities)
        recalculateScore()
    }

    override suspend fun updateItemStatus(id: Int, status: AuditStatus) {
        auditDao.updateStatus(id, status.name)
        recalculateScore()
    }

    override suspend fun recalculateScore() {
        val secured = auditDao.getSecuredCount()
        val installed = auditDao.getInstalledCount()
        val percent = if (installed > 0) (secured * 100) / installed else 100

        auditDao.insertScore(
            SecurityScoreEntity(
                totalItems = installed,
                securedItems = secured,
                scorePercent = percent,
                lastAuditDate = System.currentTimeMillis(),
            )
        )
    }

    private fun AuditItemEntity.toDomain(): AuditItem {
        return AuditItem(
            id = id,
            packageName = packageName,
            appName = appName,
            category = try {
                AppCategory.valueOf(category)
            } catch (_: IllegalArgumentException) {
                AppCategory.BROWSERS
            },
            status = try {
                AuditStatus.valueOf(status)
            } catch (_: IllegalArgumentException) {
                AuditStatus.NEEDS_REVIEW
            },
            lastChecked = lastChecked,
        )
    }
}
