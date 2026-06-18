package uk.hristijan.pitstop.feature.refill

/** A place value supplied by the favorites/map feature without coupling refill UI to it. */
data class RefillPlaceValue(
    val favoritePlaceId: Long? = null,
    val stationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

internal fun String.trimmedOrNull(): String? = trim().takeIf(String::isNotEmpty)

