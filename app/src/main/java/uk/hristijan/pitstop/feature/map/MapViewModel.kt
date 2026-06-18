package uk.hristijan.pitstop.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.data.local.entity.ServiceRecord
import uk.hristijan.pitstop.data.preferences.SelectedVehiclePreferences
import uk.hristijan.pitstop.data.repository.RefillRepository
import uk.hristijan.pitstop.data.repository.ServiceRepository

enum class MapFilter { ALL, REFILLS, SERVICES }

sealed interface MapEntry {
    val id: Long
    val latitude: Double
    val longitude: Double
    val timestamp: Long

    data class RefillEntry(val refill: Refill) : MapEntry {
        override val id = refill.id
        override val latitude = requireNotNull(refill.latitude)
        override val longitude = requireNotNull(refill.longitude)
        override val timestamp = refill.timestamp
    }

    data class ServiceEntry(val service: ServiceRecord) : MapEntry {
        override val id = service.id
        override val latitude = requireNotNull(service.latitude)
        override val longitude = requireNotNull(service.longitude)
        override val timestamp = service.timestamp
    }
}

data class MapUiState(
    val activeVehicleId: Long? = null,
    val filter: MapFilter = MapFilter.ALL,
    val entries: List<MapEntry> = emptyList(),
    val recordsWithoutCoordinates: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    preferences: SelectedVehiclePreferences,
    refillRepository: RefillRepository,
    serviceRepository: ServiceRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(MapFilter.ALL)
    private val vehicleId = preferences.selectedVehicleId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val refills = vehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else refillRepository.observeRefills(id)
    }
    private val services = vehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else serviceRepository.observeServices(id)
    }

    val uiState = combine(vehicleId, refills, services, filter) { id, refillList, serviceList, selectedFilter ->
        val validRefills = refillList.filter { validCoordinates(it.latitude, it.longitude) }
        val validServices = serviceList.map { it.service }.filter { validCoordinates(it.latitude, it.longitude) }
        val entries = buildList {
            if (selectedFilter != MapFilter.SERVICES) addAll(validRefills.map(MapEntry::RefillEntry))
            if (selectedFilter != MapFilter.REFILLS) addAll(validServices.map(MapEntry::ServiceEntry))
        }.sortedByDescending(MapEntry::timestamp)
        MapUiState(
            activeVehicleId = id,
            filter = selectedFilter,
            entries = entries,
            recordsWithoutCoordinates = refillList.size + serviceList.size - validRefills.size - validServices.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    fun setFilter(value: MapFilter) { filter.value = value }

    private fun validCoordinates(latitude: Double?, longitude: Double?): Boolean =
        latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
}

class MapViewModelFactory(
    private val preferences: SelectedVehiclePreferences,
    private val refillRepository: RefillRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MapViewModel(preferences, refillRepository, serviceRepository) as T
}
