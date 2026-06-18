package uk.hristijan.pitstop.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.pitStopPreferences: DataStore<Preferences> by preferencesDataStore(name = "pitstop_preferences")

class SelectedVehiclePreferences(private val dataStore: DataStore<Preferences>) {
    val selectedVehicleId: Flow<Long?> = dataStore.data
        .map { preferences -> preferences[SELECTED_VEHICLE_ID] }
        .distinctUntilChanged()

    suspend fun selectVehicle(vehicleId: Long?) {
        require(vehicleId == null || vehicleId > 0) { "Vehicle id must be positive" }
        dataStore.edit { preferences ->
            if (vehicleId == null) preferences.remove(SELECTED_VEHICLE_ID)
            else preferences[SELECTED_VEHICLE_ID] = vehicleId
        }
    }

    companion object {
        private val SELECTED_VEHICLE_ID = longPreferencesKey("selected_vehicle_id")

        fun from(context: Context): SelectedVehiclePreferences =
            SelectedVehiclePreferences(context.applicationContext.pitStopPreferences)
    }
}
