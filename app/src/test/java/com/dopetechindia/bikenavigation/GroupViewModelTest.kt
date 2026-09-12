package com.dopetechindia.bikenavigation

import com.dopetechindia.bikenavigation.data.model.AuthUser
import com.dopetechindia.bikenavigation.data.model.GroupSession
import com.dopetechindia.bikenavigation.data.model.LocationData
import com.dopetechindia.bikenavigation.data.model.Rider
import com.dopetechindia.bikenavigation.data.model.RiderStatus
import com.dopetechindia.bikenavigation.data.repository.FirebaseGroupRepository
import com.dopetechindia.bikenavigation.data.repository.MockGroupRepository
import com.dopetechindia.bikenavigation.ui.group.GroupMemberUiState
import com.dopetechindia.bikenavigation.ui.group.GroupViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testGroupMemberUiState() {
        val member = GroupMemberUiState(
            riderId = "rider_101",
            displayName = "Rider Bob",
            status = RiderStatus.RIDING,
            speedKmh = 45f,
            latitude = 37.7750,
            longitude = -122.4190,
            distanceFromUserMeters = 350.0,
            isHost = true,
            isCurrentUser = false
        )

        assertEquals("rider_101", member.riderId)
        assertEquals("Rider Bob", member.displayName)
        assertEquals(RiderStatus.RIDING, member.status)
        assertEquals(45f, member.speedKmh, 0.01f)
        assertTrue(member.isHost)
    }

    @Test
    fun testMockGroupRepositoryCreateAndJoin() = runTest {
        val repo = MockGroupRepository()
        val host = Rider(id = "guest_host", displayName = "Guest Host Rider")
        
        val createRes = repo.createGroup("Local Guest Pass", host)
        assertTrue(createRes.isSuccess)

        val group = createRes.getOrNull()
        assertNotNull(group)
        assertEquals("Local Guest Pass", group?.groupName)
        assertEquals(6, group?.groupCode?.length)

        val joiner = Rider(id = "guest_joiner", displayName = "Guest Joiner Rider")
        val joinRes = repo.joinGroup(group!!.groupCode, joiner)
        assertTrue(joinRes.isSuccess)
        val joinedGroup = joinRes.getOrNull()
        assertTrue((joinedGroup?.memberIds?.size ?: 0) >= 3)

        val leaveRes = repo.leaveGroup(group.groupId, "guest_joiner")
        assertTrue(leaveRes.isSuccess)
    }

    @Test
    fun testGroupViewModelCreateGroupValidation() = runTest {
        val viewModel = GroupViewModel()

        viewModel.createGroup("")
        assertEquals("Please enter a group name", viewModel.errorMessage)

        viewModel.joinGroup("")
        assertEquals("Please enter a 6-character group code", viewModel.errorMessage)
    }

    @Test
    fun testGroupViewModelCreateAndJoinGroupFlow() = runTest {
        val hostViewModel = GroupViewModel()

        hostViewModel.createGroup("Sunday Bikers")
        testScheduler.runCurrent()

        val group = hostViewModel.currentGroup.value
        assertNotNull(group)
        assertEquals("Sunday Bikers", group?.groupName)
        assertEquals(6, group?.groupCode?.length)

        val groupCode = group!!.groupCode

        val joinerViewModel = GroupViewModel()
        joinerViewModel.joinGroup(groupCode)
        testScheduler.runCurrent()

        assertNotNull(joinerViewModel.currentGroup.value)
        assertEquals("Sunday Bikers", joinerViewModel.currentGroup.value?.groupName)

        joinerViewModel.leaveGroup()
        testScheduler.runCurrent()
        assertNull(joinerViewModel.currentGroup.value)
    }

    @Test
    fun testGroupViewModelSimulatorToggle() = runTest {
        val viewModel = GroupViewModel()
        assertNull(viewModel.currentGroup.value)
        assertFalse(viewModel.isSimulatorActive)

        val userLoc = LocationData(riderId = "rider_me", latitude = 37.7749, longitude = -122.4194)
        viewModel.toggleSimulator(userLoc)

        assertTrue(viewModel.isSimulatorActive)

        testScheduler.runCurrent()
        assertNotNull(viewModel.currentGroup.value)
        assertTrue(viewModel.groupMembersList.value.isNotEmpty())

        viewModel.toggleSimulator(userLoc)
        assertFalse(viewModel.isSimulatorActive)
    }
}
