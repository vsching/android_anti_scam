/**
 * Room DAO for badge data operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.safeanot.app.data.local.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Query("SELECT * FROM badges")
    fun observeAll(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges WHERE badge_id = :badgeId")
    suspend fun getById(badgeId: String): BadgeEntity?

    @Upsert
    suspend fun upsert(entity: BadgeEntity)

    @Query("SELECT COUNT(*) FROM badges WHERE unlocked = 1")
    fun observeUnlockedCount(): Flow<Int>
}
