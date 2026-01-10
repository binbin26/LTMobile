package smart.study.planner.data.local.dao

import androidx.room.*
import smart.study.planner.data.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Event entity
 * Provides methods to interact with the local Room database
 */
@Dao
interface EventDao {
    
    /**
     * Get all events as a Flow for reactive updates
     */
    @Query("SELECT * FROM events ORDER BY startDateTime ASC")
    fun getAllEvents(): Flow<List<Event>>
    
    /**
     * Get events by date range
     */
    @Query("SELECT * FROM events WHERE startDateTime >= :startDate AND startDateTime <= :endDate ORDER BY startDateTime ASC")
    fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<List<Event>>
    
    /**
     * Get events that are not synced (pending sync)
     */
    @Query("SELECT * FROM events WHERE isSynced = 0")
    suspend fun getUnsyncedEvents(): List<Event>
    
    /**
     * Get event by ID
     */
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: String): Event?
    
    /**
     * Get upcoming events (not completed and startDateTime >= current time)
     */
    @Query("SELECT * FROM events WHERE isCompleted = 0 AND startDateTime >= :currentTime ORDER BY startDateTime ASC")
    fun getUpcomingEvents(currentTime: Long): Flow<List<Event>>
    
    /**
     * Insert a single event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)
    
    /**
     * Insert multiple events
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)
    
    /**
     * Update an event
     */
    @Update
    suspend fun updateEvent(event: Event)
    
    /**
     * Delete an event
     */
    @Delete
    suspend fun deleteEvent(event: Event)
    
    /**
     * Delete event by ID
     */
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: String)
    
    /**
     * Mark events as synced
     */
    @Query("UPDATE events SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
    
    /**
     * Get all events synchronously (for sync operations that need immediate access)
     * ✅ CRITICAL: Used in syncWithFirebase to compare timestamps
     */
    @Query("SELECT * FROM events ORDER BY startDateTime ASC")
    suspend fun getAllEventsSync(): List<Event>
}

