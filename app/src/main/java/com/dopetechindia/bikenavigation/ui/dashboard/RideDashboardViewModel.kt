package com.dopetechindia.bikenavigation.ui.dashboard

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dopetechindia.bikenavigation.data.auth.AuthRepository
import com.dopetechindia.bikenavigation.data.di.RepositoryProvider
import com.dopetechindia.bikenavigation.data.model.AuthState
import com.dopetechindia.bikenavigation.data.model.AuthUser
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.NavigationRoute
import com.dopetechindia.bikenavigation.data.model.RouteStep
import com.dopetechindia.bikenavigation.data.model.SampleRoutes
import com.dopetechindia.bikenavigation.data.model.SavedPlace
import com.dopetechindia.bikenavigation.data.model.TurnType
import com.dopetechindia.bikenavigation.data.repository.PlaceSearchHelper
import com.dopetechindia.bikenavigation.data.repository.SavedPlacesRepository
import com.dopetechindia.bikenavigation.location.LocationService
import com.dopetechindia.bikenavigation.navigation.TurnByTurnState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * ViewModel governing the primary Ride Dashboard state, handling location tracking status,
 * turn-by-turn navigation sessions, high-contrast theme toggling, guest auth guards,
 * place search suggestions, and starred places management.
 */
class RideDashboardViewModel(
    private val authRepository: AuthRepository = RepositoryProvider.authRepository,
    val savedPlacesRepository: SavedPlacesRepository = RepositoryProvider.savedPlacesRepository,
    val placeSearchHelper: PlaceSearchHelper = RepositoryProvider.placeSearchHelper
) : ViewModel() {

    val currentLocation: StateFlow<LocationData?> = RepositoryProvider.locationRepository.currentLocation
    val isServiceRunning: StateFlow<Boolean> = LocationService.isRunning
    val turnByTurnState: StateFlow<TurnByTurnState> = LocationService.turnByTurnEngine.state
    val authState: StateFlow<AuthState> = authRepository.authState
    val savedPlaces: StateFlow<List<SavedPlace>> = savedPlacesRepository.savedPlaces

    var isHighContrastMode by mutableStateOf(true)
        private set

    var showGroupSharingSheet by mutableStateOf(false)
    var showSignInDialog by mutableStateOf(false)
    var showProfileDialog by mutableStateOf(false)

    // Place search and saved location sheet states
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<SavedPlace>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var selectedPlace by mutableStateOf<SavedPlace?>(null)
    var showLocationDetailsSheet by mutableStateOf(false)
    var showStarredPlacesSheet by mutableStateOf(false)

    /**
     * True if user is signed in with an authenticated non-guest account.
     */
    val isUserAuthenticated: Boolean
        get() {
            val state = authState.value
            return state is AuthState.Authenticated && !state.user.isAnonymous
        }

    val currentUser: AuthUser?
        get() = (authState.value as? AuthState.Authenticated)?.user

    /**
     * Group sharing is available for both Guest Riders in local mode and Authenticated Riders.
     */
    fun onGroupSharingClicked() {
        showGroupSharingSheet = true
    }

    fun toggleHighContrast() {
        isHighContrastMode = !isHighContrastMode
    }

    fun ensureGpsServiceStarted(context: Context) {
        if (!isServiceRunning.value) {
            LocationService.start(context)
        }
    }

    fun startRide(context: Context, route: NavigationRoute? = null) {
        ensureGpsServiceStarted(context)
        LocationService.turnByTurnEngine.startRide(route)
    }

    fun endRide() {
        LocationService.turnByTurnEngine.stopRide()
    }

    fun toggleRide(context: Context, route: NavigationRoute? = null) {
        if (turnByTurnState.value.isRideActive) {
            endRide()
        } else {
            startRide(context, route)
        }
    }

    fun startNavigation(context: Context, route: NavigationRoute = SampleRoutes.cityScenicLoop) {
        startRide(context, route)
    }

    fun stopNavigation() {
        endRide()
    }

    // Place Search & Starred Locations methods
    fun onSearchQueryChanged(query: String, context: Context? = null) {
        searchQuery = query
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
        } else {
            isSearching = true
            searchResults = placeSearchHelper.searchPlaces(
                query = query,
                userLocation = currentLocation.value,
                context = context
            )
        }
    }

    fun clearSearchQuery() {
        searchQuery = ""
        searchResults = emptyList()
        isSearching = false
    }

    fun selectPlace(place: SavedPlace) {
        selectedPlace = place
        showLocationDetailsSheet = true
        searchResults = emptyList()
        isSearching = false
    }

    fun dismissLocationDetails() {
        showLocationDetailsSheet = false
    }

    fun openStarredPlacesSheet() {
        showStarredPlacesSheet = true
    }

    fun dismissStarredPlacesSheet() {
        showStarredPlacesSheet = false
    }

    fun toggleStarPlace(place: SavedPlace) {
        savedPlacesRepository.toggleSavePlace(place)
    }

    fun isPlaceSaved(placeId: String): Boolean {
        return savedPlacesRepository.isPlaceSaved(placeId)
    }

    fun deleteSavedPlace(placeId: String) {
        savedPlacesRepository.deleteSavedPlace(placeId)
    }

    /**
     * Constructs a navigation route to the target place and initiates turn-by-turn guidance.
     */
    fun navigateToPlace(context: Context, place: SavedPlace) {
        val route = createRouteToSavedPlace(place, currentLocation.value)
        selectedPlace = place
        showLocationDetailsSheet = false
        showStarredPlacesSheet = false
        startRide(context, route)
    }

    companion object {
        fun createRouteToSavedPlace(
            place: SavedPlace,
            startLocation: LocationData? = null
        ): NavigationRoute {
            val startLat = startLocation?.latitude ?: (place.latitude - 0.02)
            val startLng = startLocation?.longitude ?: (place.longitude - 0.02)

            val midLat1 = startLat + (place.latitude - startLat) * 0.33
            val midLng1 = startLng + (place.longitude - startLng) * 0.25

            val midLat2 = startLat + (place.latitude - startLat) * 0.66
            val midLng2 = startLng + (place.longitude - startLng) * 0.75

            val polylinePoints = listOf(
                Pair(startLat, startLng),
                Pair(midLat1, midLng1),
                Pair(midLat2, midLng2),
                Pair(place.latitude, place.longitude)
            )

            val distanceMeters = calculateDistanceMeters(startLat, startLng, place.latitude, place.longitude)
            val durationSeconds = (distanceMeters / 11.1).toLong().coerceAtLeast(60L)

            val steps = listOf(
                RouteStep(
                    stepIndex = 0,
                    startLatitude = startLat,
                    startLongitude = startLng,
                    endLatitude = midLat1,
                    endLongitude = midLng1,
                    instruction = "Head toward ${place.name}",
                    distanceMeters = distanceMeters * 0.33,
                    turnType = TurnType.STRAIGHT,
                    streetName = "Main Way"
                ),
                RouteStep(
                    stepIndex = 1,
                    startLatitude = midLat1,
                    startLongitude = midLng1,
                    endLatitude = midLat2,
                    endLongitude = midLng2,
                    instruction = "Turn right onto ${place.category} Drive",
                    distanceMeters = distanceMeters * 0.33,
                    turnType = TurnType.TURN_RIGHT,
                    streetName = "${place.category} Drive"
                ),
                RouteStep(
                    stepIndex = 2,
                    startLatitude = midLat2,
                    startLongitude = midLng2,
                    endLatitude = place.latitude,
                    endLongitude = place.longitude,
                    instruction = "Arrive at ${place.name}",
                    distanceMeters = distanceMeters * 0.34,
                    turnType = TurnType.ARRIVED,
                    streetName = place.name
                )
            )

            return NavigationRoute(
                routeId = "route_to_${place.id}",
                title = place.name,
                totalDistanceMeters = distanceMeters,
                estimatedDurationSeconds = durationSeconds,
                steps = steps,
                polylinePoints = polylinePoints
            )
        }

        fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
