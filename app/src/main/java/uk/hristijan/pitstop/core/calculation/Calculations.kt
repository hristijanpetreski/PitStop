package uk.hristijan.pitstop.core.calculation

import java.math.BigDecimal
import java.math.RoundingMode
import uk.hristijan.pitstop.core.model.FuelEfficiencyUnit

object MoneyCalculator {
    fun sum(values: Iterable<Long>): Long = values.fold(0L) { total, value -> Math.addExact(total, value) }

    fun unitPriceMinor(totalCostMinor: Long, quantity: Double): Long? {
        if (totalCostMinor < 0 || !quantity.isFinite() || quantity <= 0.0) return null
        return BigDecimal.valueOf(totalCostMinor)
            .divide(BigDecimal.valueOf(quantity), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun lineTotalMinor(unitPriceMinor: Long, quantity: Int): Long {
        require(unitPriceMinor >= 0) { "Unit price cannot be negative" }
        require(quantity > 0) { "Quantity must be positive" }
        return Math.multiplyExact(unitPriceMinor, quantity.toLong())
    }
}

object ServiceCalculator {
    fun itemsTotalMinor(itemCostsMinor: Iterable<Long>): Long = MoneyCalculator.sum(itemCostsMinor)

    fun reconcileTotalMinor(recordedTotalMinor: Long?, itemCostsMinor: Iterable<Long>): Long =
        recordedTotalMinor ?: itemsTotalMinor(itemCostsMinor)
}

object FuelEfficiencyCalculator {
    fun calculate(distanceKm: Double, fuelLitres: Double, unit: FuelEfficiencyUnit): Double? {
        if (!distanceKm.isFinite() || distanceKm <= 0.0 || !fuelLitres.isFinite() || fuelLitres <= 0.0) return null
        return when (unit) {
            FuelEfficiencyUnit.LITRES_PER_100_KM -> fuelLitres * 100.0 / distanceKm
            FuelEfficiencyUnit.KILOMETRES_PER_LITRE -> distanceKm / fuelLitres
        }
    }

    fun betweenRefills(
        previousOdometerKm: Long,
        currentOdometerKm: Long,
        litresAtCurrentRefill: Double,
        unit: FuelEfficiencyUnit = FuelEfficiencyUnit.LITRES_PER_100_KM,
    ): Double? = calculate((currentOdometerKm - previousOdometerKm).toDouble(), litresAtCurrentRefill, unit)

    fun weightedAverageLitresPer100Km(totalDistanceKm: Double, totalFuelLitres: Double): Double? =
        calculate(totalDistanceKm, totalFuelLitres, FuelEfficiencyUnit.LITRES_PER_100_KM)
}
