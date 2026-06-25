package uk.hristijan.pitstop.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.core.format.formatDistance
import uk.hristijan.pitstop.core.format.formatEfficiency
import uk.hristijan.pitstop.core.format.formatMoney
import uk.hristijan.pitstop.ui.components.EmptyState
import uk.hristijan.pitstop.ui.components.LoadingState
import androidx.compose.material.icons.outlined.Settings
import uk.hristijan.pitstop.app.LocalCurrency

@Composable
fun DashboardRoute(
    vehicleId: Long,
    onGarage: () -> Unit,
    onAddRefill: () -> Unit,
    onAddService: () -> Unit,
    onSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: DashboardViewModel = viewModel(
        key = "dashboard-$vehicleId",
        factory = DashboardViewModel.Factory(vehicleId, container.vehicleRepository, container.dashboardRepository),
    )
    val state by vm.state.collectAsState()
    DashboardScreen(state, onGarage, onAddRefill, onAddService, onSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onGarage: () -> Unit,
    onAddRefill: () -> Unit,
    onAddService: () -> Unit,
    onSettings: () -> Unit,
) {
    val currencyCode = LocalCurrency.current
    Scaffold(topBar = { TopAppBar(title = { Text("Dashboard") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }; IconButton(onClick = onGarage) { Icon(Icons.Outlined.Garage, contentDescription = "Garage") } }) }) { padding ->
        when {
            state.loading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.vehicle == null -> EmptyState("Vehicle unavailable", "Choose a vehicle from your garage.", Modifier.padding(padding), "Open garage", onGarage)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.vehicle.photoPath?.let { path ->
                                AsyncImage(model = path, contentDescription = "${state.vehicle.nickname ?: state.vehicle.model} vehicle photo", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
                            }
                            Text(state.vehicle.nickname ?: "${state.vehicle.make} ${state.vehicle.model}", style = MaterialTheme.typography.headlineMedium)
                            Text("${state.vehicle.year} ${state.vehicle.make} ${state.vehicle.model}${state.vehicle.trim?.let { " · $it" } ?: ""}")
                            Text("Odometer ${formatDistance(state.summary?.latestOdometerKm ?: state.vehicle.odometerKm)}")
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onAddRefill, modifier = Modifier.weight(1f)) { Text("Add refill") }
                        Button(onClick = onAddService, modifier = Modifier.weight(1f)) { Text("Add service") }
                    }
                }
                item {
                    val summary = state.summary
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("At a glance", style = MaterialTheme.typography.titleLarge)
                        StatCard("Fuel efficiency", formatEfficiency(summary?.averageFuelEfficiency))
                        StatCard("Fuel spend", formatMoney(summary?.totalFuelCostMinor ?: 0, currencyCode))
                        StatCard("Service spend", formatMoney(summary?.totalServiceCostMinor ?: 0, currencyCode))
                        StatCard("Total running cost", formatMoney(summary?.totalCostMinor ?: 0, currencyCode))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
