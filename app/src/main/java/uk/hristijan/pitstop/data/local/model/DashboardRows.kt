package uk.hristijan.pitstop.data.local.model

data class RefillAggregate(
    val totalFuelCostMinor: Long,
    val totalFuelLitres: Double,
    val refillCount: Int,
    val latestOdometerKm: Long?,
)

data class ServiceAggregate(
    val totalServiceCostMinor: Long,
    val serviceCount: Int,
    val latestServiceOdometerKm: Long?,
)
