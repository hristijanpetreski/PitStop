package uk.hristijan.pitstop.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.hristijan.pitstop.app.AppContainer

data class SettingsUiState(
    val theme: String = "system",
    val currency: String = "EUR",
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        container.userSettingsPreferences.theme,
        container.userSettingsPreferences.currency
    ) { theme, currency ->
        SettingsUiState(theme = theme, currency = currency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: String) {
        viewModelScope.launch {
            container.userSettingsPreferences.setTheme(theme)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            container.userSettingsPreferences.setCurrency(currency)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(container) as T
    }
}
