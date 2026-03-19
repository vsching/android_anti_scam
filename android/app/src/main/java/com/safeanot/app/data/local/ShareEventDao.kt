/**
 * Room DAO for share event analytics persistence.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.safeanot.app.data.local.entity.ShareEventEntity

@Dao
interface ShareEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ShareEventEntity)

    @Query("SELECT * FROM share_events WHERE synced = 0 ORDER BY timestamp ASC LIMIT 50")
    suspend fun getUnsynced(): List<ShareEventEntity>

    @Query("UPDATE share_events SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM share_events WHERE timestamp >= :sinceTimestamp")
    suspend fun countSince(sinceTimestamp: Long): Int

    @Query("DELETE FROM share_events WHERE synced = 1 AND timestamp < :beforeTimestamp")
    suspend fun deleteOldSynced(beforeTimestamp: Long)
}
