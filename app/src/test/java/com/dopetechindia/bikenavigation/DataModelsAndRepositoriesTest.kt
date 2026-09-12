package com.dopetechindia.bikenavigation

import com.dopetechindia.bikenavigation.data.model.AuthState
import com.dopetechindia.bikenavigation.data.model.AuthUser
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.NavigationTelemetry
import com.dopetechindia.bikenavigation.data.model.Rider
import com.dopetechindia.bikenavigation.data.model.RiderStatus
import com.dopetechindia.bikenavigation.data.repository.FirebaseGroupRepository
import com.dopetechindia.bikenavigation.data.repository.FirebaseLocationRepository
import com.dopetechindia.bikenavigation.data.repository.FirebaseTelemetryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataModelsAndRepositoriesTest {

    @Test
    fun testRiderAndLocationModels() {
        val rider = Rider(
            id = "rider_1",
            displayName = "Alex Cycling",
            email = "alex@example.com",
            status = RiderStatus.RIDING
        )
        assertEquals("rider_1", rider.id)
        assertEquals(RiderStatus.RIDING, rider.status)

        val location = LocationData(
            riderId = rider.id,
            latitude = 37.7749,
            longitude = -122.4194,
            speedMps = 10f, // 36 km/h
            bearing = 180f,
            accuracy = 5f
        )
        assertEquals(36f, location.speedKmh, 0.01f)
    }

    @Test
    fun testNavigationTelemetryModel() {
        val telemetry = NavigationTelemetry(
            riderId = "rider_1",
            currentSpeedKmh = 25f,
            avgSpeedKmh = 22f,
            maxSpeedKmh = 40f,
            distanceTraveledMeters = 15000.0,
            batteryLevel = 85,
            isNavigating = true
        )
        assertEquals(25f, telemetry.currentSpeedKmh, 0.01f)
        assertTrue(telemetry.isNavigating)
        assertEquals(85, telemetry.batteryLevel)
    }

    @Test
    fun testGroupRepositoryLocalFallback() = runTest {
        val repository = FirebaseGroupRepository()
        val hostRider = Rider(id = "host_1", displayName = "Host Rider")
        
        val createResult = repository.createGroup("Morning Ride", hostRider)
        assertTrue(createResult.isSuccess)
        val session = createResult.getOrNull()
        assertNotNull(session)
        assertEquals("Morning Ride", session?.groupName)
        assertEquals(1, session?.memberIds?.size)

        val joinRider = Rider(id = "rider_2", displayName = "Rider Two")
        val joinResult = repository.joinGroup(session!!.groupCode, joinRider)
        assertTrue(joinResult.isSuccess)
        val updatedSession = joinResult.getOrNull()
        assertEquals(2, updatedSession?.memberIds?.size)
    }

    @Test
    fun testLocationAndTelemetryRepositoryUpdate() = runTest {
        val locationRepo = FirebaseLocationRepository()
        val loc = LocationData(riderId = "rider_1", latitude = 12.9716, longitude = 77.5946, speedMps = 5f)
        
        val updateResult = locationRepo.updateLocation(loc)
        assertTrue(updateResult.isSuccess)
        assertEquals(loc, locationRepo.currentLocation.value)

        val telemetryRepo = FirebaseTelemetryRepository()
        val telemetry = NavigationTelemetry(riderId = "rider_1", currentSpeedKmh = 18f)
        val telemResult = telemetryRepo.updateTelemetry(telemetry)
        assertTrue(telemResult.isSuccess)
        assertEquals(telemetry, telemetryRepo.currentTelemetry.value)
    }

    @Test
    fun testAuthStateSealedClass() {
        val user = AuthUser(uid = "u123", email = "test@test.com", displayName = "Tester")
        val state: AuthState = AuthState.Authenticated(user)
        assertTrue(state is AuthState.Authenticated)
        assertEquals("u123", (state as AuthState.Authenticated).user.uid)
    }
}
