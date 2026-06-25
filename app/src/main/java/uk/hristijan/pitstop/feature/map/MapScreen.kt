package uk.hristijan.pitstop.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import uk.hristijan.pitstop.app.LocalAppContainer
import uk.hristijan.pitstop.app.LocalCurrency
import uk.hristijan.pitstop.core.format.formatMoney
import uk.hristijan.pitstop.feature.refill.launchExternalNavigation
import uk.hristijan.pitstop.ui.components.EmptyState
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.ColorFilter

@Composable
fun MapScreen(
    onRefillClick: (Long) -> Unit,
    onServiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel: MapViewModel = viewModel(
        factory = MapViewModelFactory(
            container.selectedVehiclePreferences,
            container.refillRepository,
            container.serviceRepository,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    MapContent(state, viewModel::setFilter, onRefillClick, onServiceClick, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapContent(
    state: MapUiState,
    onFilterChange: (MapFilter) -> Unit,
    onRefillClick: (Long) -> Unit,
    onServiceClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(state.entries) { mutableStateOf<MapEntry?>(null) }
    val cameraState = rememberCameraPositionState()
    val context = LocalContext.current
    LaunchedEffect(state.entries.firstOrNull()?.id) {
        state.entries.firstOrNull()?.let {
            MapsInitializer.initialize(context)
            cameraState.move(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 11f))
        }
    }

    val refillColor = MaterialTheme.colorScheme.tertiary
    val serviceColor = MaterialTheme.colorScheme.secondary

    val refillPin = rememberCustomMarker(
        imageVector = Icons.Outlined.LocalGasStation,
        pinColor = refillColor
    )
    val servicePin = rememberCustomMarker(
        imageVector = Icons.Outlined.Build,
        pinColor = serviceColor
    )

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.activeVehicleId == null -> EmptyState(
                    title = "No active vehicle",
                    message = "Select a vehicle to map its records.",
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
                state.entries.isEmpty() -> EmptyState(
                    title = "Nothing to map",
                    message = if (state.recordsWithoutCoordinates > 0) "Add locations to your records to see them here." else "Records with locations will appear here.",
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState
                    ) {
                        state.entries.forEach { entry ->
                            val isRefill = entry is MapEntry.RefillEntry
                            Marker(
                                state = MarkerState(LatLng(entry.latitude, entry.longitude)),
                                title = when (entry) {
                                    is MapEntry.RefillEntry -> entry.refill.stationName ?: "Fuel refill"
                                    is MapEntry.ServiceEntry -> entry.service.title
                                },
                                icon = if (isRefill) refillPin else servicePin,
                                onClick = { selected = entry; true },
                            )
                        }
                    }
                }
            }

            // Floating Header (Title + Filters + Unmapped Badge)
            if (state.activeVehicleId != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = innerPadding.calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Map View",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (state.recordsWithoutCoordinates > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Warning,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${state.recordsWithoutCoordinates} unmapped",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MapFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = state.filter == filter,
                                        onClick = { onFilterChange(filter) },
                                        label = { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) },
                                        leadingIcon = if (state.filter == filter) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Detail Card with slide animation
            if (state.activeVehicleId != null) {
                AnimatedVisibility(
                    visible = selected != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    selected?.let { entry ->
                        MapSelectionCard(
                            entry = entry,
                            onDismiss = { selected = null },
                            onOpen = {
                                when (entry) {
                                    is MapEntry.RefillEntry -> onRefillClick(entry.id)
                                    is MapEntry.ServiceEntry -> onServiceClick(entry.id)
                                }
                            },
                            refillColor = refillColor,
                            serviceColor = serviceColor
                        )
                    }
                }
            }
        }
    }
}

private data class MapCardDetails(
    val title: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val subtitle: String,
    val odometer: String,
    val cost: Long,
    val notes: String?
)

