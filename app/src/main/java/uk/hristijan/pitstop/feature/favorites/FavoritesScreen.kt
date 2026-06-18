package uk.hristijan.pitstop.feature.favorites

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.data.local.entity.FavoritePlace
import uk.hristijan.pitstop.ui.components.ConfirmDeleteDialog
import uk.hristijan.pitstop.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onUsePlace: (FavoritePlace) -> Unit,
    onNavigateToPlace: (FavoritePlace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModelFactory(container.favoritePlaceRepository))
    val places by viewModel.places.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<FavoritePlace?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<FavoritePlace?>(null) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Favorites") })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Add favorite place")
            }
        },
    ) { innerPadding ->
      Column(Modifier.fillMaxSize().padding(innerPadding)) {
        if (places.isEmpty()) {
            EmptyState(
                title = "No favorite places",
                message = "Save stations, garages, and parking spots for quick reuse.",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(places, key = FavoritePlace::id) { place ->
                    FavoriteRow(
                        place = place,
                        onUse = { onUsePlace(place) },
                        onNavigate = { onNavigateToPlace(place) },
                        onEdit = { editing = place },
                        onDelete = { deleting = place },
                    )
                }
            }
        }
      }
    }

    if (adding || editing != null) {
        FavoriteEditor(
            place = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { id, name, type, address, lat, lng, notes ->
                viewModel.save(id, name, type, address, lat, lng, notes)
                adding = false
                editing = null
            },
        )
    }
    deleting?.let { place ->
        ConfirmDeleteDialog(
            title = "Delete ${place.name}?",
            message = "This removes the place from favorites. Existing records stay intact.",
            onConfirm = { viewModel.delete(place); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun FavoriteRow(
    place: FavoritePlace,
    onUse: () -> Unit,
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(place.type.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium)
            place.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onUse, enabled = place.latitude != null && place.longitude != null) { Text("Use") }
                TextButton(onClick = onNavigate, enabled = place.latitude != null && place.longitude != null) { Text("Map") }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun FavoriteEditor(
    place: FavoritePlace?,
    onDismiss: () -> Unit,
    onSave: (Long, String, FavoritePlaceType, String?, Double?, Double?, String?) -> Unit,
) {
    val context = LocalContext.current
    val locationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }
    var name by remember(place) { mutableStateOf(place?.name.orEmpty()) }
    var type by remember(place) { mutableStateOf(place?.type ?: FavoritePlaceType.OTHER) }
    var address by remember(place) { mutableStateOf(place?.address.orEmpty()) }
    var latitude by remember(place) { mutableStateOf(place?.latitude?.toString().orEmpty()) }
    var longitude by remember(place) { mutableStateOf(place?.longitude?.toString().orEmpty()) }
    var notes by remember(place) { mutableStateOf(place?.notes.orEmpty()) }
    var locationMode by remember(place) { mutableStateOf(if (place == null) LocationInputMode.CURRENT else LocationInputMode.MANUAL) }
    var locationStatus by remember(place) { mutableStateOf<String?>(null) }

    fun applyCurrentLocation(lat: Double, lng: Double) {
        latitude = lat.toString()
        longitude = lng.toString()
        locationStatus = "Current location captured"
    }

    fun fetchCurrentLocation() {
        locationStatus = "Finding current location…"
        locationClient.fetchFavoriteLocation(
            onSuccess = ::applyCurrentLocation,
            onUnavailable = { locationStatus = "Current location is unavailable. Try again or use manual input." },
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) fetchCurrentLocation()
        else locationStatus = "Location permission was denied. You can enter coordinates manually."
    }

    fun requestCurrentLocation() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) fetchCurrentLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val lat = latitude.toDoubleOrNull()
    val lng = longitude.toDoubleOrNull()
    val validCoordinatePair = lat != null && lat in -90.0..90.0 && lng != null && lng in -180.0..180.0
    val coordinatesValid = when (locationMode) {
        LocationInputMode.CURRENT -> validCoordinatePair
        LocationInputMode.MANUAL -> (latitude.isBlank() && longitude.isBlank()) || validCoordinatePair
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (place == null) "Add favorite" else "Edit favorite") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(FavoritePlaceType.entries) { candidate ->
                            FilterChip(
                                selected = type == candidate,
                                onClick = { type = candidate },
                                label = { Text(candidate.name.substringBefore('_').lowercase().replaceFirstChar(Char::uppercase)) },
                            )
                        }
                    }
                }
                item { OutlinedTextField(address, { address = it }, label = { Text("Address") }) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LocationInputMode.entries) { mode ->
                            FilterChip(
                                selected = locationMode == mode,
                                onClick = { locationMode = mode; locationStatus = null },
                                label = { Text(if (mode == LocationInputMode.CURRENT) "Current location" else "Manual input") },
                            )
                        }
                    }
                }
                if (locationMode == LocationInputMode.CURRENT) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::requestCurrentLocation, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.MyLocation, contentDescription = null)
                                Text(" Use current location")
                            }
                            locationStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            if (validCoordinatePair) {
                                Text("%.5f, %.5f".format(lat, lng), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                item { OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }) }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && coordinatesValid,
                onClick = { onSave(place?.id ?: 0, name, type, address, lat, lng, notes) },
            ) { Text("Save") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private enum class LocationInputMode { CURRENT, MANUAL }

@SuppressLint("MissingPermission")
private fun FusedLocationProviderClient.fetchFavoriteLocation(
    onSuccess: (Double, Double) -> Unit,
    onUnavailable: () -> Unit,
) {
    getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { location ->
            if (location == null) onUnavailable() else onSuccess(location.latitude, location.longitude)
        }
        .addOnFailureListener { onUnavailable() }
}
