package uk.hristijan.pitstop.feature.vehicle

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.hristijan.pitstop.app.AppContainer
import uk.hristijan.pitstop.core.model.FuelType
import uk.hristijan.pitstop.data.local.entity.Vehicle
import uk.hristijan.pitstop.data.preferences.SelectedVehiclePreferences
import uk.hristijan.pitstop.data.repository.VehicleRepository

data class VehicleFormState(
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val trim: String = "",
    val vin: String = "",
    val registration: String = "",
    val nickname: String = "",
    val fuelType: FuelType = FuelType.OTHER,
    val odometerKm: String = "",
    val photoPath: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val showErrors: Boolean = false,
    val errorMessage: String? = null,
) {
    val makeError: String? get() = if (showErrors && make.isBlank()) "Make is required" else null
    val modelError: String? get() = if (showErrors && model.isBlank()) "Model is required" else null
    val yearError: String? get() = when {
        !showErrors -> null
        year.isBlank() -> "Year is required"
        year.toIntOrNull()?.let { it !in 1886..2100 } != false -> "Enter a year from 1886 to 2100"
        else -> null
    }
    val vinError: String? get() = if (showErrors && vin.isNotBlank() && !VIN_REGEX.matches(vin)) {
        "VIN must be 17 characters and cannot contain I, O, or Q"
    } else null
    val odometerError: String? get() = if (
        showErrors && odometerKm.isNotBlank() && (odometerKm.toLongOrNull() == null || odometerKm.toLong() < 0)
    ) "Enter a valid odometer reading" else null
    val registrationError: String? get() = if (showErrors && registration.length > 32) {
        "Registration must be at most 32 characters"
    } else null
    val isValid: Boolean get() = make.isNotBlank() && model.isNotBlank() &&
        year.toIntOrNull()?.let { it in 1886..2100 } == true &&
        (vin.isBlank() || VIN_REGEX.matches(vin)) &&
        (odometerKm.isBlank() || (odometerKm.toLongOrNull()?.let { it >= 0 } == true)) &&
        registration.length <= 32

    private companion object {
        val VIN_REGEX = Regex("^[A-HJ-NPR-Z0-9]{17}$")
    }
}

sealed interface VehicleEditorEvent {
    data class Saved(val vehicleId: Long) : VehicleEditorEvent
}

class VehicleEditorViewModel(
    private val vehicleId: Long?,
    private val repository: VehicleRepository,
    private val selectedPreferences: SelectedVehiclePreferences,
    private val photoStorage: VehiclePhotoStorage,
) : ViewModel() {
    private val _state = MutableStateFlow(VehicleFormState(isLoading = vehicleId != null))
    val state: StateFlow<VehicleFormState> = _state
    private val _events = MutableSharedFlow<VehicleEditorEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    private var original: Vehicle? = null
    private var pendingPhotoPath: String? = null

    init {
        if (vehicleId != null) loadVehicle()
    }

    private fun loadVehicle() = viewModelScope.launch {
        runCatching { repository.getVehicle(vehicleId!!) }
            .onSuccess { vehicle ->
                original = vehicle
                if (vehicle == null) {
                    _state.update { it.copy(isLoading = false, loadFailed = true, errorMessage = "Vehicle not found") }
                } else {
                    _state.value = vehicle.toFormState()
                }
            }
            .onFailure { error ->
                _state.update { it.copy(isLoading = false, loadFailed = true, errorMessage = error.userMessage()) }
            }
    }

    fun updateMake(value: String) = change { copy(make = value) }
    fun updateModel(value: String) = change { copy(model = value) }
    fun updateYear(value: String) = change { copy(year = value.filter(Char::isDigit).take(4)) }
    fun updateTrim(value: String) = change { copy(trim = value) }
    fun updateVin(value: String) = change { copy(vin = value.uppercase().filterNot(Char::isWhitespace).take(17)) }
    fun updateRegistration(value: String) = change { copy(registration = value.uppercase()) }
    fun updateNickname(value: String) = change { copy(nickname = value) }
    fun updateFuelType(value: FuelType) = change { copy(fuelType = value) }
    fun updateOdometer(value: String) = change { copy(odometerKm = value.filter(Char::isDigit)) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    private inline fun change(block: VehicleFormState.() -> VehicleFormState) {
        _state.update { it.block().copy(errorMessage = null) }
    }

    fun importPhoto(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { photoStorage.copyFrom(uri) }
                .onSuccess { path ->
                    pendingPhotoPath?.let(photoStorage::deleteOwned)
                    pendingPhotoPath = path
                    _state.update { it.copy(photoPath = path, isSaving = false) }
                }
                .onFailure { error -> _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) } }
        }
    }

    fun removePhoto() {
        pendingPhotoPath?.let(photoStorage::deleteOwned)
        pendingPhotoPath = null
        _state.update { it.copy(photoPath = null) }
    }

    fun save() {
        val form = _state.value.copy(showErrors = true, errorMessage = null)
        _state.value = form
        if (!form.isValid || form.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            val old = original
            val vehicle = Vehicle(
                id = old?.id ?: 0,
                make = form.make.trim(), model = form.model.trim(), year = form.year.toInt(),
                trim = form.trim.nullIfBlank(), vin = form.vin.nullIfBlank(),
                registration = form.registration.nullIfBlank(), nickname = form.nickname.nullIfBlank(),
                fuelType = form.fuelType, odometerKm = form.odometerKm.toLongOrNull(),
                photoPath = form.photoPath, createdAt = old?.createdAt ?: now, updatedAt = now,
            )
            runCatching {
                val id = repository.save(vehicle)
                if (old == null) selectedPreferences.selectVehicle(id)
                if (old?.photoPath != null && old.photoPath != vehicle.photoPath) photoStorage.deleteOwned(old.photoPath)
                pendingPhotoPath = null
                id
            }.onSuccess { id ->
                _state.update { it.copy(isSaving = false) }
                _events.emit(VehicleEditorEvent.Saved(id))
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false, errorMessage = error.userMessage()) }
            }
        }
    }

    override fun onCleared() {
        pendingPhotoPath?.let(photoStorage::deleteOwned)
    }
}

