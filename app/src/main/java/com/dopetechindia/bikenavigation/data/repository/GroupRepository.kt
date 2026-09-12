package com.dopetechindia.bikenavigation.data.repository

import com.dopetechindia.bikenavigation.data.model.GroupSession
import com.dopetechindia.bikenavigation.data.model.Rider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface for managing group ride sharing sessions.
 */
interface GroupRepository {
    suspend fun createGroup(groupName: String, hostRider: Rider): Result<GroupSession>
    suspend fun joinGroup(groupCodeOrId: String, rider: Rider): Result<GroupSession>
    suspend fun leaveGroup(groupId: String, riderId: String): Result<Unit>
    fun observeGroupSession(groupId: String): Flow<GroupSession?>
}

/**
 * Firebase Firestore implementation of [GroupRepository] with offline local fallback.
 */
class FirebaseGroupRepository(
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
) : GroupRepository {

    private val localGroups = ConcurrentHashMap<String, GroupSession>()

    override suspend fun createGroup(groupName: String, hostRider: Rider): Result<GroupSession> {
        val groupId = UUID.randomUUID().toString().take(12)
        val groupCode = UUID.randomUUID().toString().take(6).uppercase()

        val session = GroupSession(
            groupId = groupId,
            groupName = groupName,
            groupCode = groupCode,
            hostRiderId = hostRider.id,
            memberIds = listOf(hostRider.id),
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        localGroups[groupId] = session
        localGroups[groupCode] = session

        val store = firestore ?: return Result.success(session)

        return try {
            store.collection("groups").document(groupId).set(session).await()
            Result.success(session)
        } catch (e: Exception) {
            Result.success(session)
        }
    }

    override suspend fun joinGroup(groupCodeOrId: String, rider: Rider): Result<GroupSession> {
        val store = firestore
        if (store == null) {
            val cached = localGroups[groupCodeOrId] ?: localGroups[groupCodeOrId.uppercase()]
            if (cached != null) {
                val updatedMembers = (cached.memberIds + rider.id).distinct()
                val updatedGroup = cached.copy(memberIds = updatedMembers)
                localGroups[cached.groupId] = updatedGroup
                return Result.success(updatedGroup)
            }
            return Result.failure(Exception("Group not found with code: $groupCodeOrId"))
        }

        return try {
            val querySnapshot = store.collection("groups")
                .whereEqualTo("groupCode", groupCodeOrId.uppercase())
                .get()
                .await()

            val doc = querySnapshot.documents.firstOrNull() 
                ?: store.collection("groups").document(groupCodeOrId).get().await()

            if (!doc.exists()) {
                val cached = localGroups[groupCodeOrId] ?: localGroups[groupCodeOrId.uppercase()]
                if (cached != null) {
                    val updatedMembers = (cached.memberIds + rider.id).distinct()
                    val updatedGroup = cached.copy(memberIds = updatedMembers)
                    localGroups[cached.groupId] = updatedGroup
                    return Result.success(updatedGroup)
                }
                return Result.failure(Exception("Group not found with code: $groupCodeOrId"))
            }

            val session = doc.toObject(GroupSession::class.java)
                ?: return Result.failure(Exception("Failed to parse group session"))

            val updatedMembers = (session.memberIds + rider.id).distinct()
            val updatedGroup = session.copy(memberIds = updatedMembers)

            store.collection("groups").document(session.groupId).set(updatedGroup).await()
            localGroups[session.groupId] = updatedGroup

            Result.success(updatedGroup)
        } catch (e: Exception) {
            val cached = localGroups[groupCodeOrId] ?: localGroups[groupCodeOrId.uppercase()]
            if (cached != null) {
                val updatedMembers = (cached.memberIds + rider.id).distinct()
                val updatedGroup = cached.copy(memberIds = updatedMembers)
                localGroups[cached.groupId] = updatedGroup
                Result.success(updatedGroup)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun leaveGroup(groupId: String, riderId: String): Result<Unit> {
        val store = firestore
        if (store == null) {
            val cached = localGroups[groupId]
            if (cached != null) {
                val updatedMembers = cached.memberIds.filter { it != riderId }
                localGroups[groupId] = cached.copy(memberIds = updatedMembers)
            }
            return Result.success(Unit)
        }

        return try {
            val doc = store.collection("groups").document(groupId).get().await()
            if (doc.exists()) {
                val session = doc.toObject(GroupSession::class.java)
                if (session != null) {
                    val updatedMembers = session.memberIds.filter { it != riderId }
                    val updatedGroup = session.copy(
                        memberIds = updatedMembers,
                        isActive = updatedMembers.isNotEmpty()
                    )
                    store.collection("groups").document(groupId).set(updatedGroup).await()
                    localGroups[groupId] = updatedGroup
                }
            } else {
                val cached = localGroups[groupId]
                if (cached != null) {
                    val updatedMembers = cached.memberIds.filter { it != riderId }
                    localGroups[groupId] = cached.copy(memberIds = updatedMembers)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val cached = localGroups[groupId]
            if (cached != null) {
                val updatedMembers = cached.memberIds.filter { it != riderId }
                localGroups[groupId] = cached.copy(memberIds = updatedMembers)
            }
            Result.success(Unit)
        }
    }

    override fun observeGroupSession(groupId: String): Flow<GroupSession?> = callbackFlow {
        if (groupId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val store = firestore
        if (store == null) {
            trySend(localGroups[groupId])
            awaitClose {}
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            val docRef = store.collection("groups").document(groupId)
            registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(localGroups[groupId])
                    return@addSnapshotListener
                }
                val session = snapshot.toObject(GroupSession::class.java)
                if (session != null) {
                    localGroups[groupId] = session
                    trySend(session)
                } else {
                    trySend(localGroups[groupId])
                }
            }
        } catch (e: Exception) {
            trySend(localGroups[groupId])
        }

        awaitClose {
            registration?.remove()
        }
    }
}

/**
 * Local in-memory mock implementation of [GroupRepository] powering 100% offline Guest Mode
 * group ride sessions and simulation.
 */
class MockGroupRepository : GroupRepository {
    private val localGroups = ConcurrentHashMap<String, GroupSession>()
    private val groupSessionStateFlows = ConcurrentHashMap<String, MutableStateFlow<GroupSession?>>()

    override suspend fun createGroup(groupName: String, hostRider: Rider): Result<GroupSession> {
        val groupId = "local_group_${UUID.randomUUID().toString().take(8)}"
        val groupCode = UUID.randomUUID().toString().take(6).uppercase()

        val session = GroupSession(
            groupId = groupId,
            groupName = groupName,
            groupCode = groupCode,
            hostRiderId = hostRider.id,
            memberIds = listOf(hostRider.id, "sim_rider_alex", "sim_rider_sam"),
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        localGroups[groupId] = session
        localGroups[groupCode] = session
        getOrCreateStateFlow(groupId).value = session

        return Result.success(session)
    }

    override suspend fun joinGroup(groupCodeOrId: String, rider: Rider): Result<GroupSession> {
        val code = groupCodeOrId.trim().uppercase()
        val cached = localGroups[code] ?: localGroups[groupCodeOrId]

        val session = if (cached != null) {
            val updatedMembers = (cached.memberIds + rider.id).distinct()
            cached.copy(memberIds = updatedMembers)
        } else {
            GroupSession(
                groupId = "local_group_${code.lowercase()}",
                groupName = "Local Ride Group ($code)",
                groupCode = code,
                hostRiderId = rider.id,
                memberIds = listOf(rider.id, "sim_rider_alex", "sim_rider_sam"),
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
        }

        localGroups[session.groupId] = session
        localGroups[session.groupCode] = session
        getOrCreateStateFlow(session.groupId).value = session

        return Result.success(session)
    }

    override suspend fun leaveGroup(groupId: String, riderId: String): Result<Unit> {
        val cached = localGroups[groupId]
        if (cached != null) {
            val updatedMembers = cached.memberIds.filter { it != riderId }
            val updatedGroup = cached.copy(
                memberIds = updatedMembers,
                isActive = updatedMembers.isNotEmpty()
            )
            localGroups[groupId] = updatedGroup
            getOrCreateStateFlow(groupId).value = if (updatedMembers.isEmpty()) null else updatedGroup
        }
        return Result.success(Unit)
    }

    override fun observeGroupSession(groupId: String): Flow<GroupSession?> {
        return getOrCreateStateFlow(groupId).asStateFlow()
    }

    private fun getOrCreateStateFlow(groupId: String): MutableStateFlow<GroupSession?> {
        return groupSessionStateFlows.getOrPut(groupId) {
            MutableStateFlow(localGroups[groupId])
        }
    }
}
