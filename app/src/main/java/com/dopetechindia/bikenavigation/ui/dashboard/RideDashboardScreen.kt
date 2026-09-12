package com.dopetechindia.bikenavigation.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SubdirectoryArrowLeft
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dopetechindia.bikenavigation.data.di.RepositoryProvider
import com.dopetechindia.bikenavigation.data.model.GroupSession
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.NavigationRoute
import com.dopetechindia.bikenavigation.data.model.NavigationTelemetry
import com.dopetechindia.bikenavigation.data.model.SampleRoutes
import com.dopetechindia.bikenavigation.data.model.SavedPlace
import com.dopetechindia.bikenavigation.data.model.TurnType
import com.dopetechindia.bikenavigation.location.LocationService
import com.dopetechindia.bikenavigation.navigation.ManeuverInstruction
import com.dopetechindia.bikenavigation.navigation.TurnByTurnState
import com.dopetechindia.bikenavigation.ui.group.GroupMemberUiState
import com.dopetechindia.bikenavigation.ui.group.GroupSharingSheet
import com.dopetechindia.bikenavigation.ui.group.GroupViewModel
import com.dopetechindia.bikenavigation.ui.theme.BikeNavigationTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main Ride Dashboard Screen rendering the interactive Google Maps view, floating place search bar,
 * turn-by-turn maneuver banner, floating map HUD controls, location details sheet, and starred locations sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDashboardScreen(
    onNavigateToGroupSharing: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAuth: () -> Unit = {},
    dashboardViewModel: RideDashboardViewModel = viewModel(),
    groupViewModel: GroupViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentLocation by dashboardViewModel.currentLocation.collectAsState()
    val isServiceRunning by dashboardViewModel.isServiceRunning.collectAsState()
    val turnByTurnState by dashboardViewModel.turnByTurnState.collectAsState()
    val groupMembers by groupViewModel.groupMembersList.collectAsState()
    val currentGroup by groupViewModel.currentGroup.collectAsState()
    val savedPlaces by dashboardViewModel.savedPlaces.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(checkLocationPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
    }

    val defaultLatLng = LatLng(
        currentLocation?.latitude ?: 37.7749,
        currentLocation?.longitude ?: -122.4194
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 16f)
    }

    var isCameraFollowingUser by remember { mutableStateOf(true) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            dashboardViewModel.ensureGpsServiceStarted(context)
        }
    }

    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null && isCameraFollowingUser) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)
            )
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BikeNavigationTheme(
        darkTheme = dashboardViewModel.isHighContrastMode,
        highContrastMode = dashboardViewModel.isHighContrastMode,
        dynamicColor = false
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            if (!hasLocationPermission) {
                PermissionOverlay(
                    onRequestPermissions = {
                        val perms = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                )
            } else {
                DashboardMapView(
                    cameraPositionState = cameraPositionState,
                    currentLocation = currentLocation,
                    turnByTurnState = turnByTurnState,
                    groupMembers = groupMembers,
                    isHighContrast = dashboardViewModel.isHighContrastMode,
                    selectedPlace = dashboardViewModel.selectedPlace,
                    onUserMapInteracted = { isCameraFollowingUser = false }
                )

                // Top Area: Floating Search Bar, Optional Profile Button & Maneuver Banner
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Floating Place Search Bar
                        FloatingPlaceSearchBar(
                            query = dashboardViewModel.searchQuery,
                            onQueryChange = { dashboardViewModel.onSearchQueryChanged(it, context) },
                            onClearQuery = { dashboardViewModel.clearSearchQuery() },
                            searchResults = dashboardViewModel.searchResults,
                            onSelectPlace = { place ->
                                dashboardViewModel.selectPlace(place)
                                isCameraFollowingUser = false
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(place.latitude, place.longitude),
                                            16f
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Optional Sign In / Profile Button (never blocks local guest mode)
                        Surface(
                            onClick = {
                                if (!dashboardViewModel.isUserAuthenticated) {
                                    onNavigateToAuth()
                                } else {
                                    dashboardViewModel.showProfileDialog = true
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (dashboardViewModel.isUserAuthenticated) Icons.Rounded.Person else Icons.Rounded.AccountCircle,
                                    contentDescription = if (dashboardViewModel.isUserAuthenticated) "Rider Profile" else "Optional Sign In",
                                    tint = if (dashboardViewModel.isUserAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // High-Contrast Turn-by-Turn Maneuver Guidance Banner
                    AnimatedVisibility(
                        visible = turnByTurnState.isNavigating && turnByTurnState.currentManeuver != null,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        turnByTurnState.currentManeuver?.let { maneuver ->
                            HighContrastManeuverBanner(maneuver = maneuver)
                        }
                    }
                }

                // Map Control Buttons (Floating Right Stack)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Group Sharing Button with Active Rider Count Badge
                    FloatingActionButton(
                        onClick = { dashboardViewModel.onGroupSharingClicked() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        BadgedBox(
                            badge = {
                                if (groupMembers.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${groupMembers.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Groups,
                                contentDescription = "Group Sharing"
                            )
                        }
                    }

                    // Starred Places FAB
                    FloatingActionButton(
                        onClick = { dashboardViewModel.openStarredPlacesSheet() },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = Color(0xFFFFB300)
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedPlaces.isNotEmpty()) {
                                    Badge(
                                        containerColor = Color(0xFFFFB300),
                                        contentColor = Color.Black
                                    ) {
                                        Text("${savedPlaces.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Starred Places"
                            )
                        }
                    }

                    // Theme Toggle (High Contrast Dark / Clean Light Mode)
                    FloatingActionButton(
                        onClick = { dashboardViewModel.toggleHighContrast() },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            imageVector = if (dashboardViewModel.isHighContrastMode) Icons.Rounded.LightMode else Icons.Rounded.Nightlight,
                            contentDescription = "Toggle Theme"
                        )
                    }

                    // Recenter Camera / GPS Button
                    FloatingActionButton(
                        onClick = {
                            if (!hasLocationPermission) {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            } else {
                                isCameraFollowingUser = true
                                val loc = currentLocation
                                    ?: RepositoryProvider.locationRepository.currentLocation.value
                                    ?: LocationService.turnByTurnEngine.state.value.currentRoute?.polylinePoints?.firstOrNull()?.let {
                                        LocationData(riderId = "local_user", latitude = it.first, longitude = it.second)
                                    }

                                loc?.let { targetLoc ->
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                LatLng(targetLoc.latitude, targetLoc.longitude),
                                                16f
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (isCameraFollowingUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ) {
                        Icon(
                            imageVector = if (isCameraFollowingUser) Icons.Rounded.GpsFixed else Icons.Rounded.MyLocation,
                            contentDescription = "Recenter GPS Camera"
                        )
                    }
                }

                // Ride Dashboard HUD Card (Bottom)
                HighContrastRideDashboardHUD(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    currentLocation = currentLocation,
                    isServiceRunning = isServiceRunning,
                    turnByTurnState = turnByTurnState,
                    activeGroup = currentGroup,
                    onToggleRide = { route ->
                        dashboardViewModel.toggleRide(context, route)
                    },
                    onOpenGroupSheet = { dashboardViewModel.onGroupSharingClicked() }
                )

                // Location Details Bottom Sheet Modal
                if (dashboardViewModel.showLocationDetailsSheet && dashboardViewModel.selectedPlace != null) {
                    val place = dashboardViewModel.selectedPlace!!
                    ModalBottomSheet(
                        onDismissRequest = { dashboardViewModel.dismissLocationDetails() },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        LocationDetailsSheet(
                            place = place,
                            currentLocation = currentLocation,
                            isSaved = dashboardViewModel.isPlaceSaved(place.id),
                            onToggleStar = { dashboardViewModel.toggleStarPlace(place) },
                            onNavigate = { dashboardViewModel.navigateToPlace(context, place) },
                            onDismiss = { dashboardViewModel.dismissLocationDetails() }
                        )
                    }
                }

                // Starred Locations Bottom Sheet Modal
                if (dashboardViewModel.showStarredPlacesSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { dashboardViewModel.dismissStarredPlacesSheet() },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        StarredPlacesSheet(
                            savedPlaces = savedPlaces,
                            onPlaceClick = { place ->
                                dashboardViewModel.selectPlace(place)
                                isCameraFollowingUser = false
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(place.latitude, place.longitude),
                                            16f
                                        )
                                    )
                                }
                            },
                            onNavigateToPlace = { place ->
                                dashboardViewModel.navigateToPlace(context, place)
                            },
                            onDeletePlace = { placeId ->
                                dashboardViewModel.deleteSavedPlace(placeId)
                            },
                            onDismiss = { dashboardViewModel.dismissStarredPlacesSheet() }
                        )
                    }
                }

                // Rider Profile Dialog (For optional authenticated rider status check & sign out)
                if (dashboardViewModel.showProfileDialog) {
                    val user = dashboardViewModel.currentUser
                    AlertDialog(
                        onDismissRequest = { dashboardViewModel.showProfileDialog = false },
                        title = { Text("Rider Profile", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text(
                                    text = "Signed in as ${user?.displayName ?: "Rider"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!user?.email.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Google Authenticated Profile",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    dashboardViewModel.showProfileDialog = false
                                    onSignOut()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Sign Out")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { dashboardViewModel.showProfileDialog = false }
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }

                // Group Sharing Bottom Sheet Modal
                if (dashboardViewModel.showGroupSharingSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { dashboardViewModel.showGroupSharingSheet = false },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        GroupSharingSheet(
                            onDismiss = { dashboardViewModel.showGroupSharingSheet = false },
                            viewModel = groupViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingPlaceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    searchResults: List<SavedPlace>,
    onSelectPlace: (SavedPlace) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Search places, cafes, lookouts...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearQuery) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Suggestions Dropdown Overlay
        if (searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(searchResults, key = { it.id }) { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPlace(place) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${place.category} • ${place.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", place.rating),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardMapView(
    cameraPositionState: CameraPositionState,
    currentLocation: LocationData?,
    turnByTurnState: TurnByTurnState,
    groupMembers: List<GroupMemberUiState>,
    isHighContrast: Boolean,
    selectedPlace: SavedPlace?,
    onUserMapInteracted: () -> Unit
) {
    val mapProperties = remember(isHighContrast) {
        MapProperties(
            isMyLocationEnabled = true,
            isBuildingEnabled = true,
            mapStyleOptions = if (isHighContrast) MapStyleOptions(NIGHT_MAP_JSON) else null
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapClick = { onUserMapInteracted() }
    ) {
        val route = turnByTurnState.currentRoute
        if (route != null) {
            val polylinePoints = route.polylinePoints.map { LatLng(it.first, it.second) }
            if (polylinePoints.isNotEmpty()) {
                Polyline(
                    points = polylinePoints,
                    color = MaterialTheme.colorScheme.primary,
                    width = 16f
                )

                val startPoint = polylinePoints.first()
                Marker(
                    state = remember(startPoint) { MarkerState(position = startPoint) },
                    title = "Start: ${route.title}",
                    snippet = "Start Location"
                )

                val destinationPoint = polylinePoints.last()
                Marker(
                    state = remember(destinationPoint) { MarkerState(position = destinationPoint) },
                    title = "Destination: ${route.title}",
                    snippet = "Route End"
                )
            }
        }

        selectedPlace?.let { place ->
            val placeLatLng = LatLng(place.latitude, place.longitude)
            Marker(
                state = remember(placeLatLng) { MarkerState(position = placeLatLng) },
                title = place.name,
                snippet = "${place.category} • ${place.address}",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }

        groupMembers.forEach { member ->
            if (!member.isCurrentUser && member.latitude != 0.0 && member.longitude != 0.0) {
                val memberLatLng = LatLng(member.latitude, member.longitude)
                Marker(
                    state = remember(memberLatLng) { MarkerState(position = memberLatLng) },
                    title = "${member.displayName} (${String.format(Locale.US, "%.1f", member.speedKmh)} km/h)",
                    snippet = "Distance: ${formatDistanceMeters(member.distanceFromUserMeters)}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (member.isHost) BitmapDescriptorFactory.HUE_YELLOW else BitmapDescriptorFactory.HUE_CYAN
                    )
                )
            }
        }
    }
}

@Composable
fun HighContrastManeuverBanner(
    maneuver: ManeuverInstruction,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getTurnIcon(maneuver.turnType),
                        contentDescription = "Turn Direction",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = maneuver.instruction,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (maneuver.streetName.isNotEmpty()) {
                    Text(
                        text = maneuver.streetName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun HighContrastRideDashboardHUD(
    modifier: Modifier = Modifier,
    currentLocation: LocationData?,
    isServiceRunning: Boolean,
    turnByTurnState: TurnByTurnState,
    activeGroup: GroupSession?,
    onToggleRide: (NavigationRoute?) -> Unit,
    onOpenGroupSheet: () -> Unit
) {
    var selectedRoute by remember { mutableStateOf<NavigationRoute?>(null) }
    var showRoutePicker by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            shape = RoundedCornerShape(28.dp)
        ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Speed Gauge & Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "Speed Gauge",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = String.format(Locale.US, "%.0f", currentLocation?.speedKmh ?: 0f),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 48.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " KM/H",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (isServiceRunning) Color(0xFF00E676) else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServiceRunning) "GPS TRACKING" else "GPS IDLE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isServiceRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activeGroup != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable { onOpenGroupSheet() }
                        ) {
                            Text(
                                text = "Group: ${activeGroup.groupCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Navigation Telemetry Stats
            if (turnByTurnState.isRideActive) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (turnByTurnState.currentRoute != null) {
                        TelemetryStatItem(
                            icon = Icons.Rounded.Route,
                            label = "REMAINING",
                            value = formatDistanceMeters(turnByTurnState.telemetry.remainingDistanceMeters ?: 0.0)
                        )
                        TelemetryStatItem(
                            icon = Icons.Rounded.Timer,
                            label = "ETA",
                            value = formatEtaSeconds(turnByTurnState.telemetry.etaSeconds ?: 0L)
                        )
                    } else {
                        TelemetryStatItem(
                            icon = Icons.Rounded.Timer,
                            label = "DURATION",
                            value = formatEtaSeconds(turnByTurnState.telemetry.rideDurationSeconds)
                        )
                        TelemetryStatItem(
                            icon = Icons.Rounded.Route,
                            label = "DISTANCE",
                            value = formatDistanceMeters(turnByTurnState.telemetry.distanceTraveledMeters)
                        )
                    }
                    TelemetryStatItem(
                        icon = Icons.AutoMirrored.Rounded.DirectionsBike,
                        label = "AVG SPEED",
                        value = String.format(Locale.US, "%.1f km/h", turnByTurnState.telemetry.avgSpeedKmh)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Route Selection Bar (When Ride Idle)
            if (!turnByTurnState.isRideActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRoutePicker = !showRoutePicker }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedRoute?.title ?: "Free Ride (No Destination Route)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (showRoutePicker) "Hide" else "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Route Picker Dropdown
            if (showRoutePicker && !turnByTurnState.isRideActive) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Select Optional Route",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FilledTonalButton(
                            onClick = {
                                selectedRoute = null
                                showRoutePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Free Ride (No Destination Route)")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = {
                                selectedRoute = SampleRoutes.cityScenicLoop
                                showRoutePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("City Scenic Loop (5.2 km)")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = {
                                selectedRoute = SampleRoutes.mountainTrail
                                showRoutePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Twin Peaks Trail (8.4 km)")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Single Prominent Action Button: "Start Ride" / "End Ride"
            Button(
                onClick = {
                    if (turnByTurnState.isRideActive) {
                        onToggleRide(null)
                    } else {
                        onToggleRide(selectedRoute)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (turnByTurnState.isRideActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (turnByTurnState.isRideActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (turnByTurnState.isRideActive) "End Ride" else "Start Ride",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TelemetryStatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PermissionOverlay(
    onRequestPermissions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Location Access",
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Location Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bike Navigation needs location permissions to provide live turn-by-turn guidance and real-time group rider overlay.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Grant Location Permissions")
                }
            }
        }
    }
}

private fun getTurnIcon(turnType: TurnType): ImageVector {
    return when (turnType) {
        TurnType.START -> Icons.Rounded.Navigation
        TurnType.STRAIGHT -> Icons.Rounded.Navigation
        TurnType.TURN_LEFT -> Icons.Rounded.SubdirectoryArrowLeft
        TurnType.TURN_RIGHT -> Icons.Rounded.SubdirectoryArrowRight
        TurnType.SLIGHT_LEFT -> Icons.Rounded.SubdirectoryArrowLeft
        TurnType.SLIGHT_RIGHT -> Icons.Rounded.SubdirectoryArrowRight
        TurnType.U_TURN -> Icons.AutoMirrored.Rounded.ArrowBack
        TurnType.ARRIVED -> Icons.Rounded.Flag
    }
}

private fun formatDistanceMeters(meters: Double): String {
    return if (meters >= 1000) {
        String.format(Locale.US, "%.1f km", meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}

private fun formatEtaSeconds(seconds: Long): String {
    val mins = seconds / 60
    return if (mins >= 60) {
        val hrs = mins / 60
        val remainingMins = mins % 60
        "${hrs}h ${remainingMins}m"
    } else {
        "${mins} mins"
    }
}

private fun checkLocationPermissions(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

private const val NIGHT_MAP_JSON = """
[
  {
    "elementType": "geometry",
    "stylers": [{"color": "#242f3e"}]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#746855"}]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [{"color": "#242f3e"}]
  },
  {
    "featureType": "administrative.locality",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#d59563"}]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#d59563"}]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [{"color": "#263c3f"}]
  },
  {
    "featureType": "poi.park",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#6b9a76"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [{"color": "#38414e"}]
  },
  {
    "featureType": "road",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#212a37"}]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#9ca5b3"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [{"color": "#746855"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry.stroke",
    "stylers": [{"color": "#1f2835"}]
  },
  {
    "featureType": "road.highway",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#f3d19c"}]
  },
  {
    "featureType": "transit",
    "elementType": "geometry",
    "stylers": [{"color": "#2f3948"}]
  },
  {
    "featureType": "transit.station",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#d59563"}]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [{"color": "#17263c"}]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#515c6d"}]
  },
  {
    "featureType": "water",
    "elementType": "labels.text.stroke",
    "stylers": [{"color": "#17263c"}]
  }
]
"""

@Preview(showBackground = true)
@Composable
fun HighContrastManeuverBannerPreview() {
    BikeNavigationTheme(highContrastMode = true) {
        HighContrastManeuverBanner(
            maneuver = ManeuverInstruction(
                stepIndex = 1,
                instruction = "In 150 m, Turn Right onto Grand Ave",
                turnType = TurnType.TURN_RIGHT,
                distanceToManeuverMeters = 150.0,
                streetName = "Grand Ave"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HighContrastRideDashboardHUDPreview() {
    BikeNavigationTheme(highContrastMode = true) {
        HighContrastRideDashboardHUD(
            currentLocation = LocationData(
                speedMps = 15.2f,
                latitude = 37.7749,
                longitude = -122.4194
            ),
            isServiceRunning = true,
            turnByTurnState = TurnByTurnState(
                isNavigating = true,
                telemetry = NavigationTelemetry(
                    currentSpeedKmh = 54.7f,
                    avgSpeedKmh = 42.1f,
                    remainingDistanceMeters = 3800.0,
                    etaSeconds = 480
                )
            ),
            activeGroup = null,
            onToggleRide = {},
            onOpenGroupSheet = {}
        )
    }
}
