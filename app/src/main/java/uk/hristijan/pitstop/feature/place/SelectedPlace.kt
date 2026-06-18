package uk.hristijan.pitstop.feature.place

import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.data.local.entity.FavoritePlace

/** A location selected from favorites, Places, the device, or a map pin. */
data class SelectedPlace(
    val name: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val favoritePlaceId: Long? = null,
    val type: FavoritePlaceType = FavoritePlaceType.OTHER,
)

fun FavoritePlace.toSelectedPlace(): SelectedPlace? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return SelectedPlace(
        name = name,
        address = address,
        latitude = lat,
        longitude = lng,
        favoritePlaceId = id,
        type = type,
    )
}
