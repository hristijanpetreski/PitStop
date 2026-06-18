package uk.hristijan.pitstop.feature.history

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
import uk.hristijan.pitstop.data.local.model.ServiceWithItems
import uk.hristijan.pitstop.data.preferences.SelectedVehiclePreferences
import uk.hristijan.pitstop.data.repository.RefillRepository
import uk.hristijan.pitstop.data.repository.ServiceRepository

enum class HistoryType { ALL, REFILLS, SERVICES }

sealed interface HistoryEntry {
    val id: Long
    val timestamp: Long

    data class RefillEntry(val refill: Refill) : HistoryEntry {
        override val id = refill.id
        override val timestamp = refill.timestamp
    }

    data class ServiceEntry(val serviceWithItems: ServiceWithItems) : HistoryEntry {
        val service get() = serviceWithItems.service
        override val id = service.id
        override val timestamp = service.timestamp
    }
}

data class HistoryUiState(
    val activeVehicleId: Long? = null,
    val query: String = "",
    val type: HistoryType = HistoryType.ALL,
    val entries: List<HistoryEntry> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    preferences: SelectedVehiclePreferences,
    refillRepository: RefillRepository,
    serviceRepository: ServiceRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val type = MutableStateFlow(HistoryType.ALL)
    private val vehicleId = preferences.selectedVehicleId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val refills = vehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else refillRepository.observeRefills(id)
    }
    private val services = vehicleId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else serviceRepository.observeServices(id)
    }

    val uiState = combine(vehicleId, refills, services, query, type) { id, refillList, serviceList, text, selectedType ->
        val needle = text.trim().lowercase()
        val entries = buildList {
            if (selectedType != HistoryType.SERVICES) {
                addAll(refillList.filter { refill ->
                    needle.isBlank() || listOfNotNull(refill.stationName, refill.notes, refill.odometerKm.toString())
                        .any { needle in it.lowercase() }
                }.map(HistoryEntry::RefillEntry))
            }
            if (selectedType != HistoryType.REFILLS) {
                addAll(serviceList.filter { row ->
                    val service = row.service
                    needle.isBlank() || listOfNotNull(service.title, service.providerName, service.notes)
                        .plus(row.items.map { it.description })
                        .any { needle in it.lowercase() }
                }.map(HistoryEntry::ServiceEntry))
            }
        }.sortedByDescending(HistoryEntry::timestamp)
        HistoryUiState(id, text, selectedType, entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setQuery(value: String) { query.value = value }
    fun setType(value: HistoryType) { type.value = value }
}

class HistoryViewModelFactory(
    private val preferences: SelectedVehiclePreferences,
    private val refillRepository: RefillRepository,
    private val serviceRepository: ServiceRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HistoryViewModel(preferences, refillRepository, serviceRepository) as T
}
