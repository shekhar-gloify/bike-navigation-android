package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Data model representing real-time GPS location and speed telemetry for a rider.
 */
@Serializable
data class LocationData(
    val riderId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double? = null,
    val speedMps: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts speed from meters per second (m/s) to kilometers per hour (km/h).
     */
    val speedKmh: Float
        get() = speedMps * 3.6f
}
