package uk.hristijan.pitstop.feature.place

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import uk.hristijan.pitstop.app.LocalAppContainer

private enum class PickerMode { FAVORITES, SEARCH, PIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacePickerScreen(
    onPlaceSelected: (SelectedPlace) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val viewModel: PlacePickerViewModel = viewModel(
        factory = PlacePickerViewModelFactory(context, container.favoritePlaceRepository),
    )
    val favorites by viewModel.favorites.collectAsState()
    val search by viewModel.searchState.collectAsState()
    var mode by remember { mutableStateOf(PickerMode.FAVORITES) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    val locationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    fun useCurrentLocation() {
        try {
            locationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location == null) locationMessage = "Current location is unavailable. Try dropping a pin."
                    else onPlaceSelected(
                        SelectedPlace(
                            name = "Current location",
                            latitude = location.latitude,
                            longitude = location.longitude,
                        ),
                    )
                }
                .addOnFailureListener { locationMessage = it.localizedMessage ?: "Could not get current location" }
        } catch (_: SecurityException) {
            locationMessage = "Location permission is required"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) useCurrentLocation()
        else locationMessage = "Location permission was not granted. Favorites, search, and map pins still work."
    }
    val requestCurrentLocation = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) useCurrentLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Choose a place") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                "Saved, searched, or pinned",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Button(
                onClick = requestCurrentLocation,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            ) { Text("Use current location") }
            locationMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PickerMode.entries.forEach { candidate ->
                    FilterChip(
                        selected = mode == candidate,
                        onClick = { mode = candidate },
                        label = { Text(if (candidate == PickerMode.PIN) "Map pin" else candidate.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    )
                }
            }
            when (mode) {
                PickerMode.FAVORITES -> FavoriteChoices(favorites.mapNotNull { it.toSelectedPlace() }, onPlaceSelected)
                PickerMode.SEARCH -> PlaceSearch(search, viewModel::setQuery, viewModel::search) {
                    viewModel.selectPrediction(it, onPlaceSelected)
                }
                PickerMode.PIN -> ManualPinPicker(onPlaceSelected)
            }
        }
    }
}

@Composable
private fun FavoriteChoices(places: List<SelectedPlace>, onSelected: (SelectedPlace) -> Unit) {
    if (places.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorites with coordinates yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(places, key = { it.favoritePlaceId ?: "${it.latitude},${it.longitude}" }) { place ->
                Card(Modifier.fillMaxWidth().clickable { onSelected(place) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        place.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceSearch(
    state: PlaceSearchState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPredictionSelected: (PlacePrediction) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Place or address") },
                singleLine = true,
                enabled = state.placesAvailable,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSearch, enabled = state.placesAvailable && !state.isSearching) { Text("Search") }
        }
        if (!state.placesAvailable) {
            Text("Places search is not configured. Favorites, current location, and map pins are still available.")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        if (state.isSearching) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.predictions, key = PlacePrediction::placeId) { prediction ->
                Card(Modifier.fillMaxWidth().clickable { onPredictionSelected(prediction) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(prediction.primaryText, fontWeight = FontWeight.SemiBold)
                        Text(prediction.secondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (state.placesAvailable) {
            Text("Powered by Google", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End).padding(8.dp))
        }
    }
}

@Composable
private fun ManualPinPicker(onSelected: (SelectedPlace) -> Unit) {
    var pin by remember { mutableStateOf<LatLng?>(null) }
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.9981, 21.4254), 10f)
    }
    val context = LocalContext.current
    LaunchedEffect(pin) {
        pin?.let {
            MapsInitializer.initialize(context)
            cameraState.animate(CameraUpdateFactory.newLatLng(it))
        }
    }
    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            onMapClick = { pin = it },
        ) {
            pin?.let { Marker(state = MarkerState(it), title = "Selected location") }
        }
        Card(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (pin == null) "Tap the map to drop a pin" else "Pin ready", fontWeight = FontWeight.SemiBold)
                Button(
                    enabled = pin != null,
                    onClick = {
                        pin?.let {
                            onSelected(SelectedPlace("Pinned location", latitude = it.latitude, longitude = it.longitude))
                        }
                    },
                ) { Text("Use this location") }
            }
        }
    }
}
