package com.dopetechindia.bikenavigation.data.di

import android.content.Context
import com.dopetechindia.bikenavigation.data.auth.AuthRepository
import com.dopetechindia.bikenavigation.data.auth.FirebaseAuthRepository
import com.dopetechindia.bikenavigation.data.repository.FirebaseGroupRepository
import com.dopetechindia.bikenavigation.data.repository.FirebaseLocationRepository
import com.dopetechindia.bikenavigation.data.repository.FirebaseTelemetryRepository
import com.dopetechindia.bikenavigation.data.repository.GroupRepository
import com.dopetechindia.bikenavigation.data.repository.LocationRepository
import com.dopetechindia.bikenavigation.data.repository.PlaceSearchHelper
import com.dopetechindia.bikenavigation.data.repository.SavedPlacesRepository
import com.dopetechindia.bikenavigation.data.repository.SharedPreferencesSavedPlacesRepository
import com.dopetechindia.bikenavigation.data.repository.TelemetryRepository

/**
 * Service locator provider supplying repository singleton dependencies across the application.
 */
object RepositoryProvider {
    val authRepository: AuthRepository by lazy { FirebaseAuthRepository() }
    val locationRepository: LocationRepository by lazy { FirebaseLocationRepository() }
    val groupRepository: GroupRepository by lazy { FirebaseGroupRepository() }
    val telemetryRepository: TelemetryRepository by lazy { FirebaseTelemetryRepository() }
    val placeSearchHelper: PlaceSearchHelper by lazy { PlaceSearchHelper() }

    private var _savedPlacesRepository: SavedPlacesRepository? = null

    val savedPlacesRepository: SavedPlacesRepository
        get() {
            if (_savedPlacesRepository == null) {
                _savedPlacesRepository = SharedPreferencesSavedPlacesRepository()
            }
            return _savedPlacesRepository!!
        }

    fun initSavedPlacesRepository(context: Context): SavedPlacesRepository {
        val repo = _savedPlacesRepository
        if (repo is SharedPreferencesSavedPlacesRepository) {
            repo.bindContext(context.applicationContext)
            return repo
        }
        val newRepo = SharedPreferencesSavedPlacesRepository(context.applicationContext)
        _savedPlacesRepository = newRepo
        return newRepo
    }

    fun setSavedPlacesRepositoryForTest(repository: SavedPlacesRepository) {
        _savedPlacesRepository = repository
    }
}
