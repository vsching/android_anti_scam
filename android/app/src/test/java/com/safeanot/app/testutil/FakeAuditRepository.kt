package com.safeanot.app.testutil

import com.safeanot.app.domain.model.AuditChangeSummary
import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.SecurityScore
import com.safeanot.app.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuditRepository : AuditRepository {

    val auditItemsFlow = MutableStateFlow<List<AuditItem>>(emptyList())
    val securityScoreFlow = MutableStateFlow<SecurityScore?>(null)
    var auditRunCount = 0

    override fun getAllAuditItems(): Flow<List<AuditItem>> = auditItemsFlow

    override fun getAuditItemsByCategory(category: String): Flow<List<AuditItem>> {
        return MutableStateFlow(auditItemsFlow.value.filter { it.category.name == category })
    }

    override fun getSecurityScore(): Flow<SecurityScore?> = securityScoreFlow

    override suspend fun runAudit() {
        auditRunCount++
    }

    override suspend fun runAuditAndDetectChanges(): AuditChangeSummary {
        auditRunCount++
        return AuditChangeSummary()
    }

    override suspend fun updateItemStatus(id: Int, status: AuditStatus) {
        auditItemsFlow.value = auditItemsFlow.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    override suspend fun updateItemStatusByPackage(packageName: String, status: AuditStatus) {
        auditItemsFlow.value = auditItemsFlow.value.map {
            if (it.packageName == packageName) it.copy(status = status) else it
        }
    }

    override suspend fun recalculateScore() {}

    override fun getCompletedAuditCount(): Flow<Int> = MutableStateFlow(auditRunCount)

    override fun getLastAuditTimestamp(): Flow<Long?> = MutableStateFlow(null)
}
