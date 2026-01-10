package smart.study.planner.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import smart.study.planner.data.local.dao.EventDao
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.SyncStatus
import smart.study.planner.domain.repository.EventRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EventRepositoryImpl"

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val firebaseAuth: FirebaseAuth
) : EventRepository {

    private fun eventsRef() = firebaseAuth.currentUser?.uid?.let { uid ->
        FirebaseDatabase.getInstance().reference.child("users").child(uid).child("events")
    }

    override fun getAllEvents(): Flow<Result<List<Event>>> {
        return flow {
            // ✅ BƯỚC 1: Sync từ Firebase trước
            Log.d(TAG, "🔄 [getAllEvents] Syncing events from Firebase to Local...")
            try {
                syncWithFirebase().getOrThrow()
                Log.d(TAG, "✅ [getAllEvents] Firebase sync completed")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ [getAllEvents] Firebase sync failed: ${e.message}, using local data only", e)
            }
            
            // ✅ BƯỚC 2: Sau đó emit local data
            eventDao.getAllEvents()
                .map { events -> 
                    val userEvents = events.filter { it.userId == firebaseAuth.currentUser?.uid }
                    Log.d(TAG, "📊 [getAllEvents] Loaded ${userEvents.size} events from local (total: ${events.size})")
                    Result.success(userEvents)
                }
                .catch { e -> 
                    Log.e(TAG, "❌ [getAllEvents] Error loading from local: ${e.message}", e)
                    emit(Result.failure(e)) 
                }
                .collect { emit(it) }
        }.flowOn(Dispatchers.IO)
    }

    override fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<Result<List<Event>>> {
        return eventDao.getEventsByDateRange(startDate, endDate)
            .map { events -> Result.success(events.filter { it.userId == firebaseAuth.currentUser?.uid }) }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
    }

    override fun getUpcomingEvents(): Flow<Result<List<Event>>> {
        val currentTime = System.currentTimeMillis()
        return eventDao.getUpcomingEvents(currentTime)
            .map { events -> Result.success(events.filter { it.userId == firebaseAuth.currentUser?.uid }) }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
    }

    override fun getUpcomingEvents(days: Int): Flow<Result<List<Event>>> {
        return flow {
            // ✅ Sync Firebase trước
            Log.d(TAG, "🔄 [getUpcomingEvents] Syncing events from Firebase...")
            try {
                syncWithFirebase().getOrThrow()
                Log.d(TAG, "✅ [getUpcomingEvents] Firebase sync completed")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ [getUpcomingEvents] Sync failed: ${e.message}, using local data only", e)
            }
            
            val currentTime = System.currentTimeMillis()
            val endTime = currentTime + (days * 24 * 60 * 60 * 1000L)
            
            eventDao.getEventsByDateRange(currentTime, endTime)
                .map { events -> 
                    val filtered = events.filter { !it.isCompleted && it.userId == firebaseAuth.currentUser?.uid }
                    Log.d(TAG, "📊 [getUpcomingEvents] Loaded ${filtered.size} upcoming events")
                    Result.success(filtered)
                }
                .catch { e -> 
                    Log.e(TAG, "❌ [getUpcomingEvents] Error loading: ${e.message}", e)
                    emit(Result.failure(e)) 
                }
                .collect { emit(it) }
        }.flowOn(Dispatchers.IO)
    }

    override fun getTodayEvents(): Flow<Result<List<Event>>> {
        val today = LocalDate.now()
        val startTime = today.atStartOfDay().atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val endTime = today.atTime(23, 59, 59).atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        return eventDao.getEventsByDateRange(startTime, endTime)
            .map { events -> Result.success(events.filter { it.userId == firebaseAuth.currentUser?.uid }) }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
    }

    override fun getOverdueEvents(): Flow<Result<List<Event>>> {
        val currentTime = System.currentTimeMillis()
        return eventDao.getAllEvents()
            .map { events ->
                Result.success(events.filter { event ->
                    !event.isCompleted && event.startDateTime < currentTime && event.userId == firebaseAuth.currentUser?.uid
                })
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
    }

    override fun getEventsByCategory(category: String): Flow<Result<List<Event>>> {
        return eventDao.getAllEvents()
            .map { events ->
                Result.success(events.filter { it.category.name.equals(category, ignoreCase = true) && it.userId == firebaseAuth.currentUser?.uid })
            }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getEventById(id: String): Result<Event?> = withContext(Dispatchers.IO) {
        try {
            var event = eventDao.getEventById(id)
            if (event == null) {
                val snapshot = eventsRef()?.child(id)?.get()?.await()
                event = snapshot?.getValue(Event::class.java)
                event?.let { eventDao.insertEvent(it) }
            }
            Result.success(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting event by id: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun saveEvent(event: Event): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            eventDao.insertEvent(event)
            syncEventToFirebase(event).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving event: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun addEvent(event: Event): Result<String> = withContext(Dispatchers.IO) {
        try {
            eventDao.insertEvent(event)
            syncEventToFirebase(event).getOrThrow()
            Result.success(event.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: Event): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            eventDao.updateEvent(event)
            syncEventToFirebase(event).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
            eventDao.deleteEventById(id)
            deleteEventFromFirebase(userId, id).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleEventCompletion(eventId: String): Result<Event> = withContext(Dispatchers.IO) {
        try {
            val event = eventDao.getEventById(eventId)
                ?: return@withContext Result.failure(Exception("Event not found: $eventId"))
            
            Log.d(TAG, "🔄 BEFORE Room update: eventId=$eventId, isCompleted=${event.isCompleted}")
            
            // ✅ CRITICAL: Update Room Database FIRST (synchronous, immediate)
            val updatedEvent = event.copy(
                isCompleted = !event.isCompleted,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            eventDao.updateEvent(updatedEvent)
            
            Log.d(TAG, "✅ AFTER Room update: eventId=$eventId, isCompleted=${updatedEvent.isCompleted}")
            
            // ✅ VERIFY: Confirm Room was actually updated
            val verifyEvent = eventDao.getEventById(eventId)
            Log.d(TAG, "✅ VERIFY from Room: eventId=$eventId, isCompleted=${verifyEvent?.isCompleted}")
            
            if (verifyEvent?.isCompleted != updatedEvent.isCompleted) {
                Log.e(TAG, "❌ CRITICAL: Room update verification failed! Expected: ${updatedEvent.isCompleted}, Got: ${verifyEvent?.isCompleted}")
                return@withContext Result.failure(Exception("Room update verification failed"))
            }
            
            // ✅ THEN sync to Firebase in background (non-blocking, async)
            // This happens on IO dispatcher - doesn't block the caller
            try {
                Log.d(TAG, "🔄 Syncing to Firebase: eventId=$eventId, isCompleted=${updatedEvent.isCompleted}")
                syncEventToFirebase(updatedEvent).getOrThrow()
                
                Log.d(TAG, "✅ Firebase synced: eventId=$eventId")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Firebase sync failed (will retry later): ${e.message}")
                // Don't fail the operation - Room is updated, sync will happen later via syncPendingEvents()
            }
            
            // ✅ Return the updated event to caller for verification
            Result.success(updatedEvent)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Toggle failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun markEventAsCompleted(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val event = eventDao.getEventById(eventId)
            if (event != null) {
                val updatedEvent = event.copy(isCompleted = true, isSynced = false)
                eventDao.updateEvent(updatedEvent)
                syncEventToFirebase(updatedEvent).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingEvents(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val unsyncedEvents = eventDao.getUnsyncedEvents().filter { it.userId == firebaseAuth.currentUser?.uid }
            unsyncedEvents.forEach { event ->
                syncEventToFirebase(event).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncWithFirebase(): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 [syncWithFirebase] Starting Firebase sync for user: $userId")
                
                // ✅ STEP 1: Get all events from Firebase
                val firebaseEvents = getEventsFromFirebase(userId).getOrThrow()
                Log.d(TAG, "📥 [syncWithFirebase] Fetched ${firebaseEvents.size} events from Firebase")
                
                // ✅ STEP 2: Get all local events synchronously (using getAllEventsSync)
                val allLocalEvents = eventDao.getAllEventsSync()
                val userLocalEvents = allLocalEvents.filter { it.userId == userId }
                Log.d(TAG, "📦 [syncWithFirebase] Loaded ${userLocalEvents.size} local events")
                
                val roomEventsMap = userLocalEvents.associateBy { it.id }
                
                var updatedCount = 0
                var skippedCount = 0
                var addedCount = 0
                
                // ✅ STEP 3: Compare Firebase events with Room events using timestamps
                firebaseEvents.forEach { firebaseEvent ->
                    val roomEvent = roomEventsMap[firebaseEvent.id]
                    
                    if (roomEvent == null) {
                        // ✅ NEW EVENT: Add to Room
                        Log.d(TAG, "➕ [syncWithFirebase] Adding new event from Firebase: ${firebaseEvent.title} (ID: ${firebaseEvent.id})")
                        eventDao.insertEvent(firebaseEvent)
                        addedCount++
                        
                    } else {
                        // ✅ EVENT EXISTS: Compare timestamps to determine which is newer
                        val firebaseTime = firebaseEvent.updatedAt
                        val roomTime = roomEvent.updatedAt
                        
                        when {
                            firebaseTime > roomTime -> {
                                // ✅ FIREBASE IS NEWER: Update Room with Firebase data
                                Log.d(TAG, "⬇️ [syncWithFirebase] Firebase is newer: ${firebaseEvent.title}")
                                Log.d(TAG, "   Firebase time: $firebaseTime, Room time: $roomTime")
                                Log.d(TAG, "   Firebase isCompleted: ${firebaseEvent.isCompleted}, Room isCompleted: ${roomEvent.isCompleted}")
                                eventDao.updateEvent(firebaseEvent)
                                updatedCount++
                            }
                            
                            roomTime > firebaseTime -> {
                                // ✅ ROOM IS NEWER: DO NOT UPDATE (keep Room data as source of truth)
                                Log.d(TAG, "⏭️ [syncWithFirebase] Room is NEWER - skipping Firebase version: ${roomEvent.title}")
                                Log.d(TAG, "   Firebase time: $firebaseTime, Room time: $roomTime")
                                Log.d(TAG, "   Firebase isCompleted: ${firebaseEvent.isCompleted}, Room isCompleted: ${roomEvent.isCompleted}")
                                skippedCount++
                                
                                // ✅ CRITICAL: If Room data is not synced yet, push it to Firebase
                                if (!roomEvent.isSynced) {
                                    Log.d(TAG, "🔄 [syncWithFirebase] Pushing unsync'd Room data to Firebase: ${roomEvent.title}")
                                    try {
                                        syncEventToFirebase(roomEvent).getOrThrow()
                                        Log.d(TAG, "✅ [syncWithFirebase] Pushed Room data to Firebase successfully")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "⚠️ [syncWithFirebase] Failed to push Room data to Firebase: ${e.message}")
                                    }
                                }
                            }
                            
                            else -> {
                                // Same timestamp - do nothing
                                Log.d(TAG, "⏭️ [syncWithFirebase] Same timestamp - skipping: ${roomEvent.title}")
                                skippedCount++
                            }
                        }
                    }
                }
                
                Log.d(TAG, "✅ [syncWithFirebase] Completed: Added=$addedCount, Updated=$updatedCount, Skipped=$skippedCount")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [syncWithFirebase] Failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override fun observeSyncStatus(): Flow<SyncStatus> {
        return flow { emit(SyncStatus.IDLE) }.flowOn(Dispatchers.IO)
    }

    private suspend fun getEventsFromFirebase(userId: String): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = eventsRef()?.get()?.await()
            val events = snapshot?.children?.mapNotNull { it.getValue(Event::class.java) } ?: emptyList()
            if (events.isEmpty()) {
                return@withContext getEventsByUserId(userId)
            }
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting events from Firebase: ${e.message}", e)
            getEventsByUserId(userId)
        }
    }

    private fun getEventsByUserId(userId: String): Result<List<Event>> {
        return try {
            var events: List<Event> = emptyList()
            eventDao.getAllEvents().map { allEvents ->
                events = allEvents.filter { it.userId == userId }
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncEventToFirebase(event: Event): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            eventsRef()?.child(event.id)?.setValue(event)?.await()
            eventDao.markAsSynced(listOf(event.id))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing event to Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteEventFromFirebase(userId: String, eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth.currentUser?.uid != userId) {
                throw SecurityException("Unauthorized to delete this event.")
            }
            eventsRef()?.child(eventId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event from Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }
}
