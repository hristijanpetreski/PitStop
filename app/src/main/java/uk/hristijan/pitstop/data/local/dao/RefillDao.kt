package uk.hristijan.pitstop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.data.local.model.RefillAggregate

@Dao
interface RefillDao {
    @Query("SELECT * FROM refills WHERE vehicleId = :vehicleId ORDER BY timestamp DESC, id DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<Refill>>

    @Query("SELECT * FROM refills WHERE vehicleId = :vehicleId ORDER BY timestamp ASC, id ASC")
    fun observeChronologicalForVehicle(vehicleId: Long): Flow<List<Refill>>

    @Query("SELECT * FROM refills WHERE id = :id")
    suspend fun getById(id: Long): Refill?

    @Query(
        """SELECT COALESCE(SUM(totalCostMinor), 0) AS totalFuelCostMinor,
            COALESCE(SUM(litres), 0.0) AS totalFuelLitres,
            COUNT(*) AS refillCount,
            MAX(odometerKm) AS latestOdometerKm
            FROM refills WHERE vehicleId = :vehicleId""",
    )
    fun observeAggregate(vehicleId: Long): Flow<RefillAggregate>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(refill: Refill): Long

    @Update
    suspend fun update(refill: Refill)

    @Delete
    suspend fun delete(refill: Refill)
}
