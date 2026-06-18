package uk.hristijan.pitstop.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.hristijan.pitstop.data.local.entity.Vehicle

data class AppSessionState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long? = null,
    val loading: Boolean = true,
)

class AppSessionViewModel(private val container: AppContainer) : ViewModel() {
    val state = combine(
        container.vehicleRepository.observeVehicles(),
        container.selectedVehiclePreferences.selectedVehicleId,
    ) { vehicles, selected ->
        AppSessionState(
            vehicles = vehicles,
            selectedVehicleId = selected?.takeIf { id -> vehicles.any { it.id == id } } ?: vehicles.firstOrNull()?.id,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSessionState())

    init {
        viewModelScope.launch {
            combine(
                container.vehicleRepository.observeVehicles(),
                container.selectedVehiclePreferences.selectedVehicleId,
            ) { vehicles, selected -> vehicles to selected }
                .collect { (vehicles, selected) ->
                    val valid = selected?.takeIf { id -> vehicles.any { it.id == id } }
                    val fallback = vehicles.firstOrNull()?.id
                    if (valid != selected || (selected == null && fallback != null)) {
                        container.selectedVehiclePreferences.selectVehicle(valid ?: fallback)
                    }
                }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppSessionViewModel(container) as T
    }
}
