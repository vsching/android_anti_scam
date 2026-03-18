/**
 * Repository implementation that bridges Room database operations with the domain layer.
 * Uses PackageChecker to determine which tracked apps are installed on the device.
 */
package com.safeanot.app.data.repository

import com.safeanot.app.data.local.AuditDao
import com.safeanot.app.data.local.entity.AuditItemEntity
import com.safeanot.app.data.local.entity.SecurityScoreEntity
import com.safeanot.app.domain.model.AppCategory
import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import com.safeanot.app.util.Constants
import com.safeanot.app.util.DetectionState
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
            val detection = packageChecker.detect(trackedApp.packageName, trackedApp.appName)

            val detectionState = detection.state
            val appLabel = detection.appLabel ?: trackedApp.appName

            val status = when {
                detectionState == DetectionState.NOT_INSTALLED -> AuditStatus.NOT_INSTALLED.name
                detectionState == DetectionState.NOT_QUERYABLE -> AuditStatus.NEEDS_REVIEW.name
                existing != null && existing.status == AuditStatus.SECURED.name -> AuditStatus.SECURED.name
                existing != null && existing.status == AuditStatus.SKIPPED.name -> AuditStatus.SKIPPED.name
                else -> AuditStatus.NEEDS_REVIEW.name
            }

            AuditItemEntity(
                id = existing?.id ?: 0,
                packageName = trackedApp.packageName,
                appName = appLabel,
                category = trackedApp.category,
                status = status,
                riskDescription = trackedApp.riskDescription,
                detectionState = detectionState.name,
                lastChecked = now,
            )
        }

        auditDao.insertAll(auditEntities)

        // Increment audit count and set last full audit timestamp
        // Pass existing score to avoid a redundant getSecurityScoreOnce() inside recalculateScore
        val existingScore = auditDao.getSecurityScoreOnce()
        val newAuditCount = (existingScore?.auditCount ?: 0) + 1
        recalculateScore(
            auditCount = newAuditCount,
            lastFullAuditAt = now,
            existingScore = existingScore,
        )
    }

    override suspend fun runAuditAndDetectChanges(): AuditChangeSummary {
        val previousItems = auditDao.getAllOnce()
        val previousMap = previousItems.associateBy { it.packageName }

        runAudit()

        val currentItems = auditDao.getAllOnce()
        val newlyRisky = mutableListOf<String>()
        val regressions = mutableListOf<String>()

        for (current in currentItems) {
            val previous = previousMap[current.packageName]
            if (previous == null) {
                // New app detected as installed and needing review
                if (current.detectionState == DetectionState.INSTALLED.name &&
                    current.status == AuditStatus.NEEDS_REVIEW.name
                ) {
                    newlyRisky.add(current.appName)
                }
            } else {
                // Regression: was SECURED or NOT_INSTALLED, now NEEDS_REVIEW
                if (previous.status == AuditStatus.SECURED.name &&
                    current.status == AuditStatus.NEEDS_REVIEW.name
                ) {
                    regressions.add(current.appName)
                }
                if (previous.status == AuditStatus.NOT_INSTALLED.name &&
                    current.status == AuditStatus.NEEDS_REVIEW.name
                ) {
                    newlyRisky.add(current.appName)
                }
            }
        }

        return AuditChangeSummary(
            newlyRiskyApps = newlyRisky,
            regressions = regressions,
        )
    }

    override suspend fun updateItemStatus(id: Int, status: AuditStatus) {
        auditDao.updateStatus(id, status.name)
        recalculateScore()
    }

    override suspend fun updateItemStatusByPackage(packageName: String, status: AuditStatus) {
        auditDao.updateStatusByPackageName(packageName, status.name)
        recalculateScore()
    }

    override suspend fun recalculateScore() {
        recalculateScore(auditCount = null, lastFullAuditAt = null, existingScore = null)
    }

    /**
     * Internal recalculate that preserves existing audit stats.
     * When auditCount/lastFullAuditAt are null, existing values are preserved.
     * When existingScore is provided, avoids a redundant getSecurityScoreOnce() call.
     */
    private suspend fun recalculateScore(
        auditCount: Int?,
        lastFullAuditAt: Long?,
        existingScore: SecurityScoreEntity?,
    ) {
        val secured = auditDao.getSecuredCount()
        val installed = auditDao.getInstalledDetectedCount()
        val percent = if (installed > 0) (secured * 100) / installed else 100

        // Preserve existing audit stats on upsert (reuse passed value or fetch)
        val existing = existingScore ?: auditDao.getSecurityScoreOnce()

        auditDao.insertScore(
            SecurityScoreEntity(
                totalItems = installed,
                securedItems = secured,
                scorePercent = percent,
                lastAuditDate = System.currentTimeMillis(),
                auditCount = auditCount ?: existing?.auditCount ?: 0,
                lastFullAuditAt = lastFullAuditAt ?: existing?.lastFullAuditAt,
            )
        )
    }

    override fun getCompletedAuditCount(): Flow<Int> {
        return auditDao.getAuditCount().map { it ?: 0 }
    }

    override fun getLastAuditTimestamp(): Flow<Long?> {
        return auditDao.getLastFullAuditTimestamp()
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
            riskDescription = riskDescription,
            detectionState = try {
                DetectionState.valueOf(detectionState)
            } catch (_: IllegalArgumentException) {
                DetectionState.NOT_INSTALLED
            },
            lastChecked = lastChecked,
        )
    }
}
