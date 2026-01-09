package smart.study.planner.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
        return eventDao.getAllEvents()
            .map { events -> Result.success(events.filter { it.userId == firebaseAuth.currentUser?.uid }) }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
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
        val currentTime = System.currentTimeMillis()
        val endTime = currentTime + (days * 24 * 60 * 60 * 1000L)
        return eventDao.getEventsByDateRange(currentTime, endTime)
            .map { events -> Result.success(events.filter { !it.isCompleted && it.userId == firebaseAuth.currentUser?.uid }) }
            .catch { e -> emit(Result.failure(e)) }
            .flowOn(Dispatchers.IO)
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

    override suspend fun toggleEventCompletion(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val event = eventDao.getEventById(eventId)
            if (event != null) {
                val updatedEvent = event.copy(isCompleted = !event.isCompleted, isSynced = false)
                eventDao.updateEvent(updatedEvent)
                syncEventToFirebase(updatedEvent).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
                val firebaseEvents = getEventsFromFirebase(userId).getOrThrow()
                eventDao.getAllEvents().collect { localEvents ->
                    val eventsToDelete = localEvents.filter { it.userId == userId }
                    eventsToDelete.forEach { eventDao.deleteEvent(it) }
                }
                firebaseEvents.forEach { event ->
                    eventDao.insertEvent(event)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error in syncFromFirebase: ${e.message}", e)
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
