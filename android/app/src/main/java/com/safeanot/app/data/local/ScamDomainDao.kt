/**
 * Room DAO for scam domain lookup, sync metadata, and check result cache operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.safeanot.app.data.local.entity.CheckResultCacheEntity
import com.safeanot.app.data.local.entity.ScamDomainEntity
import com.safeanot.app.data.local.entity.SyncMetadataEntity

@Dao
interface ScamDomainDao {

    @Query("SELECT * FROM scam_domains WHERE domain = :domain LIMIT 1")
    suspend fun findByDomain(domain: String): ScamDomainEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(domains: List<ScamDomainEntity>)

    @Query("DELETE FROM scam_domains")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsertMetadata(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE id = 1")
    suspend fun getMetadata(): SyncMetadataEntity?

    @Query("SELECT * FROM check_result_cache WHERE domain = :domain AND expires_at > :now LIMIT 1")
    suspend fun getValidCachedResult(domain: String, now: Long): CheckResultCacheEntity?

    @Upsert
    suspend fun upsertCachedResult(entity: CheckResultCacheEntity)

    @Query("DELETE FROM check_result_cache WHERE expires_at <= :now")
    suspend fun deleteExpiredCache(now: Long)
}
