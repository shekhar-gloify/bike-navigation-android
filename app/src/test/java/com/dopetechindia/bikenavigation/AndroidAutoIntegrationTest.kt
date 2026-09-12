package com.dopetechindia.bikenavigation

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.car.app.validation.HostValidator
import com.dopetechindia.bikenavigation.car.MotorcycleCarAppService
import com.dopetechindia.bikenavigation.car.MotorcycleCarSession
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.SampleRoutes
import com.dopetechindia.bikenavigation.data.model.TurnType
import com.dopetechindia.bikenavigation.location.LocationService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidAutoIntegrationTest {

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }
        })
    }

    @After
    fun tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun testCarAppServiceHostValidatorAndSessionCreation() {
        val service = MotorcycleCarAppService()
        val validator = service.createHostValidator()
        assertNotNull(validator)
        assertEquals(HostValidator.ALLOW_ALL_HOSTS_VALIDATOR, validator)

        val session = service.onCreateSession()
        assertNotNull(session)
        assertTrue(session is MotorcycleCarSession)
    }

    @Test
    fun testCarScreenStateSyncWithTurnByTurnEngine() {
        val route = SampleRoutes.cityScenicLoop
        LocationService.turnByTurnEngine.startNavigation(route, "rider_auto_test")

        val state = LocationService.turnByTurnEngine.state.value
        assertTrue(state.isNavigating)
        assertFalse(state.isDestinationReached)
        assertEquals(0, state.currentStepIndex)
        assertNotNull(state.currentManeuver)
        assertEquals(TurnType.STRAIGHT, state.currentManeuver?.turnType)

        val loc = LocationData(
            riderId = "rider_auto_test",
            latitude = 37.7749,
            longitude = -122.4194,
            speedMps = 12f
        )
        val telemetry = LocationService.turnByTurnEngine.onLocationUpdated(loc)
        assertEquals(43.2f, telemetry.currentSpeedKmh, 0.5f)
        assertTrue(telemetry.isNavigating)

        val locStep1 = LocationData(
            riderId = "rider_auto_test",
            latitude = 37.77795,
            longitude = -122.41802,
            speedMps = 10f
        )
        LocationService.turnByTurnEngine.onLocationUpdated(locStep1)
        val updatedState = LocationService.turnByTurnEngine.state.value
        assertEquals(1, updatedState.currentStepIndex)
        assertEquals(TurnType.TURN_RIGHT, updatedState.currentManeuver?.turnType)
        assertTrue(updatedState.currentManeuver?.instruction?.contains("5th St") == true)

        LocationService.turnByTurnEngine.stopNavigation()
        assertFalse(LocationService.turnByTurnEngine.state.value.isNavigating)
    }
}
