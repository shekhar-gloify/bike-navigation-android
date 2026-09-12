package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Enumeration of maneuver turn types for navigation guidance.
 */
@Serializable
enum class TurnType {
    START,
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    U_TURN,
    ARRIVED
}

/**
 * Represents an individual maneuver step within a navigation route.
 */
@Serializable
data class RouteStep(
    val stepIndex: Int,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double,
    val instruction: String,
    val distanceMeters: Double,
    val turnType: TurnType,
    val streetName: String = ""
)

/**
 * Represents a complete turn-by-turn navigation route.
 */
@Serializable
data class NavigationRoute(
    val routeId: String,
    val title: String,
    val steps: List<RouteStep>,
    val polylinePoints: List<Pair<Double, Double>>,
    val totalDistanceMeters: Double,
    val estimatedDurationSeconds: Long
)
