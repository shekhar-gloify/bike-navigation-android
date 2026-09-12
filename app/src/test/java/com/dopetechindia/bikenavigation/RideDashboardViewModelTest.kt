package com.dopetechindia.bikenavigation

import android.content.Context
import com.dopetechindia.bikenavigation.data.auth.AuthRepository
import com.dopetechindia.bikenavigation.data.model.AuthState
import com.dopetechindia.bikenavigation.data.model.AuthUser
import com.dopetechindia.bikenavigation.ui.dashboard.RideDashboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeTestAuthRepository(
    initialState: AuthState = AuthState.Unauthenticated
) : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(initialState)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signInWithGoogle(context: Context, webClientId: String): Result<AuthUser> {
        val user = AuthUser(uid = "google_user", isAnonymous = false)
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }

    override suspend fun signInAnonymously(): Result<AuthUser> {
        val user = AuthUser(uid = "guest_user", isAnonymous = true)
        _authState.value = AuthState.Authenticated(user)
        return Result.success(user)
    }

    override suspend fun signOut() {
        _authState.value = AuthState.Unauthenticated
    }

    override fun getCurrentUser(): AuthUser? {
        return (_authState.value as? AuthState.Authenticated)?.user
    }
}

class RideDashboardViewModelTest {

    @Test
    fun testHighContrastModeToggle() {
        val viewModel = RideDashboardViewModel()
        assertTrue(viewModel.isHighContrastMode)

        viewModel.toggleHighContrast()
        assertFalse(viewModel.isHighContrastMode)

        viewModel.toggleHighContrast()
        assertTrue(viewModel.isHighContrastMode)
    }

    @Test
    fun testGroupSharingInUnauthenticatedGuestMode() {
        val fakeAuthRepo = FakeTestAuthRepository(AuthState.Unauthenticated)
        val viewModel = RideDashboardViewModel(authRepository = fakeAuthRepo)

        assertFalse(viewModel.isUserAuthenticated)
        viewModel.onGroupSharingClicked()

        assertFalse(viewModel.showSignInDialog)
        assertTrue(viewModel.showGroupSharingSheet)
    }

    @Test
    fun testGroupSharingInAnonymousGuestMode() {
        val guestUser = AuthUser(uid = "guest123", isAnonymous = true)
        val fakeAuthRepo = FakeTestAuthRepository(AuthState.Authenticated(guestUser))
        val viewModel = RideDashboardViewModel(authRepository = fakeAuthRepo)

        assertFalse(viewModel.isUserAuthenticated)
        viewModel.onGroupSharingClicked()

        assertFalse(viewModel.showSignInDialog)
        assertTrue(viewModel.showGroupSharingSheet)
    }

    @Test
    fun testGroupSharingAuthenticatedUserAccess() {
        val loggedInUser = AuthUser(uid = "rider123", email = "rider@example.com", isAnonymous = false)
        val fakeAuthRepo = FakeTestAuthRepository(AuthState.Authenticated(loggedInUser))
        val viewModel = RideDashboardViewModel(authRepository = fakeAuthRepo)

        assertTrue(viewModel.isUserAuthenticated)
        viewModel.onGroupSharingClicked()

        assertFalse(viewModel.showSignInDialog)
        assertTrue(viewModel.showGroupSharingSheet)
    }
}
