package uk.hristijan.pitstop.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import uk.hristijan.pitstop.app.LocalCurrency
import uk.hristijan.pitstop.core.format.formatMoney
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.ui.components.EmptyState
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun HistoryScreen(
    onRefillClick: (Long) -> Unit,
    onServiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModelFactory(
            container.selectedVehiclePreferences,
            container.refillRepository,
            container.serviceRepository,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    HistoryContent(
        state = state,
        onQueryChange = viewModel::setQuery,
        onTypeChange = viewModel::setType,
        onRefillClick = onRefillClick,
        onServiceClick = onServiceClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onTypeChange: (HistoryType) -> Unit,
    onRefillClick: (Long) -> Unit,
    onServiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchActive by remember { mutableStateOf(state.query.isNotEmpty()) }
    val searchFocusRequester = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            placeholder = { Text("Search history") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    } else {
                        Text("History")
                    }
                },
                navigationIcon = {
                    if (searchActive) {
                        IconButton(onClick = { onQueryChange(""); searchActive = false }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close search")
                        }
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search history")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
      Column(Modifier.fillMaxSize().padding(innerPadding)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            HistoryType.entries.forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.name.lowercase().replaceFirstChar(Char::uppercase)) },
                )
            }
        }
        when {
            state.activeVehicleId == null -> EmptyState(
                title = "No active vehicle",
                message = "Select a vehicle to see its refill and service history.",
                modifier = Modifier.fillMaxSize(),
            )
            state.entries.isEmpty() -> EmptyState(
                title = if (state.query.isBlank()) "No records yet" else "No matches",
                message = if (state.query.isBlank()) "Refills and services will appear here." else "Try a different search or filter.",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.entries, key = { "${it::class.simpleName}-${it.id}" }) { entry ->
                    HistoryRow(entry) {
                        when (entry) {
                            is HistoryEntry.RefillEntry -> onRefillClick(entry.id)
                            is HistoryEntry.ServiceEntry -> onServiceClick(entry.id)
                        }
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val currencyCode = LocalCurrency.current
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestamp))
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val title = when (entry) {
                    is HistoryEntry.RefillEntry -> entry.refill.stationName ?: "Fuel refill"
                    is HistoryEntry.ServiceEntry -> entry.service.title
                }
                val detail = when (entry) {
                    is HistoryEntry.RefillEntry -> "${entry.refill.litres} L · ${entry.refill.odometerKm} km"
                    is HistoryEntry.ServiceEntry -> entry.service.providerName ?: entry.service.category.name.replace('_', ' ')
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$date · $detail", style = MaterialTheme.typography.bodyMedium)
            }
            val cost = when (entry) {
                is HistoryEntry.RefillEntry -> entry.refill.totalCostMinor
                is HistoryEntry.ServiceEntry -> entry.service.totalCostMinor
            }
            Text(formatMoney(cost, currencyCode), fontWeight = FontWeight.Bold)
        }
    }
}
