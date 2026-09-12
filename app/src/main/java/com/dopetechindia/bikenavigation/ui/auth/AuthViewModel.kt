package com.dopetechindia.bikenavigation.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopetechindia.bikenavigation.data.auth.AuthRepository
import com.dopetechindia.bikenavigation.data.di.RepositoryProvider
import com.dopetechindia.bikenavigation.data.model.AuthState
import com.dopetechindia.bikenavigation.data.model.AuthUser
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel handling rider authentication operations (Google Sign-In & Guest Rider Mode).
 */
class AuthViewModel(
    private val authRepository: AuthRepository = RepositoryProvider.authRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    /**
     * Initiates Google Sign-In authentication flow.
     */
    fun signInWithGoogle(context: Context, webClientId: String = "YOUR_WEB_CLIENT_ID_PLACEHOLDER") {
        viewModelScope.launch {
            authRepository.signInWithGoogle(context, webClientId)
        }
    }

    /**
     * Initiates Guest Rider anonymous sign-in session.
     */
    fun signInAnonymously() {
        viewModelScope.launch {
            authRepository.signInAnonymously()
        }
    }

    /**
     * Signs out active rider session.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    /**
     * Returns current active [AuthUser] instance, if authenticated.
     */
    fun getCurrentUser(): AuthUser? {
        return authRepository.getCurrentUser()
    }
}
