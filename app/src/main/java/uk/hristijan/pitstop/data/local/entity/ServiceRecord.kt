package uk.hristijan.pitstop.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import uk.hristijan.pitstop.core.model.ServiceCategory
import uk.hristijan.pitstop.core.model.ServicePerformer

@Entity(
    tableName = "service_records",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FavoritePlace::class,
            parentColumns = ["id"],
            childColumns = ["favoritePlaceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["favoritePlaceId"]),
        Index(value = ["vehicleId", "timestamp"]),
    ],
)
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long,
    val title: String,
    val category: ServiceCategory = ServiceCategory.OTHER,
    val performedBy: ServicePerformer = ServicePerformer.SELF,
    val odometerKm: Long? = null,
    val laborCostMinor: Long = 0,
    val totalCostMinor: Long = 0,
    val favoritePlaceId: Long? = null,
    val providerName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val nextServiceAt: Long? = null,
    val nextServiceOdometerKm: Long? = null,
)
