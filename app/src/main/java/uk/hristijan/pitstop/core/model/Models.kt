package uk.hristijan.pitstop.core.model

enum class FuelType { PETROL, DIESEL, LPG, ELECTRIC, HYBRID, OTHER }

enum class ServiceCategory {
    ROUTINE_MAINTENANCE,
    REPAIR,
    INSPECTION,
    TYRES,
    BODYWORK,
    OTHER,
}

enum class ServicePerformer { SELF, GARAGE, MECHANIC }

enum class FavoritePlaceType { FUEL_STATION, SERVICE_CENTER, PARKING, OTHER }

enum class FuelEfficiencyUnit { LITRES_PER_100_KM, KILOMETRES_PER_LITRE }

data class Money(val minorUnits: Long, val currencyCode: String = "EUR") {
    init {
        require(currencyCode.matches(Regex("[A-Z]{3}"))) { "currencyCode must be a 3-letter ISO code" }
    }
}

data class DashboardSummary(
    val vehicleId: Long,
    val totalFuelCostMinor: Long,
    val totalServiceCostMinor: Long,
    val totalFuelLitres: Double,
    val refillCount: Int,
    val serviceCount: Int,
    val latestOdometerKm: Long?,
    val averageFuelEfficiency: Double?,
    val efficiencyUnit: FuelEfficiencyUnit = FuelEfficiencyUnit.LITRES_PER_100_KM,
) {
    val totalCostMinor: Long get() = totalFuelCostMinor + totalServiceCostMinor
}

data class VehicleCosts(
    val vehicleId: Long,
    val fuelCostMinor: Long,
    val serviceCostMinor: Long,
) {
    val totalMinor: Long get() = fuelCostMinor + serviceCostMinor
}
