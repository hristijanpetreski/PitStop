package uk.hristijan.pitstop.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uk.hristijan.pitstop.feature.dashboard.DashboardRoute
import uk.hristijan.pitstop.feature.favorites.FavoritesScreen
import uk.hristijan.pitstop.feature.history.HistoryScreen
import uk.hristijan.pitstop.feature.map.MapScreen
import uk.hristijan.pitstop.feature.place.PlacePickerScreen
import uk.hristijan.pitstop.feature.refill.RefillDetailRoute
import uk.hristijan.pitstop.feature.refill.RefillFormRoute
import uk.hristijan.pitstop.feature.refill.RefillPlaceValue
import uk.hristijan.pitstop.feature.refill.launchExternalNavigation
import uk.hristijan.pitstop.feature.service.ServiceDetailScreen
import uk.hristijan.pitstop.feature.service.ServiceFormScreen
import uk.hristijan.pitstop.feature.vehicle.AddEditVehicleScreen
import uk.hristijan.pitstop.feature.vehicle.FirstVehicleOnboardingScreen
import uk.hristijan.pitstop.feature.vehicle.GarageScreen
import uk.hristijan.pitstop.ui.components.LoadingState

@Composable
fun PitStopApp() {
    val application = LocalContext.current.applicationContext as PitStopApplication
    CompositionLocalProvider(LocalAppContainer provides application.container) {
        val session: AppSessionViewModel = viewModel(factory = AppSessionViewModel.Factory(application.container))
        val state by session.state.collectAsState()
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingState() }
            state.vehicles.isEmpty() -> FirstVehicleOnboardingScreen(onVehicleCreated = {})
            state.selectedVehicleId != null -> PitStopNavigationShell(state.selectedVehicleId!!)
        }
    }
}

@Composable
private fun PitStopNavigationShell(vehicleId: Long) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var pendingRefillPlace by remember { mutableStateOf<RefillPlaceValue?>(null) }

    val showNavigationBar = TopLevelDestination.entries.any { it.route == currentRoute }
    Scaffold(
        bottomBar = {
            if (showNavigationBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.openTopLevel(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = { Text(destination.label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = AppRoutes.HOME,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(AppRoutes.HOME) {
                DashboardRoute(
                    vehicleId = vehicleId,
                    onGarage = { navController.navigate(AppRoutes.GARAGE) },
                    onAddRefill = { navController.navigate(AppRoutes.ADD_REFILL) },
                    onAddService = { navController.navigate(AppRoutes.ADD_SERVICE) },
                    onHistory = { navController.navigate(AppRoutes.HISTORY) },
                    onMap = { navController.navigate(AppRoutes.MAP) },
                )
            }
            composable(AppRoutes.HISTORY) {
                HistoryScreen(
                    onRefillClick = { navController.navigate(AppRoutes.refill(it)) },
                    onServiceClick = { navController.navigate(AppRoutes.service(it)) },
                )
            }
            composable(AppRoutes.MAP) {
                MapScreen(
                    onRefillClick = { navController.navigate(AppRoutes.refill(it)) },
                    onServiceClick = { navController.navigate(AppRoutes.service(it)) },
                )
            }
            composable(AppRoutes.FAVORITES) {
                FavoritesScreen(
                    onUsePlace = { place ->
                        if (place.latitude != null && place.longitude != null) {
                            pendingRefillPlace = RefillPlaceValue(
                                favoritePlaceId = place.id,
                                stationName = place.name,
                                latitude = place.latitude,
                                longitude = place.longitude,
                            )
                            navController.navigate(AppRoutes.ADD_REFILL)
                        }
                    },
                    onNavigateToPlace = { place ->
                        if (place.latitude != null && place.longitude != null) {
                            launchExternalNavigation(context, place.latitude, place.longitude, place.name)
                        }
                    },
                )
            }
            composable(AppRoutes.GARAGE) {
                GarageScreen(
                    onAddVehicle = { navController.navigate(AppRoutes.ADD_VEHICLE) },
                    onEditVehicle = { navController.navigate(AppRoutes.editVehicle(it)) },
                    onVehicleSelected = { navController.openTopLevel(AppRoutes.HOME) },
                )
            }
            composable(AppRoutes.ADD_VEHICLE) {
                AddEditVehicleScreen(null, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
            }
            composable(AppRoutes.EDIT_VEHICLE, arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })) { entry ->
                AddEditVehicleScreen(entry.arguments?.getLong("vehicleId"), onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
            }
            composable(AppRoutes.ADD_REFILL) {
                RefillFormRoute(
                    vehicleId = vehicleId,
                    placeValue = pendingRefillPlace,
                    onChoosePlace = { navController.navigate(AppRoutes.PLACE_PICKER) },
                    onSaved = { id -> pendingRefillPlace = null; navController.navigate(AppRoutes.refill(id)) { popUpTo(AppRoutes.ADD_REFILL) { inclusive = true } } },
                    onCancel = { pendingRefillPlace = null; navController.popBackStack() },
                )
            }
            composable(AppRoutes.EDIT_REFILL, arguments = listOf(navArgument("refillId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("refillId") ?: return@composable
                RefillFormRoute(vehicleId, id, pendingRefillPlace, onChoosePlace = { navController.navigate(AppRoutes.PLACE_PICKER) }, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
            }
            composable(AppRoutes.REFILL_DETAIL, arguments = listOf(navArgument("refillId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("refillId") ?: return@composable
                RefillDetailRoute(id, onBack = { navController.popBackStack() }, onEdit = { navController.navigate(AppRoutes.editRefill(it)) }, onDeleted = { navController.popBackStack() })
            }
            composable(AppRoutes.ADD_SERVICE) {
                ServiceFormScreen(vehicleId, onSaved = { navController.navigate(AppRoutes.service(it)) { popUpTo(AppRoutes.ADD_SERVICE) { inclusive = true } } }, onCancel = { navController.popBackStack() })
            }
            composable(AppRoutes.EDIT_SERVICE, arguments = listOf(navArgument("serviceId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("serviceId") ?: return@composable
                ServiceFormScreen(vehicleId, id, onSaved = { navController.popBackStack() }, onCancel = { navController.popBackStack() })
            }
            composable(AppRoutes.SERVICE_DETAIL, arguments = listOf(navArgument("serviceId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("serviceId") ?: return@composable
                ServiceDetailScreen(id, onBack = { navController.popBackStack() }, onEdit = { navController.navigate(AppRoutes.editService(it)) })
            }
            composable(AppRoutes.PLACE_PICKER) {
                PlacePickerScreen(
                    onPlaceSelected = { place ->
                        pendingRefillPlace = RefillPlaceValue(
                            favoritePlaceId = place.favoritePlaceId,
                            stationName = place.name,
                            latitude = place.latitude,
                            longitude = place.longitude,
                        )
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.openTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
