package uk.hristijan.pitstop.feature.service

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.core.model.ServiceCategory
import uk.hristijan.pitstop.core.model.ServicePerformer
import uk.hristijan.pitstop.data.local.model.ServiceWithItems
import uk.hristijan.pitstop.ui.components.ConfirmDeleteDialog
import uk.hristijan.pitstop.ui.components.EmptyState
import uk.hristijan.pitstop.ui.components.LoadingState
import uk.hristijan.pitstop.ui.components.PitStopTopAppBar

@Composable
fun ServiceListScreen(
    vehicleId: Long,
    onAdd: () -> Unit,
    onServiceSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val vm: ServiceListViewModel = viewModel(
        key = "services-$vehicleId",
        factory = ServiceListViewModelFactory(vehicleId, container.serviceRepository),
    )
    val services by vm.services.collectAsState()
    ServiceListContent(services, onAdd, onServiceSelected, modifier)
}

@Composable
fun ServiceListContent(
    services: List<ServiceWithItems>,
    onAdd: () -> Unit,
    onServiceSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Service history", style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onAdd) { Text("Add service") }
            }
        }
        if (services.isEmpty()) item {
            EmptyState("No service records", "Log maintenance and repairs for this vehicle.", actionLabel = "Add service", onAction = onAdd)
        }
        items(services, key = { it.service.id }) { value ->
            ElevatedCard(onClick = { onServiceSelected(value.service.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(value.service.title, style = MaterialTheme.typography.titleMedium)
                    Text("${value.service.category.label()} • ${formatDisplayDate(value.service.timestamp)}")
                    Text("${formatMoney(value.service.totalCostMinor)} EUR", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        item { Spacer(Modifier.padding(8.dp)) }
    }
}

@Composable
fun ServiceFormScreen(
    vehicleId: Long,
    serviceId: Long? = null,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
    onDeleted: () -> Unit = onCancel,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val vm: ServiceFormViewModel = viewModel(
        key = "service-form-$vehicleId-${serviceId ?: "new"}",
        factory = ServiceFormViewModelFactory(vehicleId, serviceId, container.serviceRepository, container.vehicleRepository),
    )
    val state by vm.state.collectAsState()
    LaunchedEffect(state.savedServiceId) { state.savedServiceId?.let(onSaved) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    ServiceFormContent(state, serviceId != null, vm::update, vm::setPerformer, vm::addItem, vm::removeItem, vm::updateItem, vm::save, vm::delete, onCancel, modifier)
}

@Composable
fun ServiceFormContent(
    state: ServiceFormState,
    isEditing: Boolean,
    update: ((ServiceFormState) -> ServiceFormState) -> Unit,
    setPerformer: (ServicePerformer) -> Unit,
    addItem: () -> Unit,
    removeItem: (Long) -> Unit,
    updateItem: (Long, (ServiceItemInput) -> ServiceItemInput) -> Unit,
    save: () -> Unit,
    delete: () -> Unit,
    cancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) ConfirmDeleteDialog("Delete service?", "This also deletes all associated parts.", delete, { confirmDelete = false })
    if (state.isLoading && isEditing && state.title.isEmpty()) return LoadingState(modifier.fillMaxSize())

    Scaffold(
        modifier = modifier,
        topBar = {
            PitStopTopAppBar(
                title = if (isEditing) "Edit service" else "Add service",
                onBack = cancel,
            )
        },
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        state.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { FormTextField(state.title, { value -> update { it.copy(title = value) } }, "Title", state.errors["title"]) }
        item { FormTextField(state.date, { value -> update { it.copy(date = value) } }, "Date (YYYY-MM-DD)", state.errors["date"]) }
        item { Selector("Category", ServiceCategory.entries, state.category, { it.label() }) { value -> update { it.copy(category = value) } } }
        item { Selector("Performed by", ServicePerformer.entries, state.performer, { it.label() }, setPerformer) }
        if (state.performer != ServicePerformer.SELF) item {
            FormTextField(state.providerName, { value -> update { it.copy(providerName = value) } }, "Provider name", state.errors["provider"])
        }
        item { NumericField(state.odometerKm, { value -> update { it.copy(odometerKm = value) } }, "Odometer", "km", state.errors["odometer"], false) }
        item { NumericField(state.laborCost, { value -> update { it.copy(laborCost = value) } }, "Labor cost", "EUR", state.errors["labor"], true) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Parts", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = addItem) { Text("Add item") }
            }
        }
        itemsIndexed(state.items, key = { _, item -> item.key }) { index, item ->
            ItemEditor(item, index, state.errors, { transform -> updateItem(item.key, transform) }, { removeItem(item.key) })
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryRow("Parts", state.partsCostMinor)
                    SummaryRow("Labor", state.laborCostMinor)
                    HorizontalDivider()
                    SummaryRow("Total", state.totalCostMinor)
                }
            }
        }
        item { Text("Location (optional)", style = MaterialTheme.typography.titleLarge) }
        item { FormTextField(state.favoritePlaceId, { value -> update { it.copy(favoritePlaceId = value) } }, "Favorite place ID", state.errors["place"], KeyboardType.Number) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(state.latitude, { value -> update { it.copy(latitude = value) } }, "Latitude", state.errors["location"], KeyboardType.Decimal, Modifier.weight(1f))
                FormTextField(state.longitude, { value -> update { it.copy(longitude = value) } }, "Longitude", null, KeyboardType.Decimal, Modifier.weight(1f))
            }
        }
        item { FormTextField(state.notes, { value -> update { it.copy(notes = value) } }, "Notes", null, minLines = 3) }
        item { Text("Next service reminder", style = MaterialTheme.typography.titleLarge) }
        item { FormTextField(state.reminderDate, { value -> update { it.copy(reminderDate = value) } }, "Reminder date (YYYY-MM-DD)", state.errors["reminderDate"]) }
        item { NumericField(state.reminderOdometerKm, { value -> update { it.copy(reminderOdometerKm = value) } }, "Reminder odometer", "km", state.errors["reminderOdometer"], false) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isEditing) TextButton(onClick = { confirmDelete = true }, enabled = !state.isLoading) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = cancel) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = save, enabled = !state.isLoading) { Text(if (state.isLoading) "Saving…" else "Save") }
            }
        }
        item { Spacer(Modifier.padding(8.dp)) }
      }
    }
}

