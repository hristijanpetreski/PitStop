package uk.hristijan.pitstop.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uk.hristijan.pitstop.core.calculation.FuelEfficiencyCalculator
import uk.hristijan.pitstop.core.model.DashboardSummary
import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.data.local.dao.FavoritePlaceDao
import uk.hristijan.pitstop.data.local.dao.RefillDao
import uk.hristijan.pitstop.data.local.dao.ServiceRecordDao
import uk.hristijan.pitstop.data.local.dao.VehicleDao
import uk.hristijan.pitstop.data.local.entity.FavoritePlace
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.data.local.entity.ServiceItem
import uk.hristijan.pitstop.data.local.entity.ServiceRecord
import uk.hristijan.pitstop.data.local.entity.Vehicle
import uk.hristijan.pitstop.data.local.model.ServiceWithItems

class LocalVehicleRepository(private val dao: VehicleDao) : VehicleRepository {
    override fun observeVehicles(): Flow<List<Vehicle>> = dao.observeAll()
    override fun observeVehicle(id: Long): Flow<Vehicle?> = dao.observeById(id)
    override suspend fun getVehicle(id: Long): Vehicle? = dao.getById(id)
    override suspend fun save(vehicle: Vehicle): Long = if (vehicle.id == 0L) {
        dao.insert(vehicle)
    } else {
        dao.update(vehicle)
        vehicle.id
    }
    override suspend fun delete(vehicle: Vehicle) = dao.delete(vehicle)
}

class LocalRefillRepository(private val dao: RefillDao) : RefillRepository {
    override fun observeRefills(vehicleId: Long): Flow<List<Refill>> = dao.observeForVehicle(vehicleId)
    override suspend fun getRefill(id: Long): Refill? = dao.getById(id)
    override suspend fun save(refill: Refill): Long = if (refill.id == 0L) {
        dao.insert(refill)
    } else {
        dao.update(refill)
        refill.id
    }
    override suspend fun delete(refill: Refill) = dao.delete(refill)
}

class LocalServiceRepository(private val dao: ServiceRecordDao) : ServiceRepository {
    override fun observeServices(vehicleId: Long): Flow<List<ServiceWithItems>> = dao.observeForVehicle(vehicleId)
    override fun observeService(id: Long): Flow<ServiceWithItems?> = dao.observeById(id)
    override suspend fun save(record: ServiceRecord, items: List<ServiceItem>): Long = dao.saveWithItems(record, items)
    override suspend fun delete(record: ServiceRecord) = dao.delete(record)
}

class LocalFavoritePlaceRepository(private val dao: FavoritePlaceDao) : FavoritePlaceRepository {
    override fun observePlaces(): Flow<List<FavoritePlace>> = dao.observeAll()
    override fun observePlaces(type: FavoritePlaceType): Flow<List<FavoritePlace>> = dao.observeByType(type)
    override suspend fun getPlace(id: Long): FavoritePlace? = dao.getById(id)
    override suspend fun save(place: FavoritePlace): Long = if (place.id == 0L) {
        dao.insert(place)
    } else {
        dao.update(place)
        place.id
    }
    override suspend fun delete(place: FavoritePlace) = dao.delete(place)
}

class LocalDashboardRepository(
    private val refillDao: RefillDao,
    private val serviceDao: ServiceRecordDao,
) : DashboardRepository {
    override fun observeDashboard(vehicleId: Long): Flow<DashboardSummary> = combine(
        refillDao.observeAggregate(vehicleId),
        serviceDao.observeAggregate(vehicleId),
        refillDao.observeChronologicalForVehicle(vehicleId),
    ) { refills, services, refillHistory ->
        val (distanceKm, fuelLitres) = completedTankIntervals(refillHistory)
        DashboardSummary(
            vehicleId = vehicleId,
            totalFuelCostMinor = refills.totalFuelCostMinor,
            totalServiceCostMinor = services.totalServiceCostMinor,
            totalFuelLitres = refills.totalFuelLitres,
            refillCount = refills.refillCount,
            serviceCount = services.serviceCount,
            latestOdometerKm = listOfNotNull(refills.latestOdometerKm, services.latestServiceOdometerKm).maxOrNull(),
            averageFuelEfficiency = FuelEfficiencyCalculator.weightedAverageLitresPer100Km(distanceKm, fuelLitres),
        )
    }

    private fun completedTankIntervals(refills: List<Refill>): Pair<Double, Double> {
        var previousFullOdometer: Long? = null
        var accumulatedLitres = 0.0
        var totalDistance = 0.0
        var totalLitres = 0.0

        refills.forEach { refill ->
            if (previousFullOdometer != null) accumulatedLitres += refill.litres
            if (refill.isFullTank) {
                previousFullOdometer?.let { previous ->
                    val distance = refill.odometerKm - previous
                    if (distance > 0 && accumulatedLitres > 0.0) {
                        totalDistance += distance
                        totalLitres += accumulatedLitres
                    }
                }
                previousFullOdometer = refill.odometerKm
                accumulatedLitres = 0.0
            }
        }
        return totalDistance to totalLitres
    }
}
