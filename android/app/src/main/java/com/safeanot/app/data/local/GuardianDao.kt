/**
 * Room DAO for guardian pairing persistence.
 */
package com.safeanot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.safeanot.app.data.local.entity.GuardianPairingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GuardianDao {

    @Query("SELECT * FROM guardian_pairings ORDER BY created_at DESC")
    fun getAllPairings(): Flow<List<GuardianPairingEntity>>

    @Query("SELECT * FROM guardian_pairings WHERE role = 'WARD' ORDER BY created_at DESC")
    fun getWards(): Flow<List<GuardianPairingEntity>>

    @Query("SELECT * FROM guardian_pairings WHERE role = 'GUARDIAN' ORDER BY created_at DESC")
    fun getGuardians(): Flow<List<GuardianPairingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pairings: List<GuardianPairingEntity>)

    @Query("DELETE FROM guardian_pairings WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM guardian_pairings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM guardian_pairings")
    fun getGuardianCount(): Flow<Int>

    @Query("SELECT id FROM guardian_pairings WHERE paired_device_id = :pairedDeviceId LIMIT 1")
    suspend fun getPairingIdByPairedDeviceId(pairedDeviceId: String): String?
}
