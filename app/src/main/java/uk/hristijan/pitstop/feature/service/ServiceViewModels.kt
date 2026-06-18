package uk.hristijan.pitstop.feature.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.hristijan.pitstop.core.model.ServiceCategory
import uk.hristijan.pitstop.core.model.ServicePerformer
import uk.hristijan.pitstop.data.local.entity.ServiceItem
import uk.hristijan.pitstop.data.local.entity.ServiceRecord
import uk.hristijan.pitstop.data.local.model.ServiceWithItems
import uk.hristijan.pitstop.data.repository.ServiceRepository
import uk.hristijan.pitstop.data.repository.VehicleRepository

data class ServiceItemInput(
    val key: Long,
    val name: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
)

data class ServiceFormState(
    val isLoading: Boolean = false,
    val title: String = "",
    val date: String = LocalDate.now().toString(),
    val category: ServiceCategory = ServiceCategory.ROUTINE_MAINTENANCE,
    val performer: ServicePerformer = ServicePerformer.SELF,
    val providerName: String = "",
    val odometerKm: String = "",
    val laborCost: String = "",
    val favoritePlaceId: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val notes: String = "",
    val reminderDate: String = "",
    val reminderOdometerKm: String = "",
    val items: List<ServiceItemInput> = emptyList(),
    val errors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val savedServiceId: Long? = null,
    val deleted: Boolean = false,
) {
    val partsCostMinor: Long get() = items.sumOf { item ->
        val quantity = item.quantity.toIntOrNull() ?: 0
        val price = parseMoneyMinor(item.unitPrice) ?: 0
        runCatching { Math.multiplyExact(quantity.toLong(), price) }.getOrDefault(0)
    }
    val laborCostMinor: Long get() = parseMoneyMinor(laborCost) ?: 0
    val totalCostMinor: Long get() = runCatching { Math.addExact(partsCostMinor, laborCostMinor) }.getOrDefault(0)
}

