package smart.study.planner.domain.repository

import smart.study.planner.data.model.Event
import smart.study.planner.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Event operations
 * Defines the contract for event data access
 */
interface EventRepository {
    
    /**
     * Get all events as a Flow
     */
    fun getAllEvents(): Flow<Result<List<Event>>>
    
    /**
     * Get events by date range
     */
    fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<Result<List<Event>>>
    
    /**
     * Get upcoming events (not completed, date >= current time)
     */
    fun getUpcomingEvents(): Flow<Result<List<Event>>>
    
    /**
     * Get upcoming events within specified days
     */
    fun getUpcomingEvents(days: Int): Flow<Result<List<Event>>>
    
    /**
     * Get today's events
     */
    fun getTodayEvents(): Flow<Result<List<Event>>>
    
    /**
     * Get overdue events (not completed, date < current time)
     */
    fun getOverdueEvents(): Flow<Result<List<Event>>>
    
    /**
     * Get events by category
     */
    fun getEventsByCategory(category: String): Flow<Result<List<Event>>>
    
    /**
     * Get event by ID
     */
    suspend fun getEventById(id: String): Result<Event?>
    
    /**
     * Save a new event
     */
    suspend fun saveEvent(event: Event): Result<Unit>
    
    /**
     * Add a new event (alias for saveEvent)
     */
    suspend fun addEvent(event: Event): Result<String>
    
    /**
     * Update an existing event
     */
    suspend fun updateEvent(event: Event): Result<Unit>
    
    /**
     * Delete an event
     */
    suspend fun deleteEvent(id: String): Result<Unit>
    
    /**
     * Toggle event completion status
     * Returns the updated Event to ensure Room was updated successfully
     */
    suspend fun toggleEventCompletion(eventId: String): Result<Event>
    
    /**
     * Mark event as completed
     */
    suspend fun markEventAsCompleted(eventId: String): Result<Unit>
    
    /**
     * Sync pending events to Firebase
     */
    suspend fun syncPendingEvents(): Result<Unit>
    
    /**
     * Pull events from Firebase and update local database
     */
    suspend fun syncWithFirebase(): Result<Unit>
    
    /**
     * Observe sync status
     */
    fun observeSyncStatus(): Flow<SyncStatus>
}

