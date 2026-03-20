/**
 * Room DAO for quiz result operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.safeanot.app.data.local.entity.QuizResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Insert
    suspend fun insert(result: QuizResultEntity)

    @Query("SELECT * FROM quiz_results ORDER BY completed_at DESC")
    fun observeResults(): Flow<List<QuizResultEntity>>

    @Query("SELECT MAX(score_percent) FROM quiz_results")
    suspend fun getBestScore(): Int?
}
