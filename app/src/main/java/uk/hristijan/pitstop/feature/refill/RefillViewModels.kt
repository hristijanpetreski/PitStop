package uk.hristijan.pitstop.feature.refill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.hristijan.pitstop.app.AppContainer
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.data.local.entity.Vehicle
import uk.hristijan.pitstop.data.repository.RefillRepository
import uk.hristijan.pitstop.data.repository.VehicleRepository

data class RefillFormUiState(
    val id: Long = 0,
    val vehicleId: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val odometerKm: String = "",
    val litres: String = "",
    val totalCost: String = "",
    val isFullTank: Boolean = true,
    val place: RefillPlaceValue = RefillPlaceValue(),
    val notes: String = "",
    val vehicle: Vehicle? = null,
    val minimumOdometerKm: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val savedRefillId: Long? = null,
) {
    val pricePerLitre: Double?
        get() {
            val amount = totalCost.replace(',', '.').toDoubleOrNull() ?: return null
            val fuel = litres.replace(',', '.').toDoubleOrNull() ?: return null
            return if (amount > 0 && fuel > 0) amount / fuel else null
        }
}

class RefillFormViewModel(
    private val vehicleId: Long,
    private val refillId: Long?,
    private val refillRepository: RefillRepository,
    private val vehicleRepository: VehicleRepository,
    initialPlace: RefillPlaceValue? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RefillFormUiState(vehicleId = vehicleId))
    val uiState: StateFlow<RefillFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load(initialPlace) }
    }

    private suspend fun load(initialPlace: RefillPlaceValue?) {
        runCatching {
            val existing = refillId?.takeIf { it > 0 }?.let { refillRepository.getRefill(it) }
            require(refillId == null || refillId <= 0 || existing != null) { "Refill not found" }
            require(existing == null || existing.vehicleId == vehicleId) { "Refill belongs to another vehicle" }
            val vehicle = vehicleRepository.getVehicle(vehicleId) ?: error("Vehicle not found")
            val historyMaximum = refillRepository.observeRefills(vehicleId).first()
                .asSequence()
                .filter { it.id != existing?.id }
                .maxOfOrNull(Refill::odometerKm)
            val minimum = listOfNotNull(vehicle.odometerKm, historyMaximum).maxOrNull()
            _uiState.value = existing?.let {
                RefillFormUiState(
                    id = it.id,
                    vehicleId = it.vehicleId,
                    timestamp = it.timestamp,
                    odometerKm = it.odometerKm.toString(),
                    litres = formatDecimal(it.litres),
                    totalCost = minorToMajor(it.totalCostMinor),
                    isFullTank = it.isFullTank,
                    place = initialPlace ?: RefillPlaceValue(it.favoritePlaceId, it.stationName, it.latitude, it.longitude),
                    notes = it.notes.orEmpty(),
                    vehicle = vehicle,
                    minimumOdometerKm = minimum,
                    isLoading = false,
                )
            } ?: RefillFormUiState(
                vehicleId = vehicleId,
                odometerKm = minimum?.toString().orEmpty(),
                place = initialPlace ?: RefillPlaceValue(),
                vehicle = vehicle,
                minimumOdometerKm = minimum,
                isLoading = false,
            )
        }.onFailure { failure ->
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = failure.message ?: "Unable to load refill")
        }
    }

    fun setTimestamp(value: Long) = update { copy(timestamp = value, fieldErrors = fieldErrors - "timestamp") }
    fun setOdometer(value: String) = update { copy(odometerKm = value.filter(Char::isDigit), fieldErrors = fieldErrors - "odometer") }
    fun setLitres(value: String) {
        val normalized = decimalInput(value) ?: return
        update { copy(litres = normalized, fieldErrors = fieldErrors - "litres") }
    }
    fun setTotalCost(value: String) {
        val normalized = decimalInput(value) ?: return
        update { copy(totalCost = normalized, fieldErrors = fieldErrors - "cost") }
    }
    fun setFullTank(value: Boolean) = update { copy(isFullTank = value) }
    fun setNotes(value: String) = update { copy(notes = value, fieldErrors = fieldErrors - "notes") }
    fun setPlace(value: RefillPlaceValue) = update { copy(place = value) }
    fun clearPlace() = setPlace(RefillPlaceValue())
    fun clearError() = update { copy(errorMessage = null) }

    fun save() {
        val state = _uiState.value
        val odometer = state.odometerKm.toLongOrNull()
        val litresValue = state.litres.replace(',', '.').toDoubleOrNull()
        val costMinor = majorToMinor(state.totalCost)
        val errors = buildMap {
            if (state.timestamp <= 0) put("timestamp", "Choose a valid date")
            if (odometer == null) put("odometer", "Enter the odometer reading")
            else if (odometer < 0) put("odometer", "Odometer cannot be negative")
            else state.minimumOdometerKm?.takeIf { odometer < it }?.let { put("odometer", "Must be at least %,d km".format(it)) }
            if (litresValue == null || !litresValue.isFinite() || litresValue <= 0) put("litres", "Fuel must be greater than zero")
            if (costMinor == null || costMinor <= 0) put("cost", "Cost must be greater than zero")
            if (state.notes.length > 2_000) put("notes", "Keep notes under 2,000 characters")
        }
        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(fieldErrors = errors)
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            runCatching {
                refillRepository.save(
                    Refill(
                        id = state.id,
                        vehicleId = state.vehicleId,
                        timestamp = state.timestamp,
                        odometerKm = requireNotNull(odometer),
                        litres = requireNotNull(litresValue),
                        totalCostMinor = requireNotNull(costMinor),
                        isFullTank = state.isFullTank,
                        favoritePlaceId = state.place.favoritePlaceId,
                        stationName = state.place.stationName?.trimmedOrNull(),
                        latitude = state.place.latitude,
                        longitude = state.place.longitude,
                        notes = state.notes.trimmedOrNull(),
                    ),
                )
            }.onSuccess { id -> _uiState.value = _uiState.value.copy(isSaving = false, savedRefillId = id) }
                .onFailure { _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = it.message ?: "Unable to save refill") }
        }
    }

    private fun update(transform: RefillFormUiState.() -> RefillFormUiState) {
        _uiState.value = _uiState.value.transform()
    }

    companion object {
        fun factory(
            container: AppContainer,
            vehicleId: Long,
            refillId: Long? = null,
            initialPlace: RefillPlaceValue? = null,
        ): ViewModelProvider.Factory = RefillFormViewModelFactory(
            vehicleId, refillId, container.refillRepository, container.vehicleRepository, initialPlace,
        )
    }
}

