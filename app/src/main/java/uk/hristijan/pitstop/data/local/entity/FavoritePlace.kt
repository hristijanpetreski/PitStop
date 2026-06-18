package uk.hristijan.pitstop.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import uk.hristijan.pitstop.core.model.FavoritePlaceType

@Entity(
    tableName = "favorite_places",
    indices = [Index(value = ["name"]), Index(value = ["latitude", "longitude"])],
)
data class FavoritePlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: FavoritePlaceType = FavoritePlaceType.OTHER,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val notes: String? = null,
    val createdAt: Long,
)
