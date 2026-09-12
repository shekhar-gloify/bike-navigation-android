package com.dopetechindia.bikenavigation.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dopetechindia.bikenavigation.data.model.SavedPlace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Interface contract for persisting and observing starred/bookmarked locations.
 */
interface SavedPlacesRepository {
    val savedPlaces: StateFlow<List<SavedPlace>>
    fun observeSavedPlaces(): StateFlow<List<SavedPlace>>
    fun isPlaceSaved(placeId: String): Boolean
    fun toggleSavePlace(place: SavedPlace)
    fun savePlace(place: SavedPlace)
    fun deleteSavedPlace(placeId: String)
}

/**
 * SharedPreferences-backed implementation of [SavedPlacesRepository] providing local persistent storage
 * and reactive StateFlow updates for starred locations across application restarts.
 */
class SharedPreferencesSavedPlacesRepository(
    private var context: Context? = null
) : SavedPlacesRepository {

    private var prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _savedPlaces = MutableStateFlow<List<SavedPlace>>(loadInitialPlaces())
    override val savedPlaces: StateFlow<List<SavedPlace>> = _savedPlaces.asStateFlow()

    /**
     * Binds application context to initialize SharedPreferences storage and reload persisted starred locations.
     */
    fun bindContext(context: Context) {
        this.context = context.applicationContext
        this.prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val loaded = loadInitialPlaces()
        _savedPlaces.value = loaded
    }

    override fun observeSavedPlaces(): StateFlow<List<SavedPlace>> = savedPlaces

    override fun isPlaceSaved(placeId: String): Boolean {
        return _savedPlaces.value.any { it.id == placeId }
    }

    override fun toggleSavePlace(place: SavedPlace) {
        if (isPlaceSaved(place.id)) {
            deleteSavedPlace(place.id)
        } else {
            savePlace(place)
        }
    }

    override fun savePlace(place: SavedPlace) {
        val currentList = _savedPlaces.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == place.id }
        if (index >= 0) {
            currentList[index] = place
        } else {
            currentList.add(0, place)
        }
        updatePlaces(currentList)
    }

    override fun deleteSavedPlace(placeId: String) {
        val updatedList = _savedPlaces.value.filterNot { it.id == placeId }
        updatePlaces(updatedList)
    }

    private fun updatePlaces(newList: List<SavedPlace>) {
        _savedPlaces.value = newList
        persistPlaces(newList)
    }

    private fun loadInitialPlaces(): List<SavedPlace> {
        val p = prefs ?: return defaultPlaces()
        val jsonString = p.getString(KEY_SAVED_PLACES, null) ?: return defaultPlaces()
        return try {
            val decoded = json.decodeFromString<List<SavedPlace>>(jsonString)
            decoded.ifEmpty { defaultPlaces() }
        } catch (_: Exception) {
            defaultPlaces()
        }
    }

    private fun persistPlaces(list: List<SavedPlace>) {
        val jsonString = try {
            json.encodeToString(list)
        } catch (_: Exception) {
            return
        }
        prefs?.edit()?.putString(KEY_SAVED_PLACES, jsonString)?.apply()
    }

    private fun defaultPlaces(): List<SavedPlace> {
        return listOf(
            SavedPlace(
                id = "sp_hawk_hill",
                name = "Hawk Hill Scenic Overlook",
                address = "Conzelman Rd, Mill Valley, CA",
                latitude = 37.8324,
                longitude = -122.4994,
                rating = 4.9,
                category = "Scenic Overlook"
            ),
            SavedPlace(
                id = "sp_twin_peaks",
                name = "Twin Peaks Summit",
                address = "501 Twin Peaks Blvd, San Francisco, CA",
                latitude = 37.7544,
                longitude = -122.4477,
                rating = 4.8,
                category = "Lookout Point"
            ),
            SavedPlace(
                id = "sp_ferry_building",
                name = "Ferry Building Marketplace",
                address = "1 Ferry Building, San Francisco, CA",
                latitude = 37.7955,
                longitude = -122.3937,
                rating = 4.7,
                category = "Rider Stop & Cafe"
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "bike_navigation_saved_places"
        private const val KEY_SAVED_PLACES = "saved_places_json"
    }
}
