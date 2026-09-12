package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Data model representing a saved or searched point of interest location.
 */
@Serializable
data class SavedPlace(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 4.5,
    val photoUrls: List<String> = emptyList(),
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)
