package uk.hristijan.pitstop.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_items",
    foreignKeys = [
        ForeignKey(
            entity = ServiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["serviceRecordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["serviceRecordId"])],
)
data class ServiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceRecordId: Long,
    val description: String,
    val quantity: Int = 1,
    val unitCostMinor: Long,
)
