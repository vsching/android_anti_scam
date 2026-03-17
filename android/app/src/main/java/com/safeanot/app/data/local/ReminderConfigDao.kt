/**
 * Room DAO for reminder configuration read/write operations.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.safeanot.app.data.local.entity.ReminderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderConfigDao {

    @Query("SELECT * FROM reminder_config WHERE id = 1")
    fun getConfig(): Flow<ReminderConfigEntity?>

    @Query("SELECT * FROM reminder_config WHERE id = 1")
    suspend fun getConfigOnce(): ReminderConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: ReminderConfigEntity)

    @Query("UPDATE reminder_config SET last_reminder_date = :timestamp WHERE id = 1")
    suspend fun updateLastReminderDate(timestamp: Long)
}
