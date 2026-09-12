package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Data model representing an active group ride sharing session.
 */
@Serializable
data class GroupSession(
    val groupId: String = "",
    val groupName: String = "",
    val groupCode: String = "",
    val hostRiderId: String = "",
    val memberIds: List<String> = emptyList(),
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val destinationName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