class RefillFormViewModelFactory(
    private val vehicleId: Long,
    private val refillId: Long?,
    private val refillRepository: RefillRepository,
    private val vehicleRepository: VehicleRepository,
    private val initialPlace: RefillPlaceValue? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RefillFormViewModel::class.java))
        return RefillFormViewModel(vehicleId, refillId, refillRepository, vehicleRepository, initialPlace) as T
    }
}

data class RefillDetailUiState(
    val refill: Refill? = null,
    val vehicle: Vehicle? = null,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val deleted: Boolean = false,
)

class RefillDetailViewModel(
    private val refillId: Long,
    private val refillRepository: RefillRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RefillDetailUiState())
    val uiState: StateFlow<RefillDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val refill = refillRepository.getRefill(refillId) ?: error("Refill not found")
                refill to vehicleRepository.getVehicle(refill.vehicleId)
            }.onSuccess { (refill, vehicle) -> _uiState.value = RefillDetailUiState(refill = refill, vehicle = vehicle, isLoading = false) }
                .onFailure { _uiState.value = RefillDetailUiState(isLoading = false, errorMessage = it.message ?: "Unable to load refill") }
        }
    }

    fun delete() {
        val refill = _uiState.value.refill ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true, errorMessage = null)
            runCatching { refillRepository.delete(refill) }
                .onSuccess { _uiState.value = _uiState.value.copy(isDeleting = false, deleted = true) }
                .onFailure { _uiState.value = _uiState.value.copy(isDeleting = false, errorMessage = it.message ?: "Unable to delete refill") }
        }
    }

    companion object {
        fun factory(container: AppContainer, refillId: Long): ViewModelProvider.Factory =
            RefillDetailViewModelFactory(refillId, container.refillRepository, container.vehicleRepository)
    }
}

class RefillDetailViewModelFactory(
    private val refillId: Long,
    private val refillRepository: RefillRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RefillDetailViewModel::class.java))
        return RefillDetailViewModel(refillId, refillRepository, vehicleRepository) as T
    }
}

private fun decimalInput(value: String): String? {
    val normalized = value.replace(',', '.')
    if (normalized.count { it == '.' } > 1 || normalized.any { !it.isDigit() && it != '.' }) return null
    return normalized
}

private fun majorToMinor(value: String): Long? = runCatching {
    BigDecimal(value.replace(',', '.')).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
}.getOrNull()

private fun minorToMajor(value: Long): String = BigDecimal.valueOf(value, 2).stripTrailingZeros().toPlainString()
private fun formatDecimal(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
