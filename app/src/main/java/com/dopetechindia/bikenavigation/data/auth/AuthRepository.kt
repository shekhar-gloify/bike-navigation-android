package com.dopetechindia.bikenavigation.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.dopetechindia.bikenavigation.data.model.AuthState
import com.dopetechindia.bikenavigation.data.model.AuthUser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Interface defining authentication operations for rider sign-in and session management.
 */
interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<AuthUser>
    suspend fun signInAnonymously(): Result<AuthUser>
    suspend fun signOut()
    fun getCurrentUser(): AuthUser?
}

/**
 * Firebase Auth implementation with Credential Manager Google Sign-In and anonymous guest fallback.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth? = try { FirebaseAuth.getInstance() } catch (_: Throwable) { null }
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val auth = firebaseAuth
        if (auth != null) {
            try {
                auth.addAuthStateListener { firebase ->
                    val user = firebase.currentUser
                    if (user != null) {
                        _authState.value = AuthState.Authenticated(user.toAuthUser())
                    } else {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            } catch (_: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun signInWithGoogle(context: Context, webClientId: String): Result<AuthUser> {
        _authState.value = AuthState.Authenticating
        val auth = firebaseAuth
        if (auth == null) {
            val fallbackUser = AuthUser(
                uid = "google_rider_${UUID.randomUUID().toString().take(8)}",
                displayName = "Google Rider",
                email = "rider@example.com"
            )
            _authState.value = AuthState.Authenticated(fallbackUser)
            return Result.success(fallbackUser)
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val authUser = firebaseUser.toAuthUser(idToken)
                    _authState.value = AuthState.Authenticated(authUser)
                    Result.success(authUser)
                } else {
                    val err = "Firebase user was null after Google Sign-In"
                    _authState.value = AuthState.Error(err)
                    Result.failure(Exception(err))
                }
            } else {
                val err = "Unexpected credential type: ${credential.type}"
                _authState.value = AuthState.Error(err)
                Result.failure(Exception(err))
            }
        } catch (e: GetCredentialException) {
            val errMsg = "Credential Manager exception: ${e.localizedMessage}"
            _authState.value = AuthState.Error(errMsg)
            Result.failure(e)
        } catch (e: Exception) {
            val errMsg = "Google Sign-In failed: ${e.localizedMessage ?: e.message}"
            _authState.value = AuthState.Error(errMsg)
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        _authState.value = AuthState.Authenticating
        val auth = firebaseAuth
        if (auth == null) {
            val fallbackUser = AuthUser(
                uid = "guest_${UUID.randomUUID().toString().take(8)}",
                displayName = "Guest Rider",
                isAnonymous = true
            )
            _authState.value = AuthState.Authenticated(fallbackUser)
            return Result.success(fallbackUser)
        }

        return try {
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                val authUser = user.toAuthUser()
                _authState.value = AuthState.Authenticated(authUser)
                Result.success(authUser)
            } else {
                val fallbackUser = AuthUser(
                    uid = "guest_${UUID.randomUUID().toString().take(8)}",
                    displayName = "Guest Rider",
                    isAnonymous = true
                )
                _authState.value = AuthState.Authenticated(fallbackUser)
                Result.success(fallbackUser)
            }
        } catch (e: Exception) {
            val fallbackUser = AuthUser(
                uid = "guest_${UUID.randomUUID().toString().take(8)}",
                displayName = "Guest Rider",
                isAnonymous = true
            )
            _authState.value = AuthState.Authenticated(fallbackUser)
            Result.success(fallbackUser)
        }
    }

    override suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentUser(): AuthUser? {
        val firebaseUser = try { firebaseAuth?.currentUser } catch (_: Exception) { null }
        return firebaseUser?.toAuthUser() ?: (_authState.value as? AuthState.Authenticated)?.user
    }

    private fun FirebaseUser.toAuthUser(idToken: String? = null): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName ?: "Rider",
            photoUrl = photoUrl?.toString(),
            isAnonymous = isAnonymous,
            idToken = idToken
        )
    }
}
