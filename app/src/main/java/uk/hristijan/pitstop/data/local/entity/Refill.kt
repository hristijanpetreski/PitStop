package uk.hristijan.pitstop.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "refills",
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
data class Refill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestamp: Long,
    val odometerKm: Long,
    val litres: Double,
    val totalCostMinor: Long,
    val isFullTank: Boolean = true,
    val favoritePlaceId: Long? = null,
    val stationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
)
