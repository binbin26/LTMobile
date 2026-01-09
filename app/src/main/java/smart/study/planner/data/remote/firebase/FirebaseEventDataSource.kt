package smart.study.planner.data.remote.firebase

import android.util.Log
import smart.study.planner.data.model.Event
import smart.study.planner.data.remote.dto.EventDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseEventDataSource"

/**
 * Data source for Firebase Realtime Database operations
 * Handles all Firebase-specific event operations
 */
@Singleton
class FirebaseEventDataSource @Inject constructor(
    private val databaseReference: DatabaseReference,
    private val firebaseAuth: FirebaseAuth
) {
    
    /**
     * Get current user ID or throw exception if not authenticated
     */
    private fun getUserId(): String {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "User not authenticated!")
            throw IllegalStateException("Người dùng chưa được xác thực. Vui lòng đăng nhập trước.")
        }
        Log.d(TAG, "User ID: $uid")
        return uid
    }
    
    /**
     * Get the events reference for current user
     */
    private fun getEventsReference(): DatabaseReference {
        return databaseReference.child("users").child(getUserId()).child("events")
    }
    
    /**
     * Verify and refresh auth token before operations
     */
    private suspend fun ensureAuthTokenValid(): Boolean {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.e(TAG, "No authenticated user")
            return false
        }
        
        return try {
            // Force refresh token to ensure it's valid
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token
            Log.d(TAG, "Auth token verified - Token exists: ${token != null}")
            Log.d(TAG, "User UID: ${user.uid}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify auth token", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            false
        }
    }
    
    /**
     * Observe all events from Firebase as a Flow
     */
    fun observeAllEvents(): Flow<Result<List<Event>>> = callbackFlow {
        try {
            val eventsRef = getEventsReference()
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val events = snapshot.children.mapNotNull { child ->
                            child.getValue(EventDto::class.java)?.toEvent()
                        }
                        Log.d(TAG, "Observed ${events.size} events from Firebase")
                        trySend(Result.success(events))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing events", e)
                        trySend(Result.failure(e))
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                    trySend(Result.failure(Exception(error.message)))
                }
            }
            
            eventsRef.addValueEventListener(listener)
            
            awaitClose {
                eventsRef.removeEventListener(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in observeAllEvents", e)
            close(e)
        }
    }
    
    /**
     * Get all events once (non-reactive)
     */
    suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val snapshot = getEventsReference().get().await()
            val events = snapshot.children.mapNotNull { child ->
                child.getValue(EventDto::class.java)?.toEvent()
            }
            Log.d(TAG, "Retrieved ${events.size} events from Firebase")
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all events", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get event by ID
     */
    suspend fun getEventById(id: String): Result<Event?> {
        return try {
            val snapshot = getEventsReference().child(id).get().await()
            val event = snapshot.getValue(EventDto::class.java)?.toEvent()
            Log.d(TAG, "Retrieved event: $id")
            Result.success(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting event by id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save event to Firebase
     */
    suspend fun saveEvent(event: Event): Result<Unit> {
        return try {
            // Validate event ID format
            if (event.id.isEmpty() || event.id == "{eventId}") {
                val error = Exception("Event ID không hợp lệ: '${event.id}'. ID phải là UUID hợp lệ.")
                Log.e(TAG, "Invalid event ID detected!")
                return Result.failure(error)
            }
            
            // Validate userId
            if (event.userId.isEmpty()) {
                val error = Exception("User ID không được để trống")
                Log.e(TAG, "Empty userId!")
                return Result.failure(error)
            }
            
            // VERIFY TOKEN FIRST
            if (!ensureAuthTokenValid()) {
                val error = Exception("Authentication token không hợp lệ. Vui lòng đăng nhập lại.")
                Log.e(TAG, "Token validation failed before save")
                return Result.failure(error)
            }
            
            val userId = getUserId()
            
            // Double check userId matches
            if (event.userId != userId) {
                Log.w(TAG, "Event userId (${event.userId}) doesn't match current user ($userId). Using current user.")
            }
            
            val eventDto = EventDto.fromEvent(event.copy(userId = userId))
            val eventMap = eventDto.toMap()
            val path = "users/$userId/events/${event.id}"
            
            Log.d(TAG, "============================================")
            Log.d(TAG, "SAVING EVENT TO FIREBASE")
            Log.d(TAG, "Path: $path")
            Log.d(TAG, "Event ID: ${event.id}")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Title: ${event.title}")
            Log.d(TAG, "============================================")
            
            Log.d(TAG, "Event map to be sent to Firebase:")
            Log.d(TAG, "Keys: ${eventMap.keys}")
            eventMap.forEach { (key, value) ->
                Log.d(TAG, "  $key: $value (${value.javaClass.simpleName})")
            }
            
            // Use toMap() to exclude null values from Firebase serialization
            getEventsReference().child(event.id).setValue(eventMap).await()
            
            Log.d(TAG, "✅ Event saved successfully to Firebase!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED TO SAVE EVENT TO FIREBASE")
            Log.e(TAG, "Event ID: ${event.id}")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete event from Firebase
     */
    suspend fun deleteEvent(id: String): Result<Unit> {
        return try {
            getEventsReference().child(id).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update event in Firebase
     */
    suspend fun updateEvent(event: Event): Result<Unit> {
        return try {
            // Validate event ID
            if (event.id.isEmpty() || event.id == "{eventId}") {
                val error = Exception("Event ID không hợp lệ: '${event.id}'")
                Log.e(TAG, "Invalid event ID for update!")
                return Result.failure(error)
            }
            
            // Validate userId
            if (event.userId.isEmpty()) {
                val error = Exception("User ID không được để trống")
                Log.e(TAG, "Empty userId!")
                return Result.failure(error)
            }
            
            // VERIFY TOKEN FIRST
            if (!ensureAuthTokenValid()) {
                val error = Exception("Authentication token không hợp lệ. Vui lòng đăng nhập lại.")
                Log.e(TAG, "Token validation failed before update")
                return Result.failure(error)
            }
            
            val userId = getUserId()
            
            val eventDto = EventDto.fromEvent(
                event.copy(
                    userId = userId,
                    updatedAt = System.currentTimeMillis()
                )
            )
            val eventMap = eventDto.toMap()
            val path = "users/$userId/events/${event.id}"
            
            Log.d(TAG, "============================================")
            Log.d(TAG, "UPDATING EVENT IN FIREBASE")
            Log.d(TAG, "Path: $path")
            Log.d(TAG, "Event ID: ${event.id}")
            Log.d(TAG, "User ID: $userId")
            Log.d(TAG, "Title: ${event.title}")
            Log.d(TAG, "============================================")
            
            Log.d(TAG, "Updating with map:")
            Log.d(TAG, "Keys: ${eventMap.keys}")
            eventMap.forEach { (key, value) ->
                Log.d(TAG, "  $key: $value (${value.javaClass.simpleName})")
            }
            
            // Use toMap() to exclude null values from Firebase serialization
            getEventsReference().child(event.id).setValue(eventMap).await()
            
            Log.d(TAG, "✅ Event updated successfully in Firebase!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED TO UPDATE EVENT IN FIREBASE")
            Log.e(TAG, "Event ID: ${event.id}")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.failure(e)
        }
    }
}

