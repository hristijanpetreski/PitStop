package uk.hristijan.pitstop.data.repository

import kotlinx.coroutines.flow.Flow
import uk.hristijan.pitstop.core.model.DashboardSummary
import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.data.local.entity.FavoritePlace
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.data.local.entity.ServiceItem
import uk.hristijan.pitstop.data.local.entity.ServiceRecord
import uk.hristijan.pitstop.data.local.entity.Vehicle
import uk.hristijan.pitstop.data.local.model.ServiceWithItems

interface VehicleRepository {
    fun observeVehicles(): Flow<List<Vehicle>>
    fun observeVehicle(id: Long): Flow<Vehicle?>
    suspend fun getVehicle(id: Long): Vehicle?
    suspend fun save(vehicle: Vehicle): Long
    suspend fun delete(vehicle: Vehicle)
}

interface RefillRepository {
    fun observeRefills(vehicleId: Long): Flow<List<Refill>>
    suspend fun getRefill(id: Long): Refill?
    suspend fun save(refill: Refill): Long
    suspend fun delete(refill: Refill)
}

interface ServiceRepository {
    fun observeServices(vehicleId: Long): Flow<List<ServiceWithItems>>
    fun observeService(id: Long): Flow<ServiceWithItems?>
    suspend fun save(record: ServiceRecord, items: List<ServiceItem>): Long
    suspend fun delete(record: ServiceRecord)
}

interface FavoritePlaceRepository {
    fun observePlaces(): Flow<List<FavoritePlace>>
    fun observePlaces(type: FavoritePlaceType): Flow<List<FavoritePlace>>
    suspend fun getPlace(id: Long): FavoritePlace?
    suspend fun save(place: FavoritePlace): Long
    suspend fun delete(place: FavoritePlace)
}

interface DashboardRepository {
    fun observeDashboard(vehicleId: Long): Flow<DashboardSummary>
}
