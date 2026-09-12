package com.dopetechindia.bikenavigation.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.dopetechindia.bikenavigation.data.di.RepositoryProvider
import com.dopetechindia.bikenavigation.data.model.GroupSession
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.NavigationTelemetry
import com.dopetechindia.bikenavigation.data.model.SampleRoutes
import com.dopetechindia.bikenavigation.data.model.TurnType
import com.dopetechindia.bikenavigation.location.LocationService
import com.dopetechindia.bikenavigation.navigation.TurnByTurnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Android Auto / Automotive CarScreen rendering real-time heads-up navigation maneuvers,
 * live speed metrics, trip stats, and group rider telemetry. Subscribes reactively to mobile
 * state streams and invokes [invalidate] to reflect changes instantaneously.
 */
class MotorcycleCarScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {

    enum class DisplayMode {
        NAVIGATION,
        GROUP_TELEMETRY,
        TRIP_STATS
    }

    private var currentMode = DisplayMode.NAVIGATION

    private var turnByTurnState = TurnByTurnState()
    private var currentLocation: LocationData? = null
    private var telemetry: NavigationTelemetry = NavigationTelemetry()
    private var activeGroupSession: GroupSession? = null
    private var groupRidersLocations: Map<String, LocationData> = emptyMap()

    private var screenScope: CoroutineScope? = null
    private var groupObserveJob: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        screenScope = scope

        // 1. Observe Turn-by-Turn Engine state & trigger real-time screen invalidate
        scope.launch {
            LocationService.turnByTurnEngine.state.collectLatest { state ->
                turnByTurnState = state
                telemetry = state.telemetry
                invalidate()
            }
        }

        // 2. Observe Live Location Repository & trigger real-time screen invalidate
        scope.launch {
            RepositoryProvider.locationRepository.currentLocation.collectLatest { loc ->
                currentLocation = loc
                invalidate()
            }
        }

        // 3. Observe Telemetry Repository updates & trigger real-time screen invalidate
        scope.launch {
            RepositoryProvider.telemetryRepository.currentTelemetry.collectLatest { tel ->
                if (tel != null) {
                    telemetry = tel
                    invalidate()
                }
            }
        }

