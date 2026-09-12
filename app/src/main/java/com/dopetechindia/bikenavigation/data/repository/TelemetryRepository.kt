package com.dopetechindia.bikenavigation.data.repository

import com.dopetechindia.bikenavigation.data.model.NavigationTelemetry
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface for telemetry updates (speed, distance, ride duration, battery).
 */
interface TelemetryRepository {
    val currentTelemetry: StateFlow<NavigationTelemetry?>
    suspend fun updateTelemetry(telemetry: NavigationTelemetry): Result<Unit>
    fun observeRiderTelemetry(riderId: String): Flow<NavigationTelemetry?>
}

/**
 * Firebase Realtime Database implementation of [TelemetryRepository].
 */
class FirebaseTelemetryRepository(
    private val firebaseDatabase: FirebaseDatabase? = try { FirebaseDatabase.getInstance() } catch (_: Throwable) { null }
) : TelemetryRepository {

    private val _currentTelemetry = MutableStateFlow<NavigationTelemetry?>(null)
    override val currentTelemetry: StateFlow<NavigationTelemetry?> = _currentTelemetry.asStateFlow()

    private val localTelemetryCache = ConcurrentHashMap<String, NavigationTelemetry>()

    override suspend fun updateTelemetry(telemetry: NavigationTelemetry): Result<Unit> {
        _currentTelemetry.value = telemetry
        localTelemetryCache[telemetry.riderId] = telemetry

        val db = firebaseDatabase ?: return Result.success(Unit)

        return try {
            val riderId = telemetry.riderId.ifEmpty { "unknown_rider" }
            val dbRef = db.getReference("telemetry").child(riderId)
            dbRef.setValue(telemetry).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun observeRiderTelemetry(riderId: String): Flow<NavigationTelemetry?> = callbackFlow {
        if (riderId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val db = firebaseDatabase
        if (db == null) {
            trySend(localTelemetryCache[riderId])
            awaitClose {}
            return@callbackFlow
        }

        val dbRef = db.getReference("telemetry").child(riderId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tel = snapshot.getValue(NavigationTelemetry::class.java)
                if (tel != null) {
                    localTelemetryCache[riderId] = tel
                    trySend(tel)
                } else {
                    trySend(localTelemetryCache[riderId])
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(localTelemetryCache[riderId])
            }
        }

        try {
            dbRef.addValueEventListener(listener)
        } catch (e: Exception) {
            trySend(localTelemetryCache[riderId])
        }

        awaitClose {
            try {
                dbRef.removeEventListener(listener)
            } catch (_: Exception) {}
        }
    }
}
