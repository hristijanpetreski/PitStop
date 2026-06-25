package uk.hristijan.pitstop.feature.vehicle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.io.File
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.core.model.FuelType
import uk.hristijan.pitstop.data.local.entity.Vehicle
import uk.hristijan.pitstop.ui.components.EmptyState
import uk.hristijan.pitstop.ui.components.LoadingState
import uk.hristijan.pitstop.ui.components.PitStopTopAppBar

@Composable
fun FirstVehicleOnboardingScreen(
    onVehicleCreated: (Long) -> Unit,
    modifier: Modifier = Modifier,
) = VehicleEditorScreen(
    vehicleId = null,
    onSaved = onVehicleCreated,
    onCancel = null,
    modifier = modifier,
    onboarding = true,
)

@Composable
fun AddEditVehicleScreen(
    vehicleId: Long?,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) = VehicleEditorScreen(vehicleId, onSaved, onCancel, modifier)

@Composable
fun VehicleEditorScreen(
    vehicleId: Long?,
    onSaved: (Long) -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onboarding: Boolean = false,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val factory = remember(vehicleId, container, context) { VehicleEditorViewModelFactory(vehicleId, container, context) }
    val viewModel: VehicleEditorViewModel = viewModel(key = "vehicle-editor-$vehicleId", factory = factory)
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is VehicleEditorEvent.Saved) onSaved(event.vehicleId)
        }
    }
    VehicleEditorContent(
        state = state,
        actions = VehicleEditorActions(
            onMakeChange = viewModel::updateMake, onModelChange = viewModel::updateModel,
            onYearChange = viewModel::updateYear, onTrimChange = viewModel::updateTrim,
            onVinChange = viewModel::updateVin, onRegistrationChange = viewModel::updateRegistration,
            onNicknameChange = viewModel::updateNickname, onFuelTypeChange = viewModel::updateFuelType,
            onOdometerChange = viewModel::updateOdometer, onPhotoSelected = viewModel::importPhoto,
            onRemovePhoto = viewModel::removePhoto, onSave = viewModel::save,
        ),
        onCancel = onCancel,
        onboarding = onboarding,
        isEditing = vehicleId != null,
        modifier = modifier,
    )
}

