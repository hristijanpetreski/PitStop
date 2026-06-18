package uk.hristijan.pitstop.feature.place

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import uk.hristijan.pitstop.data.repository.FavoritePlaceRepository

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
)

data class PlaceSearchState(
    val query: String = "",
    val predictions: List<PlacePrediction> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
    val placesAvailable: Boolean = true,
)

class PlacePickerViewModel(
    favoriteRepository: FavoritePlaceRepository,
    private val placesClient: PlacesClient?,
) : ViewModel() {
    val favorites = favoriteRepository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchState = MutableStateFlow(PlaceSearchState(placesAvailable = placesClient != null))
    val searchState: StateFlow<PlaceSearchState> = _searchState.asStateFlow()

    fun setQuery(value: String) {
        _searchState.value = _searchState.value.copy(query = value, error = null)
        if (value.isBlank()) _searchState.value = _searchState.value.copy(predictions = emptyList())
    }

    fun search() {
        val client = placesClient ?: run {
            _searchState.value = _searchState.value.copy(error = "Places search is unavailable. Use a favorite or drop a pin.")
            return
        }
        val query = _searchState.value.query.trim()
        if (query.length < 2) {
            _searchState.value = _searchState.value.copy(error = "Enter at least two characters")
            return
        }
        _searchState.value = _searchState.value.copy(isSearching = true, error = null)
        val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()
        client.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    predictions = response.autocompletePredictions.map { prediction ->
                        PlacePrediction(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString(),
                        )
                    },
                )
            }
            .addOnFailureListener { error ->
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    error = error.localizedMessage ?: "Place search failed",
                )
            }
    }

    fun selectPrediction(prediction: PlacePrediction, onSelected: (SelectedPlace) -> Unit) {
        val client = placesClient ?: return
        _searchState.value = _searchState.value.copy(isSearching = true, error = null)
        val fields = listOf(Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)
        client.fetchPlace(FetchPlaceRequest.newInstance(prediction.placeId, fields))
            .addOnSuccessListener { response ->
                val location = response.place.location
                if (location == null) {
                    _searchState.value = _searchState.value.copy(isSearching = false, error = "That place has no map location")
                } else {
                    _searchState.value = _searchState.value.copy(isSearching = false)
                    onSelected(
                        SelectedPlace(
                            name = response.place.displayName ?: prediction.primaryText,
                            address = response.place.formattedAddress ?: prediction.secondaryText,
                            latitude = location.latitude,
                            longitude = location.longitude,
                        ),
                    )
                }
            }
            .addOnFailureListener { error ->
                _searchState.value = _searchState.value.copy(
                    isSearching = false,
                    error = error.localizedMessage ?: "Could not load that place",
                )
            }
    }
}

class PlacePickerViewModelFactory(
    context: Context,
    private val favoriteRepository: FavoritePlaceRepository,
) : ViewModelProvider.Factory {
    private val placesClient = runCatching {
        if (Places.isInitialized()) Places.createClient(context.applicationContext) else null
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        PlacePickerViewModel(favoriteRepository, placesClient) as T
}