class ServiceFormViewModel(
    private val vehicleId: Long,
    private val serviceId: Long?,
    private val serviceRepository: ServiceRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ServiceFormState(isLoading = serviceId != null))
    val state: StateFlow<ServiceFormState> = _state.asStateFlow()
    private var existing: ServiceRecord? = null
    private var nextItemKey = 1L

    init {
        if (serviceId != null) viewModelScope.launch {
            serviceRepository.observeService(serviceId).collect { value ->
                if (value != null && existing == null) load(value)
            }
        }
    }

    fun update(transform: (ServiceFormState) -> ServiceFormState) {
        _state.value = transform(_state.value).copy(errors = emptyMap(), errorMessage = null)
    }

    fun setPerformer(value: ServicePerformer) = update {
        it.copy(performer = value, providerName = if (value == ServicePerformer.SELF) "" else it.providerName)
    }

    fun addItem() = update { it.copy(items = it.items + ServiceItemInput(nextItemKey++)) }
    fun removeItem(key: Long) = update { it.copy(items = it.items.filterNot { row -> row.key == key }) }
    fun updateItem(key: Long, transform: (ServiceItemInput) -> ServiceItemInput) = update {
        it.copy(items = it.items.map { row -> if (row.key == key) transform(row) else row })
    }

    fun save() = viewModelScope.launch {
        val current = _state.value
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors)
            return@launch
        }
        _state.value = current.copy(isLoading = true, errorMessage = null)
        runCatching {
            requireNotNull(vehicleRepository.getVehicle(vehicleId)) { "Vehicle no longer exists" }
            val record = ServiceRecord(
                id = existing?.id ?: 0,
                vehicleId = vehicleId,
                timestamp = parseDate(current.date)!!,
                title = current.title.trim(),
                category = current.category,
                performedBy = current.performer,
                odometerKm = current.odometerKm.toLongOrNull(),
                laborCostMinor = current.laborCostMinor,
                totalCostMinor = current.totalCostMinor,
                favoritePlaceId = current.favoritePlaceId.toLongOrNull(),
                providerName = current.providerName.trim().takeIf(String::isNotEmpty),
                latitude = current.latitude.toDoubleOrNull(),
                longitude = current.longitude.toDoubleOrNull(),
                notes = current.notes.trim().takeIf(String::isNotEmpty),
                nextServiceAt = current.reminderDate.takeIf(String::isNotBlank)?.let(::parseDate),
                nextServiceOdometerKm = current.reminderOdometerKm.toLongOrNull(),
            )
            val items = current.items.map { row ->
                ServiceItem(
                    serviceRecordId = record.id,
                    description = row.name.trim(),
                    quantity = row.quantity.toInt(),
                    unitCostMinor = parseMoneyMinor(row.unitPrice)!!,
                )
            }
            serviceRepository.save(record, items)
        }.onSuccess { id -> _state.value = current.copy(isLoading = false, savedServiceId = id) }
            .onFailure { error -> _state.value = current.copy(isLoading = false, errorMessage = error.message ?: "Unable to save service") }
    }

    fun delete() = viewModelScope.launch {
        val record = existing ?: return@launch
        _state.value = _state.value.copy(isLoading = true)
        runCatching { serviceRepository.delete(record) }
            .onSuccess { _state.value = _state.value.copy(isLoading = false, deleted = true) }
            .onFailure { _state.value = _state.value.copy(isLoading = false, errorMessage = it.message ?: "Unable to delete service") }
    }

    private fun load(value: ServiceWithItems) {
        existing = value.service
        nextItemKey = value.items.size.toLong() + 1
        val service = value.service
        _state.value = ServiceFormState(
            title = service.title,
            date = formatDate(service.timestamp),
            category = service.category,
            performer = service.performedBy,
            providerName = service.providerName.orEmpty(),
            odometerKm = service.odometerKm?.toString().orEmpty(),
            laborCost = formatMoney(service.laborCostMinor),
            favoritePlaceId = service.favoritePlaceId?.toString().orEmpty(),
            latitude = service.latitude?.toString().orEmpty(),
            longitude = service.longitude?.toString().orEmpty(),
            notes = service.notes.orEmpty(),
            reminderDate = service.nextServiceAt?.let(::formatDate).orEmpty(),
            reminderOdometerKm = service.nextServiceOdometerKm?.toString().orEmpty(),
            items = value.items.mapIndexed { index, item ->
                ServiceItemInput(index.toLong() + 1, item.description, item.quantity.toString(), formatMoney(item.unitCostMinor))
            },
        )
    }

    private fun validate(s: ServiceFormState): Map<String, String> = buildMap {
        if (s.title.isBlank()) put("title", "Title is required")
        if (parseDate(s.date) == null) put("date", "Use YYYY-MM-DD")
        if (s.performer != ServicePerformer.SELF && s.providerName.isBlank()) put("provider", "Provider is required")
        if (s.odometerKm.isNotBlank() && (s.odometerKm.toLongOrNull() ?: -1) < 0) put("odometer", "Enter a valid odometer")
        if (parseMoneyMinor(s.laborCost) == null) put("labor", "Enter a valid non-negative amount")
        val lat = s.latitude.toDoubleOrNull()
        val lng = s.longitude.toDoubleOrNull()
        if (s.latitude.isBlank() != s.longitude.isBlank()) put("location", "Enter both latitude and longitude")
        if (s.latitude.isNotBlank() && (lat == null || lat !in -90.0..90.0)) put("location", "Latitude must be between -90 and 90")
        if (s.longitude.isNotBlank() && (lng == null || lng !in -180.0..180.0)) put("location", "Longitude must be between -180 and 180")
        if (s.reminderDate.isNotBlank() && parseDate(s.reminderDate) == null) put("reminderDate", "Use YYYY-MM-DD")
        if (s.reminderOdometerKm.isNotBlank() && (s.reminderOdometerKm.toLongOrNull() ?: -1) < 0) put("reminderOdometer", "Enter a valid odometer")
        s.favoritePlaceId.takeIf(String::isNotBlank)?.let { if ((it.toLongOrNull() ?: 0) <= 0) put("place", "Enter a valid place ID") }
        s.items.forEachIndexed { index, item ->
            if (item.name.isBlank()) put("item.$index.name", "Name is required")
            if ((item.quantity.toIntOrNull() ?: 0) <= 0) put("item.$index.quantity", "Use a positive whole number")
            if (parseMoneyMinor(item.unitPrice) == null) put("item.$index.price", "Enter a valid non-negative amount")
        }
    }
}

class ServiceListViewModel(vehicleId: Long, repository: ServiceRepository) : ViewModel() {
    val services = repository.observeServices(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class ServiceDetailViewModel(serviceId: Long, repository: ServiceRepository) : ViewModel() {
    val service = repository.observeService(serviceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class ServiceFormViewModelFactory(
    private val vehicleId: Long,
    private val serviceId: Long?,
    private val serviceRepository: ServiceRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ServiceFormViewModel(vehicleId, serviceId, serviceRepository, vehicleRepository) as T
}

class ServiceListViewModelFactory(private val vehicleId: Long, private val repository: ServiceRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ServiceListViewModel(vehicleId, repository) as T
}

class ServiceDetailViewModelFactory(private val serviceId: Long, private val repository: ServiceRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ServiceDetailViewModel(serviceId, repository) as T
}

internal fun parseMoneyMinor(value: String): Long? = try {
    if (value.isBlank()) 0 else BigDecimal(value.trim()).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact().takeIf { it >= 0 }
} catch (_: ArithmeticException) { null } catch (_: NumberFormatException) { null }

internal fun formatMoney(minor: Long): String = BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
internal fun parseDate(value: String): Long? = try {
    LocalDate.parse(value.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (_: DateTimeParseException) { null }
internal fun formatDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toString()
