package uk.hristijan.pitstop.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Garage as GarageOutlined
import androidx.compose.material.icons.outlined.History as HistoryOutlined
import androidx.compose.material.icons.outlined.Home as HomeOutlined
import androidx.compose.material.icons.outlined.Map as MapOutlined
import androidx.compose.ui.graphics.vector.ImageVector

object AppRoutes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val MAP = "map"
    const val FAVORITES = "favorites"
    const val GARAGE = "garage"
    const val ADD_VEHICLE = "vehicle/add"
    const val EDIT_VEHICLE = "vehicle/{vehicleId}"
    const val ADD_REFILL = "refill/add"
    const val EDIT_REFILL = "refill/{refillId}/edit"
    const val REFILL_DETAIL = "refill/{refillId}"
    const val ADD_SERVICE = "service/add"
    const val EDIT_SERVICE = "service/{serviceId}/edit"
    const val SERVICE_DETAIL = "service/{serviceId}"
    const val PLACE_PICKER = "place-picker"

    fun editVehicle(id: Long) = "vehicle/$id"
    fun refill(id: Long) = "refill/$id"
    fun editRefill(id: Long) = "refill/$id/edit"
    fun service(id: Long) = "service/$id"
    fun editService(id: Long) = "service/$id/edit"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(AppRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.HomeOutlined),
    HISTORY(AppRoutes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.HistoryOutlined),
    MAP(AppRoutes.MAP, "Map", Icons.Filled.Map, Icons.Outlined.MapOutlined),
    FAVORITES(AppRoutes.FAVORITES, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    GARAGE(AppRoutes.GARAGE, "Garage", Icons.Filled.Garage, Icons.Outlined.GarageOutlined),
}
