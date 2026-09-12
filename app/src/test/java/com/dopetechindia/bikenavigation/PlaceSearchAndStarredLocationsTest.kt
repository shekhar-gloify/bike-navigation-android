package com.dopetechindia.bikenavigation

import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.SavedPlace
import com.dopetechindia.bikenavigation.data.repository.PlaceSearchHelper
import com.dopetechindia.bikenavigation.data.repository.SharedPreferencesSavedPlacesRepository
import com.dopetechindia.bikenavigation.ui.dashboard.RideDashboardViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaceSearchAndStarredLocationsTest {

    private lateinit var savedPlacesRepo: SharedPreferencesSavedPlacesRepository
    private lateinit var searchHelper: PlaceSearchHelper
    private lateinit var viewModel: RideDashboardViewModel

    @Before
    fun setUp() {
        savedPlacesRepo = SharedPreferencesSavedPlacesRepository()
        searchHelper = PlaceSearchHelper()
        viewModel = RideDashboardViewModel(
            savedPlacesRepository = savedPlacesRepo,
            placeSearchHelper = searchHelper
        )
    }

    @Test
    fun testSavedPlacesRepositorySaveAndToggle() = runTest {
        val testPlace = SavedPlace(
            id = "test_place_1",
            name = "Test Lookout",
            address = "123 Scenic Way",
            latitude = 37.7800,
            longitude = -122.4000,
            rating = 4.8,
            category = "Scenic View"
        )

        assertFalse(savedPlacesRepo.isPlaceSaved(testPlace.id))

        // Save place
        savedPlacesRepo.savePlace(testPlace)
        assertTrue(savedPlacesRepo.isPlaceSaved(testPlace.id))
        var list = savedPlacesRepo.observeSavedPlaces().first()
        assertEquals(1, list.size)
        assertEquals("Test Lookout", list[0].name)

        // Toggle place (should un-star/delete)
        savedPlacesRepo.toggleSavePlace(testPlace)
        assertFalse(savedPlacesRepo.isPlaceSaved(testPlace.id))
        list = savedPlacesRepo.observeSavedPlaces().first()
        assertEquals(0, list.size)
    }

    @Test
    fun testSavedPlacesRepositoryDelete() = runTest {
        val place1 = SavedPlace(id = "p1", name = "Place 1", address = "Addr 1", latitude = 1.0, longitude = 1.0)
        val place2 = SavedPlace(id = "p2", name = "Place 2", address = "Addr 2", latitude = 2.0, longitude = 2.0)

        savedPlacesRepo.savePlace(place1)
        savedPlacesRepo.savePlace(place2)
        assertEquals(2, savedPlacesRepo.savedPlaces.value.size)

        savedPlacesRepo.deleteSavedPlace("p1")
        assertEquals(1, savedPlacesRepo.savedPlaces.value.size)
        assertFalse(savedPlacesRepo.isPlaceSaved("p1"))
        assertTrue(savedPlacesRepo.isPlaceSaved("p2"))
    }

    @Test
    fun testPlaceSearchHelperSamplePlacesAndFiltering() {
        val samplePlaces = searchHelper.samplePlaces
        assertTrue(samplePlaces.size >= 6)

        // Verify each sample place has 10 photo URLs
        for (place in samplePlaces) {
            assertEquals(10, place.photoUrls.size)
        }

        // Search for "scenic"
        val scenicResults = searchHelper.searchPlaces("scenic")
        assertTrue(scenicResults.isNotEmpty())
        assertTrue(scenicResults.any { it.name.contains("Hawk Hill", ignoreCase = true) || it.category.contains("Scenic", ignoreCase = true) })

        // Search for "diner"
        val dinerResults = searchHelper.searchPlaces("diner")
        assertTrue(dinerResults.isNotEmpty())

        // Search for unknown query -> verify dynamic fallback produces 10 photos per place
        val dynamicResults = searchHelper.searchPlaces("Custom Unknown Point", LocationData(latitude = 37.77, longitude = -122.41))
        assertTrue(dynamicResults.isNotEmpty())
        for (place in dynamicResults) {
            assertEquals(10, place.photoUrls.size)
        }
    }

    @Test
    fun testViewModelSearchAndPlaceSelection() {
        viewModel.onSearchQueryChanged("Hawk")
        assertTrue(viewModel.isSearching)
        assertTrue(viewModel.searchResults.isNotEmpty())
        assertEquals("Hawk", viewModel.searchQuery)

        val selected = viewModel.searchResults.first()
        viewModel.selectPlace(selected)

        assertEquals(selected, viewModel.selectedPlace)
        assertTrue(viewModel.showLocationDetailsSheet)
        assertFalse(viewModel.isSearching)
        assertTrue(viewModel.searchResults.isEmpty())

        viewModel.clearSearchQuery()
        assertEquals("", viewModel.searchQuery)

        viewModel.dismissLocationDetails()
        assertFalse(viewModel.showLocationDetailsSheet)
    }

    @Test
    fun testViewModelStarredPlacesSheetAndToggle() {
        val place = searchHelper.samplePlaces[0]
        assertFalse(viewModel.isPlaceSaved(place.id))

        viewModel.toggleStarPlace(place)
        assertTrue(viewModel.isPlaceSaved(place.id))

        viewModel.openStarredPlacesSheet()
        assertTrue(viewModel.showStarredPlacesSheet)

        viewModel.dismissStarredPlacesSheet()
        assertFalse(viewModel.showStarredPlacesSheet)

        viewModel.deleteSavedPlace(place.id)
        assertFalse(viewModel.isPlaceSaved(place.id))
    }

    @Test
    fun testCreateRouteToSavedPlace() {
        val place = searchHelper.samplePlaces[0]
        val startLoc = LocationData(latitude = 37.7749, longitude = -122.4194)

        val route = RideDashboardViewModel.createRouteToSavedPlace(place, startLoc)
        assertNotNull(route)
        assertEquals(place.name, route.title)
        assertTrue(route.polylinePoints.size >= 4)
        assertTrue(route.steps.size >= 3)
        assertTrue(route.totalDistanceMeters > 0)
    }
}
