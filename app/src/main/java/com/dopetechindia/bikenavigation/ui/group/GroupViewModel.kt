package com.dopetechindia.bikenavigation.ui.group

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dopetechindia.bikenavigation.data.auth.AuthRepository
import com.dopetechindia.bikenavigation.data.di.RepositoryProvider
import com.dopetechindia.bikenavigation.data.model.GroupSession
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.Rider
import com.dopetechindia.bikenavigation.data.model.RiderStatus
import com.dopetechindia.bikenavigation.data.repository.GroupRepository
import com.dopetechindia.bikenavigation.data.repository.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * UI State representing a rider member within a group sharing session.
 */
data class GroupMemberUiState(
    val riderId: String,
    val displayName: String,
    val status: RiderStatus = RiderStatus.ONLINE,
    val speedKmh: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceFromUserMeters: Double = 0.0,
    val isHost: Boolean = false,
    val isCurrentUser: Boolean = false,
    val lastUpdatedText: String = "Just now"
)

/**
 * ViewModel managing group ride sharing sessions, creating and joining 6-character group codes,
 * and running simulated group rider telemetry updates for testing offline.
 */
class GroupViewModel(
    private val groupRepository: GroupRepository = RepositoryProvider.groupRepository,
    private val locationRepository: LocationRepository = RepositoryProvider.locationRepository,
    private val authRepository: AuthRepository = RepositoryProvider.authRepository
) : ViewModel() {

    private val _currentGroup = MutableStateFlow<GroupSession?>(null)
    val currentGroup: StateFlow<GroupSession?> = _currentGroup.asStateFlow()

    private val _memberLocations = MutableStateFlow<Map<String, LocationData>>(emptyMap())
    val memberLocations: StateFlow<Map<String, LocationData>> = _memberLocations.asStateFlow()

    private val _groupMembersList = MutableStateFlow<List<GroupMemberUiState>>(emptyList())
    val groupMembersList: StateFlow<List<GroupMemberUiState>> = _groupMembersList.asStateFlow()

    var groupNameInput by mutableStateOf("")
    var groupCodeInput by mutableStateOf("")

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isSimulatorActive by mutableStateOf(false)
        private set

    private var groupObserveJob: Job? = null
    private var locationObserveJob: Job? = null
    private var simulatorJob: Job? = null

    init {
        viewModelScope.launch {
            locationRepository.currentLocation.collect { userLoc ->
                recalculateMemberUiStates(userLoc)
            }
        }
    }

    /**
     * Creates a new group session with the provided name and registers the current rider as host.
     * In local guest mode, seamlessly starts the local group simulator with simulated nearby riders.
     */
    fun createGroup(name: String) {
        if (name.isBlank()) {
            errorMessage = "Please enter a group name"
            return
        }
        val currentUser = authRepository.getCurrentUser()
        val riderId = currentUser?.uid ?: "guest_rider_local"
        val displayName = if (currentUser == null || currentUser.isAnonymous) "Guest Rider" else (currentUser.displayName ?: "Rider")
        val hostRider = Rider(
            id = riderId,
            displayName = displayName,
            email = currentUser?.email ?: "",
            status = RiderStatus.RIDING
        )

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = groupRepository.createGroup(name.trim(), hostRider)
            isLoading = false

            result.onSuccess { session ->
                _currentGroup.value = session
                startObservingGroup(session.groupId)
                // Automatically activate local rider simulator in local guest mode
                isSimulatorActive = true
                startSimulator(locationRepository.currentLocation.value)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to create group session"
            }
        }
    }

    /**
     * Joins an existing group session using a 6-character group code.
     * In local guest mode, seamlessly activates the local group simulator with simulated nearby riders.
     */
    fun joinGroup(codeOrId: String) {
        if (codeOrId.isBlank()) {
            errorMessage = "Please enter a 6-character group code"
            return
        }
        val currentUser = authRepository.getCurrentUser()
        val riderId = currentUser?.uid ?: "guest_rider_local"
        val displayName = if (currentUser == null || currentUser.isAnonymous) "Guest Rider" else (currentUser.displayName ?: "Rider")
        val joiningRider = Rider(
            id = riderId,
            displayName = displayName,
            email = currentUser?.email ?: "",
            status = RiderStatus.RIDING
        )

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = groupRepository.joinGroup(codeOrId.trim(), joiningRider)
            isLoading = false

            result.onSuccess { session ->
                _currentGroup.value = session
                startObservingGroup(session.groupId)
                // Automatically activate local rider simulator in local guest mode
                isSimulatorActive = true
                startSimulator(locationRepository.currentLocation.value)
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Group session not found"
            }
        }
    }

    /**
     * Leaves the active group session and stops telemetry observation.
     */
    fun leaveGroup() {
        val session = _currentGroup.value ?: return
        val currentUser = authRepository.getCurrentUser()
        val riderId = currentUser?.uid ?: "rider_me"

        viewModelScope.launch {
            groupRepository.leaveGroup(session.groupId, riderId)
            stopObservingGroup()
            _currentGroup.value = null
            _memberLocations.value = emptyMap()
            _groupMembersList.value = emptyList()
            stopSimulator()
        }
    }

    /**
     * Shares the active group code via system intent.
     */
    fun shareGroupCode(context: Context) {
        val group = _currentGroup.value ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Join my bike ride group '${group.groupName}' using code: ${group.groupCode}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Group Code"))
    }

    /**
     * Toggles simulated group rider telemetry generator for testing.
     */
    fun toggleSimulator(userLoc: LocationData?) {
        isSimulatorActive = !isSimulatorActive
        if (isSimulatorActive) {
            startSimulator(userLoc)
        } else {
            stopSimulator()
        }
    }

    private fun startObservingGroup(groupId: String) {
        groupObserveJob?.cancel()
        groupObserveJob = viewModelScope.launch {
            groupRepository.observeGroupSession(groupId).collectLatest { session ->
                _currentGroup.value = session
                if (session != null) {
                    observeMemberLocations(groupId, session.memberIds)
                }
            }
        }
    }

    private fun observeMemberLocations(groupId: String, memberIds: List<String>) {
        locationObserveJob?.cancel()
        locationObserveJob = viewModelScope.launch {
            locationRepository.observeGroupRidersLocations(groupId, memberIds).collectLatest { locations ->
                _memberLocations.value = locations
                recalculateMemberUiStates(locationRepository.currentLocation.value)
            }
        }
    }

    private fun stopObservingGroup() {
        groupObserveJob?.cancel()
        locationObserveJob?.cancel()
    }

    private fun startSimulator(userLoc: LocationData?) {
        simulatorJob?.cancel()
        val baseLat = userLoc?.latitude ?: 37.7749
        val baseLng = userLoc?.longitude ?: -122.4194

        val currentUser = authRepository.getCurrentUser()
        val currentUserId = currentUser?.uid ?: "rider_me"

        simulatorJob = viewModelScope.launch {
            var step = 0
            while (isSimulatorActive) {
                step++
                val sim1Loc = LocationData(
                    riderId = "sim_rider_alex",
                    latitude = baseLat + 0.002 * cos(step * 0.1),
                    longitude = baseLng + 0.002 * sin(step * 0.1),
                    speedMps = (8f + (step % 5)),
                    bearing = (step * 10 % 360).toFloat()
                )

                val sim2Loc = LocationData(
                    riderId = "sim_rider_sam",
                    latitude = baseLat - 0.0015 * sin(step * 0.1),
                    longitude = baseLng + 0.003 * cos(step * 0.1),
                    speedMps = (11f + (step % 4)),
                    bearing = ((step * 15 + 90) % 360).toFloat()
                )

                val currentLocs = _memberLocations.value.toMutableMap()
                currentLocs["sim_rider_alex"] = sim1Loc
                currentLocs["sim_rider_sam"] = sim2Loc

                if (_currentGroup.value == null) {
                    _currentGroup.value = GroupSession(
                        groupId = "demo_group_123",
                        groupName = "Demo Ride Squad",
                        groupCode = "RID399",
                        hostRiderId = currentUserId,
                        memberIds = listOf(currentUserId, "sim_rider_alex", "sim_rider_sam")
                    )
                }

                _memberLocations.value = currentLocs
                recalculateMemberUiStates(userLoc)
                delay(1500)
            }
        }
    }

    private fun stopSimulator() {
        simulatorJob?.cancel()
        simulatorJob = null
        isSimulatorActive = false
    }

    private fun recalculateMemberUiStates(userLocation: LocationData?) {
        val session = _currentGroup.value
        val locations = _memberLocations.value
        val currentUser = authRepository.getCurrentUser()
        val currentUserId = currentUser?.uid ?: "rider_me"

        val memberIds = session?.memberIds.orEmpty().ifEmpty {
            locations.keys.toList()
        }

        val list = mutableListOf<GroupMemberUiState>()

        for (mId in memberIds) {
            val loc = locations[mId]
            val isMe = mId == currentUserId || mId == "rider_me" || mId == "guest_rider_local" || (loc != null && loc.riderId == currentUserId)

            val name = when {
                isMe -> {
                    if (currentUser == null || currentUser.isAnonymous) "Guest Rider (You)" else (currentUser.displayName ?: "You (Current Rider)")
                }
                mId == "sim_rider_alex" -> "Alex (Rider)"
                mId == "sim_rider_sam" -> "Sam (Tail Rider)"
                else -> "Rider #${mId.takeLast(4)}"
            }

            val lat = loc?.latitude ?: userLocation?.latitude ?: 0.0
            val lng = loc?.longitude ?: userLocation?.longitude ?: 0.0
            val speed = loc?.speedKmh ?: 0f

            val userLat = userLocation?.latitude ?: lat
            val userLng = userLocation?.longitude ?: lng
            val distance = calculateDistanceMeters(userLat, userLng, lat, lng)

            val isHost = session?.hostRiderId == mId

            list.add(
                GroupMemberUiState(
                    riderId = mId,
                    displayName = name,
                    status = if (speed > 2f) RiderStatus.RIDING else RiderStatus.ONLINE,
                    speedKmh = speed,
                    latitude = lat,
                    longitude = lng,
                    distanceFromUserMeters = if (isMe) 0.0 else distance,
                    isHost = isHost,
                    isCurrentUser = isMe,
                    lastUpdatedText = "Live"
                )
            )
        }

        _groupMembersList.value = list
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