data class VehicleEditorActions(
    val onMakeChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onYearChange: (String) -> Unit,
    val onTrimChange: (String) -> Unit,
    val onVinChange: (String) -> Unit,
    val onRegistrationChange: (String) -> Unit,
    val onNicknameChange: (String) -> Unit,
    val onFuelTypeChange: (FuelType) -> Unit,
    val onOdometerChange: (String) -> Unit,
    val onPhotoSelected: (android.net.Uri) -> Unit,
    val onRemovePhoto: () -> Unit,
    val onSave: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleEditorContent(
    state: VehicleFormState,
    actions: VehicleEditorActions,
    onCancel: (() -> Unit)?,
    onboarding: Boolean,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> uri?.let(actions.onPhotoSelected) }
    var fuelMenuOpen by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            PitStopTopAppBar(
                title = if (onboarding) "Your first vehicle" else if (isEditing) "Edit vehicle" else "Add vehicle",
                onBack = onCancel,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        if (state.loadFailed) {
            EmptyState("Vehicle unavailable", state.errorMessage.orEmpty(), Modifier.fillMaxSize().padding(padding),
                actionLabel = if (onCancel == null) null else "Go back", onAction = onCancel)
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (onboarding) Text("Let’s set up the car you’ll track in PitStop.", style = MaterialTheme.typography.bodyLarge)
            VehiclePhoto(
                path = state.photoPath,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 8f),
                onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.photoPath == null) "Choose photo" else "Change photo") }
                if (state.photoPath != null) {
                    TextButton(
                        onClick = actions.onRemovePhoto,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(state.make, actions.onMakeChange, "Make *", state.makeError, Modifier.weight(1f))
                FormField(state.model, actions.onModelChange, "Model *", state.modelError, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(state.year, actions.onYearChange, "Year *", state.yearError, Modifier.weight(1f), KeyboardType.Number)
                FormField(state.trim, actions.onTrimChange, "Trim", null, Modifier.weight(1f))
            }
            FormField(state.nickname, actions.onNicknameChange, "Nickname", null)
            FormField(state.registration, actions.onRegistrationChange, "Registration", state.registrationError)
            FormField(state.vin, actions.onVinChange, "VIN", state.vinError, supporting = "17 characters; I, O and Q are not used")
            Box {
                OutlinedTextField(
                    value = state.fuelType.displayName(), onValueChange = {}, readOnly = true,
                    label = { Text("Fuel type") }, modifier = Modifier.fillMaxWidth().clickable { fuelMenuOpen = true },
                    trailingIcon = { TextButton(onClick = { fuelMenuOpen = true }) { Text("Choose") } },
                )
                DropdownMenu(expanded = fuelMenuOpen, onDismissRequest = { fuelMenuOpen = false }) {
                    FuelType.entries.forEach { type ->
                        DropdownMenuItem(text = { Text(type.displayName()) }, onClick = {
                            actions.onFuelTypeChange(type); fuelMenuOpen = false
                        })
                    }
                }
            }
            FormField(state.odometerKm, actions.onOdometerChange, "Odometer (km)", state.odometerError, keyboardType = KeyboardType.Number)
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                onCancel?.let { OutlinedButton(onClick = it, enabled = !state.isSaving) { Text("Cancel") } }
                Button(onClick = actions.onSave, enabled = !state.isSaving, modifier = if (onCancel == null) Modifier.fillMaxWidth() else Modifier) {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (onboarding) "Start using PitStop" else "Save vehicle")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    onAddVehicle: () -> Unit,
    onEditVehicle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onVehicleSelected: (Long) -> Unit = {},
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val factory = remember(container, context) { GarageViewModelFactory(container, context) }
    val viewModel: GarageViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Vehicle?>(null) }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Garage") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Default.Add, contentDescription = "Add vehicle")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.vehicles.isEmpty() -> EmptyState(
                "Your garage is empty", "Add a vehicle to start tracking fuel, service, and running costs.",
                Modifier.fillMaxSize().padding(padding), "Add vehicle", onAddVehicle,
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.vehicles, key = Vehicle::id) { vehicle ->
                    VehicleCard(
                        vehicle, selected = state.selectedVehicleId == vehicle.id,
                        onSelect = { viewModel.selectVehicle(vehicle.id); onVehicleSelected(vehicle.id) },
                        onEdit = { onEditVehicle(vehicle.id) }, onDelete = { pendingDelete = vehicle },
                    )
                }
            }
        }
    }
    pendingDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${vehicle.title()}?") },
            text = { Text("Fuel and service records linked to this vehicle may also be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteVehicle(vehicle.id); pendingDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onSelect, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            VehiclePhoto(vehicle.photoPath, Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(vehicle.title(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (selected) Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text("${vehicle.year} · ${vehicle.make} ${vehicle.model}", style = MaterialTheme.typography.bodyMedium)
                vehicle.registration?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun VehiclePhoto(path: String?, modifier: Modifier, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick == null) modifier else modifier.clickable(onClick = onClick)
    Box(clickable.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (path != null) AsyncImage(model = File(path), contentDescription = "Vehicle photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("P", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("Vehicle photo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FormField(
    value: String, onValueChange: (String) -> Unit, label: String, error: String?,
    modifier: Modifier = Modifier.fillMaxWidth(), keyboardType: KeyboardType = KeyboardType.Text,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier,
        isError = error != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = (error ?: supporting)?.let { text -> { Text(text) } },
    )
}

private fun FuelType.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun Vehicle.title() = nickname?.takeIf(String::isNotBlank) ?: "$make $model"
