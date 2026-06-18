package uk.hristijan.pitstop.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import uk.hristijan.pitstop.core.model.FuelType

@Entity(
    tableName = "vehicles",
    indices = [Index(value = ["registration"], unique = true)],
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val make: String,
    val model: String,
    val year: Int,
    val trim: String? = null,
    val vin: String? = null,
    val registration: String? = null,
    val nickname: String? = null,
    val fuelType: FuelType = FuelType.OTHER,
    val odometerKm: Long? = null,
    val photoPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
)
