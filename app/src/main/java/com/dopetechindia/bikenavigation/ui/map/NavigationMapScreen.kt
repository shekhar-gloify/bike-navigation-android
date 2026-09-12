package com.dopetechindia.bikenavigation.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SubdirectoryArrowLeft
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.dopetechindia.bikenavigation.data.di.RepositoryProvider
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.NavigationRoute
import com.dopetechindia.bikenavigation.data.model.NavigationTelemetry
import com.dopetechindia.bikenavigation.data.model.SampleRoutes
import com.dopetechindia.bikenavigation.data.model.TurnType
import com.dopetechindia.bikenavigation.location.LocationService
import com.dopetechindia.bikenavigation.navigation.ManeuverInstruction
import com.dopetechindia.bikenavigation.navigation.TurnByTurnState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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
 * Dedicated Standalone Navigation Map Screen rendering live turn-by-turn guidance and route polyline.
 */
@Composable
fun NavigationMapScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentLocation by RepositoryProvider.locationRepository.currentLocation.collectAsState()
    val isServiceRunning by LocationService.isRunning.collectAsState()
    val turnByTurnState by LocationService.turnByTurnEngine.state.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(checkLocationPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
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
        if (hasLocationPermission && !isServiceRunning) {
            LocationService.start(context)
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

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            PermissionRequestOverlay(
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
            MapViewContent(
                cameraPositionState = cameraPositionState,
                currentLocation = currentLocation,
                turnByTurnState = turnByTurnState,
                onUserMapInteracted = { isCameraFollowingUser = false }
            )

            // Top Maneuver Guidance Banner
            AnimatedVisibility(
                visible = turnByTurnState.isNavigating && turnByTurnState.currentManeuver != null,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                turnByTurnState.currentManeuver?.let { maneuver ->
                    ManeuverBannerCard(maneuver = maneuver)
                }
            }

            // Map Control Buttons (Right Floating)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        isCameraFollowingUser = true
                        currentLocation?.let { loc ->
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude),
                                        16f
                                    )
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (isCameraFollowingUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = if (isCameraFollowingUser) Icons.Rounded.GpsFixed else Icons.Rounded.MyLocation,
                        contentDescription = "Recenter Camera"
                    )
                }
            }

            // Bottom Telemetry HUD Card
            TelemetryHudCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                currentLocation = currentLocation,
                isServiceRunning = isServiceRunning,
                turnByTurnState = turnByTurnState,
                onToggleRide = { route ->
                    if (turnByTurnState.isRideActive) {
                        LocationService.turnByTurnEngine.stopRide()
                    } else {
                        val routeToStart = route ?: SampleRoutes.cityScenicLoop
                        LocationService.turnByTurnEngine.startRide(routeToStart)
                        if (!isServiceRunning) {
                            LocationService.start(context)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MapViewContent(
    cameraPositionState: CameraPositionState,
    currentLocation: LocationData?,
    turnByTurnState: TurnByTurnState,
    onUserMapInteracted: () -> Unit
) {
    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = true,
            isBuildingEnabled = true
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
                    width = 14f
                )

                val startPoint = polylinePoints.first()
                Marker(
                    state = MarkerState(position = startPoint),
                    title = "Start: ${route.title}",
                    snippet = "Start Location"
                )

                val destinationPoint = polylinePoints.last()
                Marker(
                    state = MarkerState(position = destinationPoint),
                    title = "Destination",
                    snippet = route.title
                )

                route.steps.forEachIndexed { idx, step ->
                    if (step.turnType != TurnType.STRAIGHT && step.turnType != TurnType.ARRIVED) {
                        Marker(
                            state = MarkerState(position = LatLng(step.endLatitude, step.endLongitude)),
                            title = "Step ${idx + 1}: ${step.turnType.name}",
                            snippet = step.instruction
                        )
                    }
                }
            }
        }

        currentLocation?.let { loc ->
            Marker(
                state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                title = "Current Rider Position",
                snippet = "Speed: %.1f km/h".format(loc.speedKmh)
            )
        }
    }
}

