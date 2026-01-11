package smart.study.planner.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.Holiday
import smart.study.planner.data.model.Motivation
import smart.study.planner.data.repository.HolidayRepository
import smart.study.planner.data.repository.MotivationRepository
import smart.study.planner.domain.repository.AuthRepository
import java.time.LocalDate
import smart.study.planner.domain.usecase.DeleteEventUseCase
import smart.study.planner.domain.usecase.GetAllEventsUseCase
import smart.study.planner.domain.usecase.GetUpcomingEventsUseCase
import smart.study.planner.domain.usecase.SaveEventUseCase
import smart.study.planner.domain.usecase.UpdateEventUseCase
import smart.study.planner.domain.usecase.ToggleEventCompletionUseCase
import smart.study.planner.domain.usecase.SyncPendingEventsUseCase
import smart.study.planner.presentation.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import smart.study.planner.data.local.notification.ReminderScheduler
import javax.inject.Inject

private const val TAG = "EventViewModel"

/**
 * ViewModel for Event management
 * Handles UI state and business logic for events
 * Manages offline-first data loading from the local database.
 */
@HiltViewModel
class EventViewModel @Inject constructor(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getUpcomingEventsUseCase: GetUpcomingEventsUseCase,
    private val saveEventUseCase: SaveEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val toggleEventCompletionUseCase: ToggleEventCompletionUseCase,
    private val syncPendingEventsUseCase: SyncPendingEventsUseCase,
    private val authRepository: AuthRepository,
    private val holidayRepository: HolidayRepository,
    private val motivationRepository: MotivationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _eventsState = MutableStateFlow<UiState<List<Event>>>(UiState.Idle)
    val eventsState: StateFlow<UiState<List<Event>>> = _eventsState.asStateFlow()

    private val _upcomingEventsState = MutableStateFlow<UiState<List<Event>>>(UiState.Idle)
    val upcomingEventsState: StateFlow<UiState<List<Event>>> = _upcomingEventsState.asStateFlow()

    private val _saveEventState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveEventState: StateFlow<UiState<Unit>> = _saveEventState.asStateFlow()

    private val _deleteEventState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteEventState: StateFlow<UiState<Unit>> = _deleteEventState.asStateFlow()

    private val _updateEventState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val updateEventState: StateFlow<UiState<Unit>> = _updateEventState.asStateFlow()

    private val _holidaysState = MutableStateFlow<UiState<List<Holiday>>>(UiState.Idle)
    val holidaysState: StateFlow<UiState<List<Holiday>>> = _holidaysState.asStateFlow()

    private val _motivationsState = MutableStateFlow<UiState<List<Motivation>>>(UiState.Idle)
    val motivationsState: StateFlow<UiState<List<Motivation>>> = _motivationsState.asStateFlow()

    private val _randomMotivation = MutableStateFlow<Motivation?>(null)
    val randomMotivation: StateFlow<Motivation?> = _randomMotivation.asStateFlow()

    init {
        Log.d(TAG, "EventViewModel initialized")
        if (authRepository.isUserLoggedIn()) {
            Log.d(TAG, "User logged in, loading data")
            initializeData()
        } else {
            Log.w(TAG, "User not logged in, skipping data load")
            _eventsState.value = UiState.Idle
            _upcomingEventsState.value = UiState.Idle
        }
    }
    
    fun initializeData() {
        Log.d(TAG, "Initializing data...")
        loadEvents()
        loadUpcomingEvents()
        syncPendingEvents()
    }

    fun loadEvents() {
        Log.d(TAG, "Loading all events")
        viewModelScope.launch {
            _eventsState.value = UiState.Loading
            getAllEventsUseCase()
                .distinctUntilChanged()
                .catch { e ->
                    Log.e(TAG, "Error loading events: ${e.message}", e)
                    _eventsState.value = UiState.Error(e)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            Log.d(TAG, "Successfully loaded ${events.size} events")
                            _eventsState.value = UiState.Success(events)
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Failure loading events: ${e.message}", e)
                            _eventsState.value = UiState.Error(e)
                        }
                    )
                }
        }
    }

    fun loadUpcomingEvents() {
        Log.d(TAG, "Loading upcoming events")
        viewModelScope.launch {
            _upcomingEventsState.value = UiState.Loading
            getUpcomingEventsUseCase()
                .distinctUntilChanged()
                .catch { e ->
                    Log.e(TAG, "Error loading upcoming events: ${e.message}", e)
                    _upcomingEventsState.value = UiState.Error(e)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            Log.d(TAG, "Successfully loaded ${events.size} upcoming events")
                            _upcomingEventsState.value = UiState.Success(events)
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Failure loading upcoming events: ${e.message}", e)
                            _upcomingEventsState.value = UiState.Error(e)
                        }
                    )
                }
        }
    }

    fun saveEvent(event: Event) {
        viewModelScope.launch {
            _saveEventState.value = UiState.Loading
            if (event.title.isBlank()) {
                _saveEventState.value = UiState.Error(Exception("Title cannot be empty."))
                return@launch
            }
            // Defensive ID validation
            if (event.id.contains("{") || event.id.contains("?") || event.id.contains("}")) {
                val ex = Exception("Invalid event ID format: ${event.id}")
                Log.e(TAG, "Malformed event ID detected in saveEvent: ${event.id}")
                _saveEventState.value = UiState.Error(ex)
                return@launch
            }
            try {
                saveEventUseCase(event)
                Log.d(TAG, "Event saved successfully!")
                _saveEventState.value = UiState.Success(Unit)
                forceRefreshEvents()
            } catch (e: Exception) {
                Log.e(TAG, "Save event failed", e)
                _saveEventState.value = UiState.Error(e)
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            _updateEventState.value = UiState.Loading
            // Defensive ID validation
            if (event.id.contains("{") || event.id.contains("?") || event.id.contains("}")) {
                val ex = Exception("Invalid event ID format: ${event.id}")
                Log.e(TAG, "Malformed event ID detected in updateEvent: ${event.id}")
                _updateEventState.value = UiState.Error(ex)
                return@launch
            }
            try {
                updateEventUseCase(event)
                Log.d(TAG, "Event updated successfully: ${event.id}")
                _updateEventState.value = UiState.Success(Unit)
                forceRefreshEvents()
            } catch (e: Exception) {
                Log.e(TAG, "Update event failed", e)
                _updateEventState.value = UiState.Error(e)
            }
        }
    }

    fun deleteEvent(eventId: String) {
        Log.d(TAG, "Deleting event: $eventId")
        viewModelScope.launch {
            _deleteEventState.value = UiState.Loading
            try {
                deleteEventUseCase(eventId)
                Log.d(TAG, "Event deleted successfully: $eventId")
                _deleteEventState.value = UiState.Success(Unit)
                forceRefreshEvents()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting event", e)
                _deleteEventState.value = UiState.Error(e)
            }
        }
    }

    fun toggleEventCompletion(eventId: String) {
        Log.d(TAG, "Toggling event completion: $eventId")
        viewModelScope.launch {
            // ✅ Step 1: Optimistic UI update (immediate visual feedback)
            val currentEvents = (_eventsState.value as? UiState.Success)?.data
            val updatedEvents = currentEvents?.map { event ->
                if (event.id == eventId) {
                    Log.d(TAG, "🔄 Optimistic UI update: eventId=$eventId, isCompleted will toggle")
                    event.copy(isCompleted = !event.isCompleted)
                } else event
            }
            if (updatedEvents != null) {
                _eventsState.value = UiState.Success(updatedEvents)
                Log.d(TAG, "✅ Optimistic UI updated")
            }

            try {
                // ✅ Step 2: Call UseCase and WAIT for Room update to complete
                Log.d(TAG, "🔄 Calling toggleEventCompletionUseCase for eventId=$eventId")
                val result = toggleEventCompletionUseCase(eventId)
                
                result.fold(
                    onSuccess = { updatedEvent ->
                        Log.d(TAG, "✅ Toggle completed and persisted to Room: eventId=$eventId, isCompleted=${updatedEvent.isCompleted}")
                        // ✅ Room is updated, Firebase sync is in progress or queued
                        // No need to reload immediately - optimistic UI already shows correct state
                        // Future loads will get data from Room (source of truth)
                    },
                    onFailure = { e ->
                        Log.e(TAG, "❌ Toggle UseCase returned failure: ${e.message}", e)
                        // ✅ Rollback UI by reloading from authoritative Room source
                        forceRefreshEvents()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Toggle failed with exception: ${e.message}", e)
                // ✅ Rollback UI by reloading from authoritative source
                forceRefreshEvents()
            }
        }
    }

    private fun syncPendingEvents() {
        Log.d(TAG, "Attempting to sync pending events to Firebase")
        viewModelScope.launch {
            try {
                syncPendingEventsUseCase()
                Log.d(TAG, "Pending events synced successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync pending events", e)
            }
        }
    }

    fun forceRefreshEvents() {
        Log.d(TAG, "Force refreshing all events...")
        loadEvents()
        loadUpcomingEvents()
    }
    
    fun clearSaveEventState() {
        _saveEventState.value = UiState.Idle
    }
    
    fun clearDeleteEventState() {
        _deleteEventState.value = UiState.Idle
    }

    fun clearUpdateEventState() {
        _updateEventState.value = UiState.Idle
    }

    fun loadHolidays() {
        Log.d(TAG, "Loading holidays from API...")
        viewModelScope.launch {
            _holidaysState.value = UiState.Loading
            try {
                val result = holidayRepository.getHolidays()
                result.fold(
                    onSuccess = { holidays ->
                        Log.d(TAG, "Successfully loaded ${holidays.size} holidays")
                        _holidaysState.value = UiState.Success(holidays)
                    },
                    onFailure = { e ->
                         Log.e(TAG, "Error loading holidays", e)
                        _holidaysState.value = UiState.Error(e)
                    }
                )
            } catch (e: Exception) {
                 Log.e(TAG, "Exception while loading holidays", e)
                _holidaysState.value = UiState.Error(e)
            }
        }
    }

    fun getHolidayForDate(date: LocalDate): Holiday? {
        return (_holidaysState.value as? UiState.Success)?.data?.firstOrNull { it.date == date.toString() }
    }

    fun loadMotivations() {
        Log.d(TAG, "Loading motivations from API...")
        viewModelScope.launch {
            _motivationsState.value = UiState.Loading
            try {
                val result = motivationRepository.getMotivations()
                result.fold(
                    onSuccess = { motivations ->
                        Log.d(TAG, "Successfully loaded ${motivations.size} motivations")
                        _motivationsState.value = UiState.Success(motivations)
                        if (_randomMotivation.value == null && motivations.isNotEmpty()) {
                            _randomMotivation.value = motivations.random()
                        }
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Error loading motivations", e)
                        _motivationsState.value = UiState.Error(e)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception while loading motivations", e)
                _motivationsState.value = UiState.Error(e)
            }
        }
    }

    fun getRandomMotivation(): Motivation? {
        _randomMotivation.value?.let { return it }
        
        return when (val state = _motivationsState.value) {
            is UiState.Success -> state.data.randomOrNull()?.also { _randomMotivation.value = it }
            else -> motivationRepository.getRandomMotivation()?.also { _randomMotivation.value = it }
        }
    }
    
    fun refreshRandomMotivation() {
        Log.d(TAG, "Refreshing random motivation")
        // This ensures we don't try to refresh if the list is empty or not loaded
        (_motivationsState.value as? UiState.Success)?.data?.takeIf { it.isNotEmpty() }?.let {
             _randomMotivation.value = it.random()
        }
    }

    /**
     * ✅ DEBUG: Check and log scheduled notifications status
     * Call this from UI to debug notification system
     */
    fun debugCheckNotifications() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Debug: Checking scheduled notifications...")
                ReminderScheduler.checkScheduledNotificationsSync(context)

                // Get detailed status string
                val status = ReminderScheduler.getScheduledWorksStatusSync(context)
                Log.d(TAG, "Scheduled works status:\n$status")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notifications: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ DEBUG: Send test notifications immediately
     * Useful for testing the notification system
     */
    fun testNotifications() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Debug: Sending test notifications...")
                ReminderScheduler.testDeadlineNotification(context, "test-123", "Test Deadline Event")
                ReminderScheduler.testDeadlineExpiredNotification(context, "test-456", "Test Expired Event")
                Log.d(TAG, "✅ Test notifications sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test notifications: ${e.message}", e)
            }
        }
    }

    /**
     * ✅ DEBUG: Cancel all scheduled notifications (for testing/debugging only)
     * WARNING: This will remove all pending notifications!
     */
    fun debugCancelAllNotifications() {
        try {
            Log.d(TAG, "Debug: Cancelling all scheduled notifications...")
            ReminderScheduler.cancelAllScheduledNotifications(context)
            Log.d(TAG, "✅ All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notifications: ${e.message}", e)
        }
    }
}
