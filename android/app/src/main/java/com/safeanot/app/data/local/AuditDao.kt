/**
 * Room DAO for audit item and security score database operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.safeanot.app.data.local.entity.AuditItemEntity
import com.safeanot.app.data.local.entity.SecurityScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {

    @Query("SELECT * FROM audit_items ORDER BY category, app_name")
    fun getAll(): Flow<List<AuditItemEntity>>

    @Query("SELECT * FROM audit_items ORDER BY category, app_name")
    suspend fun getAllOnce(): List<AuditItemEntity>

    @Query("SELECT * FROM audit_items WHERE category = :category ORDER BY app_name")
    fun getByCategory(category: String): Flow<List<AuditItemEntity>>

    @Query("SELECT * FROM audit_items WHERE package_name = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): AuditItemEntity?

    @Query("UPDATE audit_items SET status = :status, last_checked = :timestamp WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE audit_items SET status = :status, last_checked = :timestamp WHERE package_name = :packageName")
    suspend fun updateStatusByPackageName(packageName: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM audit_items WHERE status = 'SECURED'")
    suspend fun getSecuredCount(): Int

    @Query("SELECT COUNT(*) FROM audit_items WHERE detection_state IN ('INSTALLED', 'NOT_QUERYABLE')")
    suspend fun getInstalledDetectedCount(): Int

    @Query("SELECT COUNT(*) FROM audit_items WHERE status != 'NOT_INSTALLED'")
    suspend fun getInstalledCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AuditItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: SecurityScoreEntity)

    @Query("SELECT * FROM security_scores WHERE id = 1")
    fun getSecurityScore(): Flow<SecurityScoreEntity?>

    @Query("DELETE FROM audit_items")
    suspend fun deleteAll()

    @Update
    suspend fun update(item: AuditItemEntity)
}
