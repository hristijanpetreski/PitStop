package uk.hristijan.pitstop.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserSettingsPreferences(private val dataStore: DataStore<Preferences>) {
    val theme: Flow<String> = dataStore.data
        .map { preferences -> preferences[THEME_KEY] ?: "system" }
        .distinctUntilChanged()

    val currency: Flow<String> = dataStore.data
        .map { preferences -> preferences[CURRENCY_KEY] ?: "EUR" }
        .distinctUntilChanged()

    suspend fun setTheme(theme: String) {
        require(theme in listOf("light", "dark", "system")) { "Invalid theme option" }
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setCurrency(currency: String) {
        require(currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code" }
        dataStore.edit { preferences ->
            preferences[CURRENCY_KEY] = currency
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("user_theme")
        private val CURRENCY_KEY = stringPreferencesKey("user_currency")
    }
}
