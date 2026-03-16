/**
 * Repository interface for audit operations, following clean architecture principles.
 */
package com.safeanot.app.domain.repository

import com.safeanot.app.domain.model.AuditItem
import com.safeanot.app.domain.model.AuditStatus
import com.safeanot.app.domain.model.SecurityScore
import kotlinx.coroutines.flow.Flow

interface AuditRepository {

    /** Observe all audit items across all categories. */
    fun getAllAuditItems(): Flow<List<AuditItem>>

    /** Observe audit items filtered by category name. */
    fun getAuditItemsByCategory(category: String): Flow<List<AuditItem>>

    /** Observe the current security score. */
    fun getSecurityScore(): Flow<SecurityScore?>

    /** Run a full device audit, checking which tracked packages are installed. */
    suspend fun runAudit()

    /** Update the status of a specific audit item. */
    suspend fun updateItemStatus(id: Int, status: AuditStatus)

    /** Recalculate and persist the security score. */
    suspend fun recalculateScore()
}
