package com.dopetechindia.bikenavigation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dopetechindia.bikenavigation.ui.auth.AuthScreen
import com.dopetechindia.bikenavigation.ui.auth.AuthViewModel
import com.dopetechindia.bikenavigation.ui.dashboard.RideDashboardScreen
import com.dopetechindia.bikenavigation.ui.dashboard.RideDashboardViewModel
import com.dopetechindia.bikenavigation.ui.group.GroupSharingSheet
import com.dopetechindia.bikenavigation.ui.group.GroupViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppNavKey : NavKey

@Serializable
data object AuthRoute : AppNavKey

@Serializable
data object DashboardRoute : AppNavKey

@Serializable
data object GroupSharingRoute : AppNavKey

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    dashboardViewModel: RideDashboardViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    // App defaults to Local Guest Startup on Main Ride Dashboard, bypassing sign-in requirement
    val initialRoute: AppNavKey = DashboardRoute

    val backStack = remember { NavBackStack<AppNavKey>(initialRoute) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AuthRoute> {
                AuthScreen(
                    onAuthSuccess = {
                        backStack.remove(AuthRoute)
                        if (!backStack.contains(DashboardRoute)) {
                            backStack.add(DashboardRoute)
                        }
                    },
                    onBackToGuest = {
                        backStack.remove(AuthRoute)
                        if (!backStack.contains(DashboardRoute)) {
                            backStack.add(DashboardRoute)
                        }
                    },
                    viewModel = authViewModel
                )
            }
            entry<DashboardRoute> {
                RideDashboardScreen(
                    onNavigateToGroupSharing = {
                        backStack.add(GroupSharingRoute)
                    },
                    onNavigateToAuth = {
                        if (!backStack.contains(AuthRoute)) {
                            backStack.add(AuthRoute)
                        }
                    },
                    onSignOut = {
                        authViewModel.signOut()
                    },
                    dashboardViewModel = dashboardViewModel,
                    groupViewModel = groupViewModel
                )
            }
            entry<GroupSharingRoute> {
                GroupSharingSheet(
                    onDismiss = { backStack.remove(GroupSharingRoute) },
                    viewModel = groupViewModel
                )
            }
        }
    )
}
