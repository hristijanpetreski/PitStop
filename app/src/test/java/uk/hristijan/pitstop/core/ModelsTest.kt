package uk.hristijan.pitstop.core

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.hristijan.pitstop.core.model.DashboardSummary

class ModelsTest {
    @Test
    fun `dashboard total combines fuel and service cost`() {
        val summary = DashboardSummary(
            vehicleId = 1,
            totalFuelCostMinor = 12_500,
            totalServiceCostMinor = 20_000,
            totalFuelLitres = 70.0,
            refillCount = 2,
            serviceCount = 1,
            latestOdometerKm = 42_000,
            averageFuelEfficiency = 7.5,
        )

        assertEquals(32_500L, summary.totalCostMinor)
    }
}
