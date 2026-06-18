package uk.hristijan.pitstop.feature.refill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.ui.components.ErrorState
import uk.hristijan.pitstop.ui.components.LoadingState
import uk.hristijan.pitstop.ui.components.PitStopTopAppBar

@Composable
fun RefillFormRoute(
    vehicleId: Long,
    refillId: Long? = null,
    placeValue: RefillPlaceValue? = null,
    currencySymbol: String = "€",
    onChoosePlace: (RefillPlaceValue) -> Unit = {},
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    val container = LocalAppContainer.current
    val factory = remember(container, vehicleId, refillId) {
        RefillFormViewModel.factory(container, vehicleId, refillId, placeValue)
    }
    val viewModel: RefillFormViewModel = viewModel(
        key = "refill-form-$vehicleId-${refillId ?: 0}",
        factory = factory,
    )
    RefillFormScreen(
        viewModel = viewModel,
        placeValue = placeValue,
        currencySymbol = currencySymbol,
        onChoosePlace = onChoosePlace,
        onSaved = onSaved,
        onCancel = onCancel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillFormScreen(
    viewModel: RefillFormViewModel,
    placeValue: RefillPlaceValue? = null,
    currencySymbol: String = "€",
    onChoosePlace: (RefillPlaceValue) -> Unit = {},
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(placeValue) { placeValue?.let(viewModel::setPlace) }
    LaunchedEffect(state.savedRefillId) { state.savedRefillId?.let(onSaved) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = state.timestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let(viewModel::setTimestamp)
                    showDatePicker = false
                }) { Text("Use date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }

    Scaffold(
        topBar = {
            PitStopTopAppBar(
                title = if (state.id == 0L) "Log a refill" else "Edit refill",
                onBack = onCancel,
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).imePadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !state.isSaving) { Text("Cancel") }
                Button(onClick = viewModel::save, modifier = Modifier.weight(1.5f), enabled = !state.isSaving && !state.isLoading) {
                    Text(if (state.isSaving) "Saving…" else "Save refill")
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.errorMessage != null && state.vehicle == null -> ErrorState(state.errorMessage.orEmpty(), modifier = Modifier.fillMaxSize().padding(padding))
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                RefillHero(state, currencySymbol)
                FormSection("Refill") {
                    OutlinedTextField(
                        value = state.litres,
                        onValueChange = viewModel::setLitres,
                        modifier = Modifier.weight(1f),
                        label = { Text("Fuel") },
                        suffix = { Text("L") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = state.fieldErrors["litres"] != null,
                        supportingText = state.fieldErrors["litres"]?.let { { Text(it) } },
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = state.totalCost,
                        onValueChange = viewModel::setTotalCost,
                        modifier = Modifier.weight(1f),
                        label = { Text("Total") },
                        prefix = { Text(currencySymbol) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = state.fieldErrors["cost"] != null,
                        supportingText = state.fieldErrors["cost"]?.let { { Text(it) } },
                    )
                }
                OutlinedTextField(
                    value = state.odometerKm,
                    onValueChange = viewModel::setOdometer,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Odometer") },
                    suffix = { Text("km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = state.fieldErrors["odometer"] != null,
                    supportingText = {
                        Text(state.fieldErrors["odometer"] ?: state.minimumOdometerKm?.let { "Recorded history: %,d km or higher".format(it) }.orEmpty())
                    },
                )
                OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDate(state.timestamp), style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Full tank", style = MaterialTheme.typography.titleMedium)
                            Text("Helps calculate real fuel economy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.isFullTank, onCheckedChange = viewModel::setFullTank)
                    }
                }
                FormSectionColumn("Place") {
                    if (state.place.stationName != null || state.place.latitude != null) {
                        PlaceSummary(state.place)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onChoosePlace(state.place) }) { Text("Change") }
                            TextButton(onClick = viewModel::clearPlace) { Text("Remove") }
                        }
                    } else {
                        Text("Attach a favorite station or map location.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { onChoosePlace(state.place) }) { Text("Choose place") }
                    }
                }
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    modifier = Modifier.fillMaxWidth().height(128.dp),
                    label = { Text("Notes") },
                    placeholder = { Text("Fuel grade, pump number, receipt…") },
                    isError = state.fieldErrors["notes"] != null,
                    supportingText = state.fieldErrors["notes"]?.let { { Text(it) } },
                )
                state.errorMessage?.let {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Spacer(Modifier.height(76.dp))
            }
        }
    }
}

@Composable
private fun RefillHero(state: RefillFormUiState, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("PRICE PER LITRE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f))
            Text(
                state.pricePerLitre?.let { "$currencySymbol${NumberFormat.getNumberInstance().apply { maximumFractionDigits = 3 }.format(it)}" } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text("Calculated automatically from fuel and total", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun FormSectionColumn(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
    }
}

@Composable
private fun PlaceSummary(place: RefillPlaceValue) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(place.stationName ?: "Pinned location", style = MaterialTheme.typography.titleMedium)
        if (place.latitude != null && place.longitude != null) {
            Text("%.5f, %.5f".format(place.latitude, place.longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

private fun vehicleLabel(nickname: String?, make: String, model: String): String = nickname?.takeIf { it.isNotBlank() } ?: "$make $model"
private fun formatDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
