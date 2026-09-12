package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Navigation telemetry metrics recorded during an active ride session.
 */
@Serializable
data class NavigationTelemetry(
    val riderId: String = "",
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val distanceTraveledMeters: Double = 0.0,
    val batteryLevel: Int = 100,
    val isNavigating: Boolean = false,
    val isRideActive: Boolean = false,
    val currentRouteStepIndex: Int? = null,
    val etaSeconds: Long? = null,
    val remainingDistanceMeters: Double? = null,
    val rideDurationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