data class GarageUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleId: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class GarageViewModel(
    private val repository: VehicleRepository,
    private val selectedPreferences: SelectedVehiclePreferences,
    private val photoStorage: VehiclePhotoStorage,
) : ViewModel() {
    private val errors = MutableStateFlow<String?>(null)
    val state: StateFlow<GarageUiState> = combine(
        repository.observeVehicles(), selectedPreferences.selectedVehicleId, errors,
    ) { vehicles, selectedId, error ->
        GarageUiState(vehicles, selectedId?.takeIf { id -> vehicles.any { it.id == id } }, false, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GarageUiState())

    init {
        viewModelScope.launch {
            combine(repository.observeVehicles(), selectedPreferences.selectedVehicleId) { vehicles, selected -> vehicles to selected }
                .collect { (vehicles, selected) ->
                    if (vehicles.isEmpty() && selected != null) selectedPreferences.selectVehicle(null)
                    else if (vehicles.isNotEmpty() && vehicles.none { it.id == selected }) {
                        selectedPreferences.selectVehicle(vehicles.first().id)
                    }
                }
        }
    }

    fun selectVehicle(id: Long) = launchAction { selectedPreferences.selectVehicle(id) }

    fun deleteVehicle(id: Long) = launchAction {
        val vehicle = repository.getVehicle(id) ?: return@launchAction
        repository.delete(vehicle)
        val remaining = state.value.vehicles.filterNot { it.id == id }
        if (state.value.selectedVehicleId == id) selectedPreferences.selectVehicle(remaining.firstOrNull()?.id)
        vehicle.photoPath?.let(photoStorage::deleteOwned)
    }

    fun clearError() { errors.value = null }

    private fun launchAction(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { errors.value = it.userMessage() }
    }
}

class VehicleEditorViewModelFactory(
    private val vehicleId: Long?,
    private val container: AppContainer,
    context: Context,
) : ViewModelProvider.Factory {
    private val photoStorage = VehiclePhotoStorage(context.applicationContext)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(VehicleEditorViewModel::class.java))
        return VehicleEditorViewModel(vehicleId, container.vehicleRepository, container.selectedVehiclePreferences, photoStorage) as T
    }
}

class GarageViewModelFactory(
    private val container: AppContainer,
    context: Context,
) : ViewModelProvider.Factory {
    private val photoStorage = VehiclePhotoStorage(context.applicationContext)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GarageViewModel::class.java))
        return GarageViewModel(container.vehicleRepository, container.selectedVehiclePreferences, photoStorage) as T
    }
}

class VehiclePhotoStorage(private val context: Context) {
    private val directory: File get() = File(context.filesDir, "vehicle_photos")

    suspend fun copyFrom(uri: Uri): String = withContext(Dispatchers.IO) {
        directory.mkdirs()
        val extension = when (context.contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }
        val target = File(directory, "${UUID.randomUUID()}$extension")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use(input::copyTo)
            } ?: error("The selected photo could not be opened")
            target.absolutePath
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    fun deleteOwned(path: String) {
        val file = File(path)
        runCatching {
            if (file.canonicalFile.parentFile == directory.canonicalFile) file.delete()
        }
    }
}

private fun Vehicle.toFormState() = VehicleFormState(
    make = make, model = model, year = year.toString(), trim = trim.orEmpty(), vin = vin.orEmpty(),
    registration = registration.orEmpty(), nickname = nickname.orEmpty(), fuelType = fuelType,
    odometerKm = odometerKm?.toString().orEmpty(), photoPath = photoPath,
)

private fun String.nullIfBlank(): String? = trim().ifBlank { null }
private fun Throwable.userMessage(): String = message?.takeIf(String::isNotBlank) ?: "Something went wrong. Please try again."
