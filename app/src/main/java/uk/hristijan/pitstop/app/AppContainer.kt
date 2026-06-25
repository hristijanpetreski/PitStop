package uk.hristijan.pitstop.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import uk.hristijan.pitstop.data.local.PitStopDatabase
import uk.hristijan.pitstop.data.preferences.SelectedVehiclePreferences
import uk.hristijan.pitstop.data.preferences.UserSettingsPreferences
import uk.hristijan.pitstop.data.repository.DashboardRepository
import uk.hristijan.pitstop.data.repository.FavoritePlaceRepository
import uk.hristijan.pitstop.data.repository.LocalDashboardRepository
import uk.hristijan.pitstop.data.repository.LocalFavoritePlaceRepository
import uk.hristijan.pitstop.data.repository.LocalRefillRepository
import uk.hristijan.pitstop.data.repository.LocalServiceRepository
import uk.hristijan.pitstop.data.repository.LocalVehicleRepository
import uk.hristijan.pitstop.data.repository.RefillRepository
import uk.hristijan.pitstop.data.repository.ServiceRepository
import uk.hristijan.pitstop.data.repository.VehicleRepository

private val Context.pitStopDataStore by preferencesDataStore(name = "pitstop_preferences")

/** Application-scoped dependencies. Feature repositories can be added here as they land. */
interface AppContainer {
    val preferences: DataStore<Preferences>
    val selectedVehiclePreferences: SelectedVehiclePreferences
    val userSettingsPreferences: UserSettingsPreferences
    val vehicleRepository: VehicleRepository
    val refillRepository: RefillRepository
    val serviceRepository: ServiceRepository
    val favoritePlaceRepository: FavoritePlaceRepository
    val dashboardRepository: DashboardRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val preferences: DataStore<Preferences> = context.pitStopDataStore
    private val database = PitStopDatabase.getInstance(context)
    override val selectedVehiclePreferences = SelectedVehiclePreferences(preferences)
    override val userSettingsPreferences = UserSettingsPreferences(preferences)
    override val vehicleRepository = LocalVehicleRepository(database.vehicleDao())
    override val refillRepository = LocalRefillRepository(database.refillDao())
    override val serviceRepository = LocalServiceRepository(database.serviceRecordDao())
    override val favoritePlaceRepository = LocalFavoritePlaceRepository(database.favoritePlaceDao())
    override val dashboardRepository = LocalDashboardRepository(database.refillDao(), database.serviceRecordDao())
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("No AppContainer provided")
}
