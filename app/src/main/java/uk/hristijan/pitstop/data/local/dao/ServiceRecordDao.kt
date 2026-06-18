package uk.hristijan.pitstop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import uk.hristijan.pitstop.data.local.entity.ServiceItem
import uk.hristijan.pitstop.data.local.entity.ServiceRecord
import uk.hristijan.pitstop.data.local.model.ServiceAggregate
import uk.hristijan.pitstop.data.local.model.ServiceWithItems

@Dao
abstract class ServiceRecordDao {
    @Transaction
    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY timestamp DESC, id DESC")
    abstract fun observeForVehicle(vehicleId: Long): Flow<List<ServiceWithItems>>

    @Transaction
    @Query("SELECT * FROM service_records WHERE id = :id")
    abstract fun observeById(id: Long): Flow<ServiceWithItems?>

    @Query(
        """SELECT COALESCE(SUM(totalCostMinor), 0) AS totalServiceCostMinor,
            COUNT(*) AS serviceCount,
            MAX(odometerKm) AS latestServiceOdometerKm
            FROM service_records WHERE vehicleId = :vehicleId""",
    )
    abstract fun observeAggregate(vehicleId: Long): Flow<ServiceAggregate>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertRecord(record: ServiceRecord): Long

    @Update
    protected abstract suspend fun updateRecord(record: ServiceRecord)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertItems(items: List<ServiceItem>)

    @Query("DELETE FROM service_items WHERE serviceRecordId = :serviceRecordId")
    protected abstract suspend fun deleteItems(serviceRecordId: Long)

    @Delete
    abstract suspend fun delete(record: ServiceRecord)

    @Transaction
    open suspend fun saveWithItems(record: ServiceRecord, items: List<ServiceItem>): Long {
        val recordId = if (record.id == 0L) {
            insertRecord(record)
        } else {
            updateRecord(record)
            deleteItems(record.id)
            record.id
        }
        if (items.isNotEmpty()) {
            insertItems(items.map { it.copy(id = 0, serviceRecordId = recordId) })
        }
        return recordId
    }
}
