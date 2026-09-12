package com.dopetechindia.bikenavigation.navigation

import android.location.Location
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.NavigationRoute
import com.dopetechindia.bikenavigation.data.model.NavigationTelemetry
import com.dopetechindia.bikenavigation.data.model.RouteStep
import com.dopetechindia.bikenavigation.data.model.TurnType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Encapsulates maneuver instruction details for turn-by-turn guidance.
 */
data class ManeuverInstruction(
    val stepIndex: Int,
    val instruction: String,
    val turnType: TurnType,
    val distanceToManeuverMeters: Double,
    val streetName: String
)

/**
 * State representation of turn-by-turn navigation engine.
 */
data class TurnByTurnState(
    val isNavigating: Boolean = false,
    val isRideActive: Boolean = false,
    val currentRoute: NavigationRoute? = null,
    val currentStepIndex: Int = 0,
    val currentManeuver: ManeuverInstruction? = null,
    val telemetry: NavigationTelemetry = NavigationTelemetry(),
    val isDestinationReached: Boolean = false
)

/**
 * Turn-by-Turn engine calculating real-time route progress, maneuver instructions, speed telemetry,
 * distance metrics, and ETA estimates for active bike rides.
 */
class TurnByTurnEngine {

    private val _state = MutableStateFlow(TurnByTurnState())
    val state: StateFlow<TurnByTurnState> = _state.asStateFlow()

    private var previousLocation: LocationData? = null
    private var totalDistanceTraveledMeters: Double = 0.0
    private var maxSpeedKmh: Float = 0f
    private var speedSamples = mutableListOf<Float>()
    private var riderId: String = ""
    private var rideStartTimeMillis: Long? = null

    fun startRide(route: NavigationRoute? = null, riderId: String = "rider_1") {
        this.riderId = riderId
        this.totalDistanceTraveledMeters = 0.0
        this.maxSpeedKmh = 0f
        this.speedSamples.clear()
        this.previousLocation = null
        this.rideStartTimeMillis = System.currentTimeMillis()

        val firstManeuver = if (route != null && route.steps.isNotEmpty()) {
            val step = route.steps.first()
            ManeuverInstruction(
                stepIndex = 0,
                instruction = step.instruction,
                turnType = step.turnType,
                distanceToManeuverMeters = step.distanceMeters,
                streetName = step.streetName
            )
        } else null

        val isNav = route != null
        val initialTelemetry = NavigationTelemetry(
            riderId = riderId,
            currentSpeedKmh = 0f,
            avgSpeedKmh = 0f,
            maxSpeedKmh = 0f,
            distanceTraveledMeters = 0.0,
            batteryLevel = 100,
            isNavigating = isNav,
            isRideActive = true,
            currentRouteStepIndex = if (route != null && route.steps.isNotEmpty()) 0 else null,
            etaSeconds = route?.estimatedDurationSeconds,
            remainingDistanceMeters = route?.totalDistanceMeters,
            rideDurationSeconds = 0L,
            timestamp = System.currentTimeMillis()
        )

        _state.value = TurnByTurnState(
            isNavigating = isNav,
            isRideActive = true,
            currentRoute = route,
            currentStepIndex = 0,
            currentManeuver = firstManeuver,
            telemetry = initialTelemetry,
            isDestinationReached = false
        )
    }

    fun startNavigation(route: NavigationRoute, riderId: String = "rider_1") {
        startRide(route, riderId)
    }

    fun stopRide() {
        stopNavigation()
    }

    fun stopNavigation() {
        this.totalDistanceTraveledMeters = 0.0
        this.maxSpeedKmh = 0f
        this.speedSamples.clear()
        this.previousLocation = null
        this.rideStartTimeMillis = null

        _state.value = TurnByTurnState(
            isNavigating = false,
            isRideActive = false,
            currentRoute = null,
            currentStepIndex = 0,
            currentManeuver = null,
            telemetry = NavigationTelemetry(
                riderId = riderId,
                isNavigating = false,
                isRideActive = false,
                rideDurationSeconds = 0L,
                distanceTraveledMeters = 0.0
            ),
            isDestinationReached = false
        )
    }

