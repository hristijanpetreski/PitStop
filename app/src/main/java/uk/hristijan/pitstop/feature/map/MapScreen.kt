package uk.hristijan.pitstop.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.ui.components.EmptyState
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun MapScreen(
    onRefillClick: (Long) -> Unit,
    onServiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            container.selectedVehiclePreferences,
            container.refillRepository,
            container.serviceRepository,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    MapContent(state, viewModel::setFilter, onRefillClick, onServiceClick, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapContent(
    state: MapUiState,
    onFilterChange: (MapFilter) -> Unit,
    onRefillClick: (Long) -> Unit,
    onServiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(state.entries) { mutableStateOf<MapEntry?>(null) }
    val cameraState = rememberCameraPositionState()
    LaunchedEffect(state.entries.firstOrNull()?.id) {
        state.entries.firstOrNull()?.let {
            cameraState.move(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 11f))
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Map") }) },
    ) { innerPadding ->
      Column(Modifier.fillMaxSize().padding(innerPadding)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MapFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            if (state.recordsWithoutCoordinates > 0) {
                Text(
                    "${state.recordsWithoutCoordinates} record(s) have no location",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        when {
            state.activeVehicleId == null -> EmptyState(
                title = "No active vehicle",
                message = "Select a vehicle to map its records.",
                modifier = Modifier.fillMaxSize(),
            )
            state.entries.isEmpty() -> EmptyState(
                title = "Nothing to map",
                message = if (state.recordsWithoutCoordinates > 0) "Add locations to your records to see them here." else "Records with locations will appear here.",
                modifier = Modifier.fillMaxSize(),
            )
            else -> Box(Modifier.fillMaxSize()) {
                GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraState) {
                    state.entries.forEach { entry ->
                        val isRefill = entry is MapEntry.RefillEntry
                        Marker(
                            state = MarkerState(LatLng(entry.latitude, entry.longitude)),
                            title = when (entry) {
                                is MapEntry.RefillEntry -> entry.refill.stationName ?: "Fuel refill"
                                is MapEntry.ServiceEntry -> entry.service.title
                            },
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (isRefill) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ORANGE,
                            ),
                            onClick = { selected = entry; true },
                        )
                    }
                }
                selected?.let { entry ->
                    MapSelectionCard(
                        entry = entry,
                        onDismiss = { selected = null },
                        onOpen = {
                            when (entry) {
                                is MapEntry.RefillEntry -> onRefillClick(entry.id)
                                is MapEntry.ServiceEntry -> onServiceClick(entry.id)
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    )
                }
            }
        }
      }
    }
}

@Composable
private fun MapSelectionCard(entry: MapEntry, onDismiss: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestamp))
    val currency = NumberFormat.getCurrencyInstance()
    val (title, subtitle, cost) = when (entry) {
        is MapEntry.RefillEntry -> Triple(
            entry.refill.stationName ?: "Fuel refill",
            "$date · ${entry.refill.litres} L",
            entry.refill.totalCostMinor,
        )
        is MapEntry.ServiceEntry -> Triple(entry.service.title, date, entry.service.totalCostMinor)
    }
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$subtitle · ${currency.format(cost / 100.0)}")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close") }
                TextButton(onClick = onOpen) { Text("View details") }
            }
        }
    }
}
