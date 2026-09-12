package com.dopetechindia.bikenavigation

import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.SampleRoutes
import com.dopetechindia.bikenavigation.data.model.TurnType
import com.dopetechindia.bikenavigation.navigation.TurnByTurnEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TurnByTurnEngineTest {

    private lateinit var engine: TurnByTurnEngine

    @Before
    fun setUp() {
        engine = TurnByTurnEngine()
    }

    @Test
    fun testStartNavigationInitialState() {
        val route = SampleRoutes.cityScenicLoop
        engine.startNavigation(route, riderId = "rider_test")

        val state = engine.state.value
        assertTrue(state.isNavigating)
        assertFalse(state.isDestinationReached)
        assertEquals(0, state.currentStepIndex)
        assertNotNull(state.currentManeuver)
        assertEquals(TurnType.STRAIGHT, state.currentManeuver?.turnType)
        assertEquals(5200.0, state.telemetry.remainingDistanceMeters ?: 0.0, 0.1)
    }

    @Test
    fun testStopNavigation() {
        engine.startNavigation(SampleRoutes.cityScenicLoop, riderId = "rider_test")
        assertTrue(engine.state.value.isNavigating)

        engine.stopNavigation()
        assertFalse(engine.state.value.isNavigating)
    }

    @Test
    fun testLocationUpdateAndSpeedTelemetry() {
        val route = SampleRoutes.cityScenicLoop
        engine.startNavigation(route, riderId = "rider_test")

        val loc1 = LocationData(
            riderId = "rider_test",
            latitude = 37.7749,
            longitude = -122.4194,
            speedMps = 6.944f // 25 km/h
        )

        val telemetry1 = engine.onLocationUpdated(loc1)
        assertEquals(25f, telemetry1.currentSpeedKmh, 0.1f)
        assertEquals(25f, telemetry1.maxSpeedKmh, 0.1f)
        assertEquals(25f, telemetry1.avgSpeedKmh, 0.1f)

        val loc2 = LocationData(
            riderId = "rider_test",
            latitude = 37.7760,
            longitude = -122.4190,
            speedMps = 9.722f // 35 km/h
        )
        val telemetry2 = engine.onLocationUpdated(loc2)
        assertEquals(35f, telemetry2.currentSpeedKmh, 0.1f)
        assertEquals(35f, telemetry2.maxSpeedKmh, 0.1f)
        assertTrue(telemetry2.avgSpeedKmh > 25f && telemetry2.avgSpeedKmh < 35f)
        assertTrue(telemetry2.distanceTraveledMeters > 0)
    }

    @Test
    fun testStepAdvancementWhenApproachingWaypoint() {
        val route = SampleRoutes.cityScenicLoop
        engine.startNavigation(route, riderId = "rider_test")

        val nearStep0End = LocationData(
            riderId = "rider_test",
            latitude = 37.77795,
            longitude = -122.41802,
            speedMps = 5f
        )

        engine.onLocationUpdated(nearStep0End)
        val state = engine.state.value

        assertEquals(1, state.currentStepIndex)
        assertEquals(TurnType.TURN_RIGHT, state.currentManeuver?.turnType)
        assertTrue(state.currentManeuver?.instruction?.contains("5th St") == true)
    }

    @Test
    fun testDestinationArrival() {
        val route = SampleRoutes.cityScenicLoop
        engine.startNavigation(route, riderId = "rider_test")

        val destinationLoc = LocationData(
            riderId = "rider_test",
            latitude = 37.7980,
            longitude = -122.3920,
            speedMps = 0f
        )

        engine.onLocationUpdated(destinationLoc)
        val state = engine.state.value

        assertTrue(state.isDestinationReached)
        assertEquals(TurnType.ARRIVED, state.currentManeuver?.turnType)
        assertTrue(state.currentManeuver?.instruction?.contains("arrived") == true)
    }
}
