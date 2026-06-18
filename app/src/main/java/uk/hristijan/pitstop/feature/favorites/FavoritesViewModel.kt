package uk.hristijan.pitstop.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.hristijan.pitstop.core.model.FavoritePlaceType
import uk.hristijan.pitstop.data.local.entity.FavoritePlace
import uk.hristijan.pitstop.data.repository.FavoritePlaceRepository
import kotlin.math.abs

class FavoritesViewModel(private val repository: FavoritePlaceRepository) : ViewModel() {
    val places = repository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun save(
        id: Long = 0,
        name: String,
        type: FavoritePlaceType,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        notes: String?,
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _message.value = "A place name is required"
            return
        }
        val duplicate = places.value.any { existing ->
            existing.id != id && (
                existing.name.equals(cleanName, ignoreCase = true) ||
                    coordinatesMatch(existing.latitude, latitude) && coordinatesMatch(existing.longitude, longitude)
                )
        }
        if (duplicate) {
            _message.value = "That place is already in favorites"
            return
        }
        viewModelScope.launch {
            val previous = if (id == 0L) null else repository.getPlace(id)
            repository.save(
                FavoritePlace(
                    id = id,
                    name = cleanName,
                    type = type,
                    address = address.cleanOrNull(),
                    latitude = latitude,
                    longitude = longitude,
                    notes = notes.cleanOrNull(),
                    createdAt = previous?.createdAt ?: System.currentTimeMillis(),
                ),
            )
            _message.value = if (id == 0L) "Favorite added" else "Favorite updated"
        }
    }

    fun delete(place: FavoritePlace) = viewModelScope.launch {
        repository.delete(place)
        _message.value = "Favorite deleted"
    }

    fun clearMessage() { _message.value = null }

    private fun coordinatesMatch(first: Double?, second: Double?): Boolean =
        first != null && second != null && abs(first - second) < 0.00001

    private fun String?.cleanOrNull() = this?.trim()?.takeIf(String::isNotBlank)
}

class FavoritesViewModelFactory(private val repository: FavoritePlaceRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FavoritesViewModel(repository) as T
}
