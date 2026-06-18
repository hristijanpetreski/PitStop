package uk.hristijan.pitstop.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.hristijan.pitstop.core.calculation.FuelEfficiencyCalculator
import uk.hristijan.pitstop.core.calculation.MoneyCalculator
import uk.hristijan.pitstop.core.calculation.ServiceCalculator
import uk.hristijan.pitstop.core.model.FuelEfficiencyUnit

class CalculationsTest {
    @Test
    fun `money calculations use minor units and round half up`() {
        assertEquals(167L, MoneyCalculator.unitPriceMinor(totalCostMinor = 1_000, quantity = 6.0))
        assertEquals(1_500L, MoneyCalculator.lineTotalMinor(unitPriceMinor = 500, quantity = 3))
        assertEquals(2_000L, MoneyCalculator.sum(listOf(500L, 1_500L)))
    }

    @Test
    fun `service total falls back to line items`() {
        assertEquals(3_500L, ServiceCalculator.reconcileTotalMinor(null, listOf(1_000L, 2_500L)))
        assertEquals(4_000L, ServiceCalculator.reconcileTotalMinor(4_000L, listOf(1_000L, 2_500L)))
    }

    @Test
    fun `fuel efficiency supports both units`() {
        assertEquals(
            8.0,
            FuelEfficiencyCalculator.calculate(500.0, 40.0, FuelEfficiencyUnit.LITRES_PER_100_KM)!!,
            0.0001,
        )
        assertEquals(
            12.5,
            FuelEfficiencyCalculator.calculate(500.0, 40.0, FuelEfficiencyUnit.KILOMETRES_PER_LITRE)!!,
            0.0001,
        )
    }

    @Test
    fun `fuel efficiency rejects non-positive intervals`() {
        assertNull(FuelEfficiencyCalculator.betweenRefills(1_000, 1_000, 20.0))
        assertNull(FuelEfficiencyCalculator.calculate(100.0, 0.0, FuelEfficiencyUnit.LITRES_PER_100_KM))
    }
}