@Composable
fun ManeuverBannerCard(
    maneuver: ManeuverInstruction,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getTurnIcon(maneuver.turnType),
                        contentDescription = "Turn Direction",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = maneuver.instruction,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (maneuver.streetName.isNotEmpty()) {
                    Text(
                        text = maneuver.streetName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryHudCard(
    modifier: Modifier = Modifier,
    currentLocation: LocationData?,
    isServiceRunning: Boolean,
    turnByTurnState: TurnByTurnState,
    onToggleRide: (NavigationRoute?) -> Unit
) {
    var showRoutePicker by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "Speed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", currentLocation?.speedKmh ?: 0f),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " km/h",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

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
                                .size(8.dp)
                                .background(
                                    color = if (isServiceRunning) Color(0xFF4CAF50) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isServiceRunning) "GPS Tracking Active" else "GPS Idle",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (turnByTurnState.isRideActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TelemetryStatItem(
                        icon = Icons.Rounded.Route,
                        label = "Remaining",
                        value = formatDistance(turnByTurnState.telemetry.remainingDistanceMeters ?: 0.0)
                    )
                    TelemetryStatItem(
                        icon = Icons.Rounded.Timer,
                        label = "ETA",
                        value = formatEta(turnByTurnState.telemetry.etaSeconds ?: 0L)
                    )
                    TelemetryStatItem(
                        icon = Icons.AutoMirrored.Rounded.DirectionsBike,
                        label = "Avg Speed",
                        value = String.format(Locale.US, "%.1f km/h", turnByTurnState.telemetry.avgSpeedKmh)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Single Prominent Action Button ("Start Ride" / "End Ride")
            Button(
                onClick = {
                    if (turnByTurnState.isRideActive) {
                        onToggleRide(null)
                    } else {
                        showRoutePicker = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (turnByTurnState.isRideActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (turnByTurnState.isRideActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (turnByTurnState.isRideActive) "End Ride" else "Start Ride",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showRoutePicker && !turnByTurnState.isRideActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Select Route to Navigate",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FilledTonalButton(
                            onClick = {
                                showRoutePicker = false
                                onToggleRide(SampleRoutes.cityScenicLoop)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("City Scenic Loop (5.2 km)")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = {
                                showRoutePicker = false
                                onToggleRide(SampleRoutes.mountainTrail)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Twin Peaks Trail (8.4 km)")
                        }
                    }
                }
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
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PermissionRequestOverlay(
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
                    text = "Bike Navigation needs high-accuracy location permissions to provide live route overlays, speed telemetry, and turn-by-turn guidance.",
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

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format(Locale.US, "%.1f km", meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}

private fun formatEta(seconds: Long): String {
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

@Preview(showBackground = true)
@Composable
fun ManeuverBannerCardPreview() {
    MaterialTheme {
        ManeuverBannerCard(
            maneuver = ManeuverInstruction(
                stepIndex = 1,
                instruction = "In 150 m, Turn Right onto 5th St",
                turnType = TurnType.TURN_RIGHT,
                distanceToManeuverMeters = 150.0,
                streetName = "5th St"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TelemetryHudCardPreview() {
    MaterialTheme {
        TelemetryHudCard(
            currentLocation = LocationData(
                speedMps = 6.5f,
                latitude = 37.7749,
                longitude = -122.4194
            ),
            isServiceRunning = true,
            turnByTurnState = TurnByTurnState(
                isNavigating = true,
                telemetry = NavigationTelemetry(
                    currentSpeedKmh = 23.4f,
                    avgSpeedKmh = 19.8f,
                    remainingDistanceMeters = 4200.0,
                    etaSeconds = 720
                )
            ),
            onToggleRide = {}
        )
    }
}