@Composable
private fun MapSelectionCard(
    entry: MapEntry,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    refillColor: androidx.compose.ui.graphics.Color,
    serviceColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.timestamp))
    val currencyCode = LocalCurrency.current
    val context = LocalContext.current

    val cardDetails = when (entry) {
        is MapEntry.RefillEntry -> {
            val sub = "$date · ${entry.refill.litres} L"
            val odo = "${NumberFormat.getNumberInstance().format(entry.refill.odometerKm)} km"
            MapCardDetails(
                title = entry.refill.stationName ?: "Fuel refill",
                icon = Icons.Outlined.LocalGasStation,
                color = refillColor,
                subtitle = sub,
                odometer = odo,
                cost = entry.refill.totalCostMinor,
                notes = entry.refill.notes
            )
        }
        is MapEntry.ServiceEntry -> {
            val sub = date
            val odo = entry.service.odometerKm?.let { "${NumberFormat.getNumberInstance().format(it)} km" } ?: "No odometer"
            MapCardDetails(
                title = entry.service.title,
                icon = Icons.Outlined.Build,
                color = serviceColor,
                subtitle = sub,
                odometer = odo,
                cost = entry.service.totalCostMinor,
                notes = entry.service.notes
            )
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardDetails.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cardDetails.icon,
                        contentDescription = null,
                        tint = cardDetails.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cardDetails.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = cardDetails.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = cardDetails.odometer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    when (entry) {
                        is MapEntry.RefillEntry -> {
                            if (entry.refill.isFullTank) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Full tank", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                        is MapEntry.ServiceEntry -> {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(entry.service.category.name.lowercase().replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Cost",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMoney(cardDetails.cost, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!cardDetails.notes.isNullOrBlank()) {
                Text(
                    text = cardDetails.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        launchExternalNavigation(context, entry.latitude, entry.longitude, cardDetails.title)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions")
                }

                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("View details")
                }
            }
        }
    }
}

@Composable
private fun rememberCustomMarker(
    imageVector: ImageVector,
    pinColor: androidx.compose.ui.graphics.Color
): BitmapDescriptor? {
    val density = LocalDensity.current
    val painter = rememberVectorPainter(imageVector)
    
    return remember(imageVector, pinColor, density) {
        try {
            val scale = density.density
            val width = (40 * scale).toInt()
            val height = (50 * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            val shadowPaint = Paint().apply {
                color = android.graphics.Color.argb(70, 0, 0, 0)
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            val shadowWidth = 14f * scale
            val shadowHeight = 4f * scale
            canvas.drawOval(
                (width / 2f) - shadowWidth,
                height - shadowHeight - (2f * scale),
                (width / 2f) + shadowWidth,
                height - (2f * scale),
                shadowPaint
            )
            
            val pinPaint = Paint().apply {
                color = pinColor.toArgb()
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            
            val pinPath = Path().apply {
                val radius = 18f * scale
                val centerX = width / 2f
                val centerY = radius + (2f * scale)
                
                addArc(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                    120f,
                    300f
                )
                val pointerY = height - (6f * scale)
                lineTo(centerX, pointerY)
                close()
            }
            canvas.drawPath(pinPath, pinPaint)
            
            val borderPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * scale
            }
            canvas.drawPath(pinPath, borderPaint)

            val whiteRadius = 11f * scale
            val centerX = width / 2f
            val centerY = 18f * scale + (2f * scale)
            
            val innerCirclePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            canvas.drawCircle(centerX, centerY, whiteRadius, innerCirclePaint)
            
            val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)
            val drawScope = CanvasDrawScope()
            val bitmapSize = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
            val iconSize = androidx.compose.ui.geometry.Size(14f * scale, 14f * scale)
            
            drawScope.draw(
                density = density,
                layoutDirection = LayoutDirection.Ltr,
                canvas = composeCanvas,
                size = bitmapSize
            ) {
                drawContext.transform.translate(centerX - 7f * scale, centerY - 7f * scale)
                with(painter) {
                    draw(iconSize, colorFilter = ColorFilter.tint(pinColor))
                }
            }
            
            BitmapDescriptorFactory.fromBitmap(bitmap)
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "Failed to create custom marker: ${e.message}", e)
            try {
                BitmapDescriptorFactory.defaultMarker()
            } catch (ex: Exception) {
                null
            }
        }
    }
}