    fun onLocationUpdated(location: LocationData): NavigationTelemetry {
        val currentState = _state.value
        val route = currentState.currentRoute

        val currentSpeedKmh = location.speedKmh
        if (currentSpeedKmh > maxSpeedKmh) {
            maxSpeedKmh = currentSpeedKmh
        }
        if (currentSpeedKmh > 0f) {
            speedSamples.add(currentSpeedKmh)
            if (speedSamples.size > 50) {
                speedSamples.removeAt(0)
            }
        }
        val avgSpeedKmh = if (speedSamples.isNotEmpty()) speedSamples.average().toFloat() else currentSpeedKmh

        if (currentState.isRideActive) {
            previousLocation?.let { prev ->
                val distDelta = calculateDistanceMeters(
                    prev.latitude, prev.longitude,
                    location.latitude, location.longitude
                )
                if (distDelta in 0.5..150.0) {
                    totalDistanceTraveledMeters += distDelta
                }
            }
            previousLocation = location
        } else {
            previousLocation = location
        }

        val durationSeconds = rideStartTimeMillis?.let { startTime ->
            (System.currentTimeMillis() - startTime).coerceAtLeast(0L) / 1000L
        } ?: 0L

        if (!currentState.isRideActive) {
            val updatedTelemetry = NavigationTelemetry(
                riderId = location.riderId.ifEmpty { riderId },
                currentSpeedKmh = currentSpeedKmh,
                avgSpeedKmh = avgSpeedKmh,
                maxSpeedKmh = maxSpeedKmh,
                distanceTraveledMeters = totalDistanceTraveledMeters,
                isNavigating = false,
                isRideActive = false,
                rideDurationSeconds = 0L,
                timestamp = System.currentTimeMillis()
            )
            _state.value = currentState.copy(telemetry = updatedTelemetry)
            return updatedTelemetry
        }

        if (!currentState.isNavigating || route == null || route.steps.isEmpty()) {
            val updatedTelemetry = NavigationTelemetry(
                riderId = location.riderId.ifEmpty { riderId },
                currentSpeedKmh = currentSpeedKmh,
                avgSpeedKmh = avgSpeedKmh,
                maxSpeedKmh = maxSpeedKmh,
                distanceTraveledMeters = totalDistanceTraveledMeters,
                isNavigating = false,
                isRideActive = true,
                rideDurationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis()
            )
            _state.value = currentState.copy(isRideActive = true, isNavigating = false, telemetry = updatedTelemetry)
            return updatedTelemetry
        }

        var stepIdx = currentState.currentStepIndex
        var currentStep = route.steps.getOrNull(stepIdx) ?: route.steps.last()

        var distToStepEnd = calculateDistanceMeters(
            location.latitude, location.longitude,
            currentStep.endLatitude, currentStep.endLongitude
        )

        val stepEndThresholdMeters = 25.0
        for (i in (stepIdx + 1) until route.steps.size) {
            val stepDist = calculateDistanceMeters(
                location.latitude, location.longitude,
                route.steps[i].endLatitude, route.steps[i].endLongitude
            )
            if (stepDist < stepEndThresholdMeters || (i == route.steps.size - 1 && stepDist < 50.0)) {
                stepIdx = i
                currentStep = route.steps[i]
                distToStepEnd = stepDist
            }
        }

        if (distToStepEnd < stepEndThresholdMeters && stepIdx < route.steps.size - 1) {
            stepIdx++
            currentStep = route.steps[stepIdx]
            distToStepEnd = calculateDistanceMeters(
                location.latitude, location.longitude,
                currentStep.endLatitude, currentStep.endLongitude
            )
        }

        val isReached = stepIdx == route.steps.size - 1 && distToStepEnd < 25.0

        var remainingDistanceMeters = distToStepEnd
        for (i in (stepIdx + 1) until route.steps.size) {
            remainingDistanceMeters += route.steps[i].distanceMeters
        }

        val speedMps = if (currentSpeedKmh > 3.0f) (currentSpeedKmh / 3.6f) else 4.16f
        val etaSeconds = (remainingDistanceMeters / speedMps).toLong()

        val turnType = if (isReached) TurnType.ARRIVED else currentStep.turnType
        val instructionText = if (isReached) {
            "You have arrived at your destination!"
        } else {
            val distFormatted = if (distToStepEnd >= 1000) {
                String.format(Locale.US, "%.1f km", distToStepEnd / 1000.0)
            } else {
                "${distToStepEnd.toInt()} m"
            }
            "In $distFormatted, ${currentStep.instruction}"
        }

        val maneuver = ManeuverInstruction(
            stepIndex = stepIdx,
            instruction = instructionText,
            turnType = turnType,
            distanceToManeuverMeters = distToStepEnd,
            streetName = currentStep.streetName
        )

        val updatedTelemetry = NavigationTelemetry(
            riderId = location.riderId.ifEmpty { riderId },
            currentSpeedKmh = currentSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            distanceTraveledMeters = totalDistanceTraveledMeters,
            batteryLevel = 100,
            isNavigating = !isReached,
            isRideActive = !isReached,
            currentRouteStepIndex = stepIdx,
            etaSeconds = etaSeconds,
            remainingDistanceMeters = remainingDistanceMeters,
            rideDurationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis()
        )

        _state.value = TurnByTurnState(
            isNavigating = !isReached,
            isRideActive = !isReached,
            currentRoute = route,
            currentStepIndex = stepIdx,
            currentManeuver = maneuver,
            telemetry = updatedTelemetry,
            isDestinationReached = isReached
        )

        return updatedTelemetry
    }

    companion object {
        fun calculateDistanceMeters(
            startLat: Double, startLng: Double,
            endLat: Double, endLng: Double
        ): Double {
            try {
                val results = FloatArray(1)
                Location.distanceBetween(startLat, startLng, endLat, endLng, results)
                if (results[0] > 0f) {
                    return results[0].toDouble()
                }
            } catch (_: Throwable) {
                // Fallback to Haversine formula for JVM test environment
            }

            val earthRadiusMeters = 6371000.0
            val dLat = Math.toRadians(endLat - startLat)
            val dLng = Math.toRadians(endLng - startLng)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                    sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusMeters * c
        }

        fun calculateBearing(
            startLat: Double, startLng: Double,
            endLat: Double, endLng: Double
        ): Float {
            val startLatRad = Math.toRadians(startLat)
            val endLatRad = Math.toRadians(endLat)
            val dLngRad = Math.toRadians(endLng - startLng)

            val y = sin(dLngRad) * cos(endLatRad)
            val x = cos(startLatRad) * sin(endLatRad) - sin(startLatRad) * cos(endLatRad) * cos(dLngRad)
            val bearingRad = atan2(y, x)
            return ((Math.toDegrees(bearingRad) + 360) % 360).toFloat()
        }
    }
}