@Composable
fun ServiceDetailScreen(serviceId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val vm: ServiceDetailViewModel = viewModel(key = "service-$serviceId", factory = ServiceDetailViewModelFactory(serviceId, container.serviceRepository))
    val value by vm.service.collectAsState()
    if (value == null) LoadingState(modifier.fillMaxSize()) else ServiceDetailContent(value!!, onBack, onEdit, modifier)
}

@Composable
fun ServiceDetailContent(value: ServiceWithItems, onBack: () -> Unit, onEdit: (Long) -> Unit, modifier: Modifier = Modifier) {
    val service = value.service
    Scaffold(
        modifier = modifier,
        topBar = {
            PitStopTopAppBar(
                title = "Service details",
                onBack = onBack,
                onEdit = { onEdit(service.id) },
            )
        },
    ) { innerPadding ->
      LazyColumn(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(service.title, style = MaterialTheme.typography.headlineMedium) }
        item { DetailRow("Date", formatDisplayDate(service.timestamp)) }
        item { DetailRow("Category", service.category.label()) }
        item { DetailRow("Performed by", service.performedBy.label()) }
        service.providerName?.let { item { DetailRow("Provider", it) } }
        service.odometerKm?.let { item { DetailRow("Odometer", "$it km") } }
        if (value.items.isNotEmpty()) item { Text("Parts", style = MaterialTheme.typography.titleLarge) }
        items(value.items, key = { it.id }) { item ->
            DetailRow("${item.description} × ${item.quantity}", "${formatMoney(item.unitCostMinor * item.quantity)} EUR")
        }
        item { HorizontalDivider() }
        item { DetailRow("Parts", "${formatMoney(value.items.sumOf { it.unitCostMinor * it.quantity })} EUR") }
        item { DetailRow("Labor", "${formatMoney(service.laborCostMinor)} EUR") }
        item { DetailRow("Total", "${formatMoney(service.totalCostMinor)} EUR", true) }
        if (service.latitude != null && service.longitude != null) item { DetailRow("Location", "${service.latitude}, ${service.longitude}") }
        service.notes?.let { item { Column { Text("Notes", style = MaterialTheme.typography.labelLarge); Text(it) } } }
        service.nextServiceAt?.let { item { DetailRow("Reminder date", formatDisplayDate(it)) } }
        service.nextServiceOdometerKm?.let { item { DetailRow("Reminder odometer", "$it km") } }
      }
    }
}

@Composable
private fun ItemEditor(item: ServiceItemInput, index: Int, errors: Map<String, String>, update: ((ServiceItemInput) -> ServiceItemInput) -> Unit, remove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Item ${index + 1}", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = remove) { Text("Remove") }
            }
            FormTextField(item.name, { update { row -> row.copy(name = it) } }, "Name", errors["item.$index.name"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormTextField(item.quantity, { update { row -> row.copy(quantity = it) } }, "Quantity", errors["item.$index.quantity"], KeyboardType.Number, Modifier.weight(1f))
                FormTextField(item.unitPrice, { update { row -> row.copy(unitPrice = it) } }, "Unit price (EUR)", errors["item.$index.price"], KeyboardType.Decimal, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun <T> Selector(title: String, values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(values) { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) },
                )
            }
        }
    }
}

@Composable
private fun FormTextField(value: String, onChange: (String) -> Unit, label: String, error: String?, keyboardType: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier, minLines: Int = 1) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, isError = error != null, supportingText = error?.let { { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), minLines = minLines, singleLine = minLines == 1)
}

@Composable
private fun NumericField(value: String, onChange: (String) -> Unit, label: String, suffix: String, error: String?, decimal: Boolean) =
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, suffix = { Text(suffix) }, isError = error != null, supportingText = error?.let { { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number), singleLine = true)

@Composable private fun SummaryRow(label: String, minor: Long) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text("${formatMoney(minor)} EUR") }
@Composable private fun DetailRow(label: String, value: String, emphasize: Boolean = false) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge) }

private fun ServiceCategory.label() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
private fun ServicePerformer.label() = name.lowercase().replaceFirstChar(Char::uppercase)
private fun formatDisplayDate(timestamp: Long): String = DateTimeFormatter.ofPattern("d MMM uuuu").format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