        // 4. Observe Group Session & Group Rider Telemetry & trigger real-time screen invalidate
        scope.launch {
            RepositoryProvider.groupRepository.observeGroupSession("demo_group_123")
                .collectLatest { session ->
                    activeGroupSession = session
                    if (session != null) {
                        observeGroupRidersLocations(session.groupId, session.memberIds)
                    }
                    invalidate()
                }
        }
    }

    private fun observeGroupRidersLocations(groupId: String, memberIds: List<String>) {
        groupObserveJob?.cancel()
        groupObserveJob = screenScope?.launch {
            RepositoryProvider.locationRepository.observeGroupRidersLocations(groupId, memberIds)
                .collectLatest { locations ->
                    groupRidersLocations = locations
                    invalidate()
                }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        groupObserveJob?.cancel()
        screenScope?.cancel()
        screenScope = null
    }

    override fun onGetTemplate(): Template {
        return when (currentMode) {
            DisplayMode.NAVIGATION -> buildNavigationTemplate()
            DisplayMode.GROUP_TELEMETRY -> buildGroupTelemetryTemplate()
            DisplayMode.TRIP_STATS -> buildTripStatsTemplate()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // TEMPLATE BUILDERS
    // ---------------------------------------------------------------------------------------------

    private fun buildNavigationTemplate(): Template {
        val isNavigating = turnByTurnState.isNavigating
        val currentManeuver = turnByTurnState.currentManeuver

        if (!isNavigating || currentManeuver == null) {
            val paneBuilder = Pane.Builder()

            val titleRow = Row.Builder()
                .setTitle(
                    if (turnByTurnState.isDestinationReached) "Destination Reached!"
                    else "Motorcycle Navigation Ready"
                )
                .addText(
                    if (turnByTurnState.isDestinationReached) "You have safely arrived at your destination."
                    else "Select a motorcycle route or tap 'Start Navigation' below."
                )
                .build()
            paneBuilder.addRow(titleRow)

            val speedRow = Row.Builder()
                .setTitle("Current Speed")
                .addText(String.format(Locale.US, "%.1f km/h", currentLocation?.speedKmh ?: 0f))
                .build()
            paneBuilder.addRow(speedRow)

            val startAction = Action.Builder()
                .setTitle(if (isNavigating) "Stop Navigation" else "Start Demo Route")
                .setOnClickListener {
                    if (isNavigating) {
                        LocationService.turnByTurnEngine.stopNavigation()
                    } else {
                        val sampleRoute = SampleRoutes.cityScenicLoop
                        LocationService.turnByTurnEngine.startNavigation(sampleRoute, "rider_me")
                        LocationService.start(carContext)
                    }
                    invalidate()
                }
                .build()

            paneBuilder.addAction(startAction)

            val header = Header.Builder()
                .setTitle("Motorcycle Navigation")
                .addEndHeaderAction(buildCycleModeAction())
                .build()

            return PaneTemplate.Builder(paneBuilder.build())
                .setHeader(header)
                .build()
        }

        val maneuverType = mapTurnTypeToCarManeuver(currentManeuver.turnType)
        val carManeuver = Maneuver.Builder(maneuverType).build()

        val stepBuilder = Step.Builder(currentManeuver.instruction)
            .setManeuver(carManeuver)
        if (currentManeuver.streetName.isNotBlank()) {
            stepBuilder.setCue(currentManeuver.streetName)
        }
        val step = stepBuilder.build()

        val distMeters = currentManeuver.distanceToManeuverMeters
        val distance = if (distMeters >= 1000.0) {
            Distance.create(distMeters / 1000.0, Distance.UNIT_KILOMETERS)
        } else {
            Distance.create(distMeters, Distance.UNIT_METERS)
        }

        val routingInfo = RoutingInfo.Builder()
            .setCurrentStep(step, distance)
            .setLoading(false)
            .build()

        val remainingDistMeters = telemetry.remainingDistanceMeters ?: 0.0
        val remainingDist = if (remainingDistMeters >= 1000.0) {
            Distance.create(remainingDistMeters / 1000.0, Distance.UNIT_KILOMETERS)
        } else {
            Distance.create(remainingDistMeters, Distance.UNIT_METERS)
        }

        val etaSeconds = telemetry.etaSeconds ?: 0L
        val arrivalTimeMillis = System.currentTimeMillis() + (etaSeconds * 1000L)
        val dateTimeWithZone = DateTimeWithZone.create(
            arrivalTimeMillis,
            TimeZone.getDefault()
        )

        val travelEstimate = TravelEstimate.Builder(remainingDist, dateTimeWithZone)
            .setRemainingTimeSeconds(etaSeconds)
            .setRemainingDistanceColor(CarColor.GREEN)
            .build()

        return NavigationTemplate.Builder()
            .setNavigationInfo(routingInfo)
            .setDestinationTravelEstimate(travelEstimate)
            .setActionStrip(buildGlobalActionStrip())
            .build()
    }

    private fun buildGroupTelemetryTemplate(): Template {
        val paneBuilder = Pane.Builder()

        val session = activeGroupSession
        val locations = groupRidersLocations
        val myLoc = currentLocation

        val groupHeaderRow = Row.Builder()
            .setTitle(session?.groupName ?: "Motorcycle Group Ride")
            .addText("Group Code: ${session?.groupCode ?: "RID399"} • ${locations.size.coerceAtLeast(1)} Active Riders")
            .build()

        paneBuilder.addRow(groupHeaderRow)

        if (locations.isEmpty()) {
            val emptyRow = Row.Builder()
                .setTitle("Connected Riders Telemetry")
                .addText("No group telemetry updates received yet. Start group session in mobile app.")
                .build()
            paneBuilder.addRow(emptyRow)
        } else {
            for ((riderId, riderLoc) in locations.entries.take(4)) {
                val isMe = riderId == "rider_me" || riderId == (myLoc?.riderId ?: "")
                val name = when {
                    isMe -> "You (Leader)"
                    riderId == "sim_rider_alex" -> "Alex (Rider)"
                    riderId == "sim_rider_sam" -> "Sam (Tail Rider)"
                    else -> "Rider #${riderId.takeLast(4)}"
                }

                val distFromUser = if (isMe || myLoc == null) 0.0 else calculateDistanceMeters(
                    myLoc.latitude, myLoc.longitude,
                    riderLoc.latitude, riderLoc.longitude
                )

                val distFormatted = if (distFromUser >= 1000.0) {
                    String.format(Locale.US, "%.1f km away", distFromUser / 1000.0)
                } else {
                    String.format(Locale.US, "%.0f m away", distFromUser)
                }

                val riderRow = Row.Builder()
                    .setTitle(name)
                    .addText(
                        String.format(
                            Locale.US,
                            "Speed: %.1f km/h • %s",
                            riderLoc.speedKmh,
                            if (isMe) "Current Location" else distFormatted
                        )
                    )
                    .build()

                paneBuilder.addRow(riderRow)
            }
        }

        val header = Header.Builder()
            .setTitle("Group Rider Telemetry")
            .addEndHeaderAction(buildCycleModeAction())
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
            .build()
    }

    private fun buildTripStatsTemplate(): Template {
        val paneBuilder = Pane.Builder()

        val speedRow = Row.Builder()
            .setTitle("Live Speed Metrics")
            .addText(
                String.format(
                    Locale.US,
                    "Current: %.1f km/h | Avg: %.1f km/h | Max: %.1f km/h",
                    telemetry.currentSpeedKmh,
                    telemetry.avgSpeedKmh,
                    telemetry.maxSpeedKmh
                )
            )
            .build()
        paneBuilder.addRow(speedRow)

        val distKm = telemetry.distanceTraveledMeters / 1000.0
        val distRow = Row.Builder()
            .setTitle("Distance & Duration")
            .addText(
                String.format(
                    Locale.US,
                    "Total Traveled: %.2f km | Battery: %d%%",
                    distKm,
                    telemetry.batteryLevel
                )
            )
            .build()
        paneBuilder.addRow(distRow)

        val statusRow = Row.Builder()
            .setTitle("Navigation Status")
            .addText(
                if (turnByTurnState.isNavigating) "Status: NAVIGATING • Step ${turnByTurnState.currentStepIndex + 1}/${turnByTurnState.currentRoute?.steps?.size ?: 0}"
                else "Status: IDLE / READY"
            )
            .build()
        paneBuilder.addRow(statusRow)

        val toggleNavAction = Action.Builder()
            .setTitle(if (turnByTurnState.isNavigating) "Stop Navigation" else "Start Demo Navigation")
            .setOnClickListener {
                if (turnByTurnState.isNavigating) {
                    LocationService.turnByTurnEngine.stopNavigation()
                } else {
                    val sampleRoute = SampleRoutes.cityScenicLoop
                    LocationService.turnByTurnEngine.startNavigation(sampleRoute, "rider_me")
                    LocationService.start(carContext)
                }
                invalidate()
            }
            .build()
        paneBuilder.addAction(toggleNavAction)

        val header = Header.Builder()
            .setTitle("Ride Dashboard Telemetry")
            .addEndHeaderAction(buildCycleModeAction())
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
            .build()
    }

    // ---------------------------------------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------------------------------------

    private fun buildCycleModeAction(): Action {
        val nextModeText = when (currentMode) {
            DisplayMode.NAVIGATION -> "View: Group"
            DisplayMode.GROUP_TELEMETRY -> "View: Stats"
            DisplayMode.TRIP_STATS -> "View: Nav"
        }

        return Action.Builder()
            .setTitle(nextModeText)
            .setOnClickListener {
                currentMode = when (currentMode) {
                    DisplayMode.NAVIGATION -> DisplayMode.GROUP_TELEMETRY
                    DisplayMode.GROUP_TELEMETRY -> DisplayMode.TRIP_STATS
                    DisplayMode.TRIP_STATS -> DisplayMode.NAVIGATION
                }
                invalidate()
            }
            .build()
    }

    private fun buildGlobalActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            .addAction(buildCycleModeAction())
            .build()
    }

    private fun mapTurnTypeToCarManeuver(turnType: TurnType): Int {
        return when (turnType) {
            TurnType.START -> Maneuver.TYPE_DEPART
            TurnType.STRAIGHT -> Maneuver.TYPE_STRAIGHT
            TurnType.TURN_LEFT -> Maneuver.TYPE_TURN_NORMAL_LEFT
            TurnType.TURN_RIGHT -> Maneuver.TYPE_TURN_NORMAL_RIGHT
            TurnType.SLIGHT_LEFT -> Maneuver.TYPE_TURN_SLIGHT_LEFT
            TurnType.SLIGHT_RIGHT -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
            TurnType.U_TURN -> Maneuver.TYPE_U_TURN_LEFT
            TurnType.ARRIVED -> Maneuver.TYPE_DESTINATION
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
