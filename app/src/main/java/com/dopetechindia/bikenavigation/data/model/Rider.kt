package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Status of a group rider.
 */
enum class RiderStatus {
    ONLINE,
    RIDING,
    IDLE,
    OFFLINE
}

/**
 * Model representing a registered rider member.
 */
@Serializable
data class Rider(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val status: RiderStatus = RiderStatus.ONLINE,
    val currentGroupId: String? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
