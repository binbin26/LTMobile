package smart.study.planner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import smart.study.planner.data.model.CalendarViewMode
import smart.study.planner.data.model.Event
import smart.study.planner.domain.repository.EventRepository
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * ViewModel for Calendar screen
 * Handles calendar navigation, date selection, and event display
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "CalendarViewModel"
    }
    
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()
    
    private val _viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()
    
    private val _eventsInMonth = MutableStateFlow<Map<LocalDate, List<Event>>>(emptyMap())
    val eventsInMonth: StateFlow<Map<LocalDate, List<Event>>> = _eventsInMonth.asStateFlow()
    
    private val _selectedDateEvents = MutableStateFlow<List<Event>>(emptyList())
    val selectedDateEvents: StateFlow<List<Event>> = _selectedDateEvents.asStateFlow()
    
    private val _upcomingEvents = MutableStateFlow<List<Event>>(emptyList())
    val upcomingEvents: StateFlow<List<Event>> = _upcomingEvents.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadEventsForMonth(YearMonth.now())
        loadUpcomingEvents()
        observeSelectedDateEvents()
    }
    
    /**
     * Select a date
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        // Update month if needed
        val dateMonth = YearMonth.from(date)
        if (dateMonth != _selectedMonth.value) {
            _selectedMonth.value = dateMonth
            loadEventsForMonth(dateMonth)
        }
    }
    
    /**
     * Change month (offset: +1 for next, -1 for previous)
     */
    fun changeMonth(offset: Int) {
        val newMonth = _selectedMonth.value.plusMonths(offset.toLong())
        _selectedMonth.value = newMonth
        loadEventsForMonth(newMonth)
    }
    
    /**
     * Switch between month and week view
     */
    fun switchViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }
    
    /**
     * Load events for a specific month
     */
    fun loadEventsForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val startDate = yearMonth.atDay(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            val endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            
            eventRepository.getEventsByDateRange(startDate, endDate)
                .catch { e ->
                    _errorMessage.value = e.message ?: "Failed to load events"
                    _isLoading.value = false
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            // Group events by date
                            val eventsByDate = events.groupBy { event ->
                                java.time.Instant.ofEpochMilli(event.startDateTime)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            _eventsInMonth.value = eventsByDate
                            _isLoading.value = false
                        },
                        onFailure = { e ->
                            _errorMessage.value = e.message ?: "Failed to load events"
                            _isLoading.value = false
                        }
                    )
                }
        }
    }
    
    /**
     * Get events by date range
     */
    fun getEventsByDateRange(startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            val startTimestamp = startDate.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            val endTimestamp = endDate.atTime(23, 59, 59).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
            
            eventRepository.getEventsByDateRange(startTimestamp, endTimestamp)
                .catch { e ->
                    _errorMessage.value = e.message ?: "Failed to load events"
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            val eventsByDate = events.groupBy { event ->
                                java.time.Instant.ofEpochMilli(event.startDateTime)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                            _eventsInMonth.value = eventsByDate
                        },
                        onFailure = { e ->
                            _errorMessage.value = e.message ?: "Failed to load events"
                        }
                    )
                }
        }
    }
    
    /**
     * Observe events for selected date
     */
    private fun observeSelectedDateEvents() {
        viewModelScope.launch {
            combine(
                _selectedDate,
                _eventsInMonth
            ) { date, eventsMap ->
                eventsMap[date] ?: emptyList()
            }.collect { events ->
                _selectedDateEvents.value = events
            }
        }
    }
    
    /**
     * Load upcoming events
     */
    private fun loadUpcomingEvents() {
        viewModelScope.launch {
            eventRepository.getUpcomingEvents(7) // Next 7 days
                .catch { e ->
                    // Silent fail for upcoming events
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            _upcomingEvents.value = events
                        },
                        onFailure = {
                            // Silent fail
                        }
                    )
                }
        }
    }
    
    /**
     * Delete an event
     */
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
                .fold(
                    onSuccess = {
                        // Reload events
                        loadEventsForMonth(_selectedMonth.value)
                        loadUpcomingEvents()
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Failed to delete event"
                    }
                )
        }
    }
    
    /**
     * Toggle event completion
     */
    fun toggleEventCompletion(eventId: String) {
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(eventId)
                .fold(
                    onSuccess = { updatedEvent ->
                        Log.d(TAG, "✅ Toggle persisted to Room: eventId=$eventId, isCompleted=${updatedEvent.isCompleted}")
                        // Reload events to reflect changes
                        loadEventsForMonth(_selectedMonth.value)
                        loadUpcomingEvents()
                    },
                    onFailure = { e ->
                        Log.e(TAG, "❌ Failed to toggle event completion: ${e.message}", e)
                        _errorMessage.value = e.message ?: "Failed to update event"
                    }
                )
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

