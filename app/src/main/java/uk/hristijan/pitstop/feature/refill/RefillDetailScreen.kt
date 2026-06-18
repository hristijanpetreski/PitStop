package uk.hristijan.pitstop.feature.refill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.data.local.entity.Refill
import uk.hristijan.pitstop.ui.components.ErrorState
import uk.hristijan.pitstop.ui.components.LoadingState
import uk.hristijan.pitstop.ui.components.PitStopTopAppBar

@Composable
fun RefillDetailRoute(
    refillId: Long,
    currencySymbol: String = "€",
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
) {
    val container = LocalAppContainer.current
    val factory = remember(container, refillId) { RefillDetailViewModel.factory(container, refillId) }
    val viewModel: RefillDetailViewModel = viewModel(key = "refill-detail-$refillId", factory = factory)
    RefillDetailScreen(viewModel, currencySymbol, onBack, onEdit, onDeleted)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillDetailScreen(
    viewModel: RefillDetailViewModel,
    currencySymbol: String = "€",
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var navigationFailed by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) confirmDelete = false },
            title = { Text("Delete this refill?") },
            text = { Text("This removes the refill permanently and may change fuel economy totals.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }, enabled = !state.isDeleting) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep refill") } },
        )
    }
    if (navigationFailed) {
        AlertDialog(
            onDismissRequest = { navigationFailed = false },
            title = { Text("No navigation app found") },
            text = { Text("Install a maps or browser app to open this location.") },
            confirmButton = { TextButton(onClick = { navigationFailed = false }) { Text("OK") } },
        )
    }

    Scaffold(
        topBar = {
            PitStopTopAppBar(
                title = "Refill details",
                onBack = onBack,
                onEdit = state.refill?.let { refill -> { onEdit(refill.id) } },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.fillMaxSize().padding(padding))
            state.refill == null -> ErrorState(state.errorMessage ?: "Refill not found", viewModel::load, Modifier.fillMaxSize().padding(padding))
            else -> {
                val refill = state.refill!!
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    DetailHero(refill, currencySymbol)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FactCard("Fuel", "${formatLitres(refill.litres)} L", Modifier.weight(1f))
                        FactCard("Odometer", "${NumberFormat.getIntegerInstance().format(refill.odometerKm)} km", Modifier.weight(1f))
                    }
                    DetailSection("Refill") {
                        DetailRow("Date", formatDetailDate(refill.timestamp))
                        DetailRow("Tank", if (refill.isFullTank) "Filled to full" else "Partial refill")
                        DetailRow("Price / litre", "$currencySymbol${formatMinorPerLitre(refill)}")
                    }
                    if (refill.stationName != null || (refill.latitude != null && refill.longitude != null)) {
                        DetailSection("Place") {
                            refill.stationName?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                            if (refill.latitude != null && refill.longitude != null) {
                                Text("%.5f, %.5f".format(refill.latitude, refill.longitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(onClick = {
                                    navigationFailed = !launchExternalNavigation(context, refill.latitude, refill.longitude, refill.stationName)
                                }, modifier = Modifier.fillMaxWidth()) { Text("Navigate there") }
                            }
                        }
                    }
                    refill.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        DetailSection("Notes") { Text(notes, style = MaterialTheme.typography.bodyLarge) }
                    }
                    state.errorMessage?.let {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(), enabled = !state.isDeleting) {
                        Text(if (state.isDeleting) "Deleting…" else "Delete refill", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailHero(refill: Refill, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(refill.stationName ?: "FUEL REFILL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .7f))
            Text("$currencySymbol${minorToDisplay(refill.totalCostMinor)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(formatDetailDate(refill.timestamp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FactCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
}

/** Tries turn-by-turn Google Maps, then any geo handler, then a browser URL. */
fun launchExternalNavigation(context: Context, latitude: Double, longitude: Double, label: String? = null): Boolean {
    if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    val encodedLabel = Uri.encode(label ?: "Destination")
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitude,$longitude")).setPackage("com.google.android.apps.maps"),
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")),
    )
    return intents.any { intent ->
        runCatching {
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}

private fun formatDetailDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm"))
private fun minorToDisplay(value: Long): String = BigDecimal.valueOf(value, 2).setScale(2).toPlainString()
private fun formatMinorPerLitre(refill: Refill): String = BigDecimal.valueOf(refill.totalCostMinor).divide(BigDecimal.valueOf(refill.litres), 1, java.math.RoundingMode.HALF_UP).movePointLeft(2).setScale(3).toPlainString()
private fun formatLitres(value: Double): String = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(value)
