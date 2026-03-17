/**
 * Room DAO for scam alerts cache operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.safeanot.app.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertsDao {

    @Query("SELECT * FROM alerts ORDER BY created_at DESC")
    fun observeAlertsAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE region = :region OR region = 'both' ORDER BY created_at DESC")
    fun observeAlertsByRegion(region: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :id LIMIT 1")
    suspend fun getAlertById(id: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()

    @Query("DELETE FROM alerts WHERE region = :region OR region = 'both'")
    suspend fun clearByRegion(region: String)

    @Transaction
    suspend fun replaceAll(alerts: List<AlertEntity>) {
        clearAll()
        insertAll(alerts)
    }

    @Transaction
    suspend fun replaceByRegion(region: String, alerts: List<AlertEntity>) {
        clearByRegion(region)
        insertAll(alerts)
    }
}
