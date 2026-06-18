package uk.hristijan.pitstop.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uk.hristijan.pitstop.core.model.DashboardSummary
import uk.hristijan.pitstop.data.local.entity.Vehicle
import uk.hristijan.pitstop.data.repository.DashboardRepository
import uk.hristijan.pitstop.data.repository.VehicleRepository

data class DashboardUiState(
    val vehicle: Vehicle? = null,
    val summary: DashboardSummary? = null,
    val loading: Boolean = true,
)

class DashboardViewModel(
    vehicleId: Long,
    vehicleRepository: VehicleRepository,
    dashboardRepository: DashboardRepository,
) : ViewModel() {
    val state = combine(
        vehicleRepository.observeVehicle(vehicleId),
        dashboardRepository.observeDashboard(vehicleId),
    ) { vehicle, summary -> DashboardUiState(vehicle, summary, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    class Factory(
        private val vehicleId: Long,
        private val vehicles: VehicleRepository,
        private val dashboard: DashboardRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(vehicleId, vehicles, dashboard) as T
    }
}
