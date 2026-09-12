package com.dopetechindia.bikenavigation.data.repository

import com.dopetechindia.bikenavigation.data.model.LocationData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface for real-time location stream publishing and observation.
 */
interface LocationRepository {
    val currentLocation: StateFlow<LocationData?>
    suspend fun updateLocation(locationData: LocationData): Result<Unit>
    fun observeRiderLocation(riderId: String): Flow<LocationData?>
    fun observeGroupRidersLocations(groupId: String, memberIds: List<String>): Flow<Map<String, LocationData>>
}

/**
 * Firebase Realtime Database & Firestore implementation of [LocationRepository] with dual-write and offline fallback.
 */
class FirebaseLocationRepository(
    private val firebaseDatabase: FirebaseDatabase? = try { FirebaseDatabase.getInstance() } catch (_: Throwable) { null },
    private val firestore: FirebaseFirestore? = try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
) : LocationRepository {

    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    override val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    private val localLocationsCache = ConcurrentHashMap<String, LocationData>()

    override suspend fun updateLocation(locationData: LocationData): Result<Unit> {
        _currentLocation.value = locationData
        localLocationsCache[locationData.riderId] = locationData

        val db = firebaseDatabase
        val store = firestore
        if (db == null && store == null) {
            return Result.success(Unit)
        }

        return try {
            val riderId = locationData.riderId.ifEmpty { "unknown_rider" }

            db?.getReference("locations")?.child(riderId)?.setValue(locationData)?.await()
            store?.collection("locations")?.document(riderId)?.set(locationData)?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun observeRiderLocation(riderId: String): Flow<LocationData?> = callbackFlow {
        if (riderId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val db = firebaseDatabase
        if (db == null) {
            trySend(localLocationsCache[riderId])
            awaitClose {}
            return@callbackFlow
        }

        val dbRef = db.getReference("locations").child(riderId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loc = snapshot.getValue(LocationData::class.java)
                if (loc != null) {
                    localLocationsCache[riderId] = loc
                    trySend(loc)
                } else {
                    trySend(localLocationsCache[riderId])
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(localLocationsCache[riderId])
            }
        }

        try {
            dbRef.addValueEventListener(listener)
        } catch (e: Exception) {
            trySend(localLocationsCache[riderId])
        }

        awaitClose {
            try {
                dbRef.removeEventListener(listener)
            } catch (_: Exception) {}
        }
    }

    override fun observeGroupRidersLocations(groupId: String, memberIds: List<String>): Flow<Map<String, LocationData>> = callbackFlow {
        if (groupId.isEmpty() || memberIds.isEmpty()) {
            trySend(localLocationsCache.toMap())
            close()
            return@callbackFlow
        }

        val db = firebaseDatabase
        if (db == null) {
            trySend(localLocationsCache.toMap())
            awaitClose {}
            return@callbackFlow
        }

        val locationsMap = ConcurrentHashMap<String, LocationData>(localLocationsCache)
        val listeners = mutableListOf<Pair<String, ValueEventListener>>()

        for (memberId in memberIds) {
            val dbRef = db.getReference("locations").child(memberId)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val loc = snapshot.getValue(LocationData::class.java)
                    if (loc != null) {
                        locationsMap[memberId] = loc
                        localLocationsCache[memberId] = loc
                    }
                    trySend(locationsMap.toMap())
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(locationsMap.toMap())
                }
            }

            try {
                dbRef.addValueEventListener(listener)
                listeners.add(Pair(memberId, listener))
            } catch (_: Exception) {
                trySend(locationsMap.toMap())
            }
        }

        awaitClose {
            for ((memberId, listener) in listeners) {
                try {
                    db.getReference("locations").child(memberId).removeEventListener(listener)
                } catch (_: Exception) {}
            }
        }
    }
}
