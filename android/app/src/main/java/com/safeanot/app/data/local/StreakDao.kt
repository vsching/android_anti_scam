/**
 * Room DAO for streak data operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.safeanot.app.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Query("SELECT * FROM streaks WHERE id = 1")
    fun observeStreak(): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks WHERE id = 1")
    suspend fun getStreakOnce(): StreakEntity?

    @Upsert
    suspend fun upsert(entity: StreakEntity)
}
