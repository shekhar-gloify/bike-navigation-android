package com.dopetechindia.bikenavigation.data.model

import kotlinx.serialization.Serializable

/**
 * Authenticated rider user profile model.
 */
@Serializable
data class AuthUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val idToken: String? = null
)

/**
 * Represents rider authentication state across the application.
 */
sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Authenticating : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data class Error(val message: String) : AuthState
}
