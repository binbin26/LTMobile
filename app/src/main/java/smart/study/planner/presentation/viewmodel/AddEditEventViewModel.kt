package smart.study.planner.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventCategory
import smart.study.planner.data.model.EventPriority
import smart.study.planner.domain.repository.EventRepository
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Event screen
 * Handles form state and validation
 */
@HiltViewModel
class AddEditEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val eventId: String? = savedStateHandle.get<String>("eventId")
    val isEditMode: Boolean = eventId != null && eventId != "new"
    
    // Form fields
    private val _eventTitle = MutableStateFlow("")
    val eventTitle: StateFlow<String> = _eventTitle.asStateFlow()
    
    private val _eventDescription = MutableStateFlow("")
    val eventDescription: StateFlow<String> = _eventDescription.asStateFlow()
    
    private val _startDateTime = MutableStateFlow(LocalDateTime.now())
    val startDateTime: StateFlow<LocalDateTime> = _startDateTime.asStateFlow()
    
    private val _endDateTime = MutableStateFlow(LocalDateTime.now().plusHours(1))
    val endDateTime: StateFlow<LocalDateTime> = _endDateTime.asStateFlow()
    
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()
    
    private val _category = MutableStateFlow(EventCategory.STUDY)
    val category: StateFlow<EventCategory> = _category.asStateFlow()
    
    private val _priority = MutableStateFlow(EventPriority.MEDIUM)
    val priority: StateFlow<EventPriority> = _priority.asStateFlow()
    
    private val _isReminderEnabled = MutableStateFlow(false)
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()
    
    private val _reminderTime = MutableStateFlow(15) // minutes before
    val reminderTime: StateFlow<Int> = _reminderTime.asStateFlow()
    
    private val _isAllDayEvent = MutableStateFlow(false)
    val isAllDayEvent: StateFlow<Boolean> = _isAllDayEvent.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        if (isEditMode && eventId != null) {
            loadEvent(eventId)
        }
    }
    
    /**
     * Load event for editing
     */
    private fun loadEvent(id: String) {
        viewModelScope.launch {
            eventRepository.getEventById(id)
                .fold(
                    onSuccess = { event ->
                        event?.let {
                            _eventTitle.value = it.title
                            _eventDescription.value = it.description
                            _startDateTime.value = java.time.Instant.ofEpochMilli(it.startDateTime)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                            it.endDateTime?.let { end ->
                                _endDateTime.value = java.time.Instant.ofEpochMilli(end)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                            }
                            _location.value = it.location
                            _category.value = it.category
                            _priority.value = it.priority
                            _isReminderEnabled.value = it.reminderEnabled
                            _reminderTime.value = it.reminderMinutes
                            _isAllDayEvent.value = it.isAllDay
                        }
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Failed to load event"
                    }
                )
        }
    }
    
    /**
     * Update form fields
     */
    fun updateTitle(title: String) {
        _eventTitle.value = title
        validateField("title", title)
    }
    
    fun updateDescription(description: String) {
        _eventDescription.value = description
        validateField("description", description)
    }
    
    fun updateStartDateTime(dateTime: LocalDateTime) {
        _startDateTime.value = dateTime
        // Auto-adjust end time if it's before start time
        if (_endDateTime.value.isBefore(dateTime) || _endDateTime.value.isEqual(dateTime)) {
            _endDateTime.value = dateTime.plusHours(1)
        }
        validateDateTimeRange()
    }
    
    fun updateEndDateTime(dateTime: LocalDateTime) {
        _endDateTime.value = dateTime
        validateDateTimeRange()
    }
    
    fun updateLocation(location: String) {
        _location.value = location
        validateField("location", location)
    }
    
    fun updateCategory(category: EventCategory) {
        _category.value = category
    }
    
    fun updatePriority(priority: EventPriority) {
        _priority.value = priority
    }
    
    fun toggleReminder(enabled: Boolean) {
        _isReminderEnabled.value = enabled
    }
    
    fun updateReminderTime(minutes: Int) {
        _reminderTime.value = minutes.coerceIn(0, 1440) // Max 24 hours
    }
    
    fun toggleAllDay(isAllDay: Boolean) {
        _isAllDayEvent.value = isAllDay
    }
    
    /**
     * Validate form
     */
    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        
        // Title validation
        if (_eventTitle.value.isBlank()) {
            errors["title"] = "Tiêu đề không được để trống"
        } else if (_eventTitle.value.length > 200) {
            errors["title"] = "Tiêu đề không được quá 200 ký tự"
        }
        
        // Description validation
        if (_eventDescription.value.length > 1000) {
            errors["description"] = "Mô tả không được quá 1000 ký tự"
        }
        
        // Location validation
        if (_location.value.length > 200) {
            errors["location"] = "Địa điểm không được quá 200 ký tự"
        }
        
        // DateTime validation
        if (_endDateTime.value.isBefore(_startDateTime.value)) {
            errors["endDateTime"] = "Thời gian kết thúc phải sau thời gian bắt đầu"
        }
        
        _validationErrors.value = errors
        return errors.isEmpty()
    }
    
    /**
     * Validate individual field
     */
    private fun validateField(fieldName: String, value: String) {
        val errors = _validationErrors.value.toMutableMap()
        
        when (fieldName) {
            "title" -> {
                if (value.isBlank()) {
                    errors["title"] = "Tiêu đề không được để trống"
                } else if (value.length > 200) {
                    errors["title"] = "Tiêu đề không được quá 200 ký tự"
                } else {
                    errors.remove("title")
                }
            }
            "description" -> {
                if (value.length > 1000) {
                    errors["description"] = "Mô tả không được quá 1000 ký tự"
                } else {
                    errors.remove("description")
                }
            }
            "location" -> {
                if (value.length > 200) {
                    errors["location"] = "Địa điểm không được quá 200 ký tự"
                } else {
                    errors.remove("location")
                }
            }
        }
        
        _validationErrors.value = errors
    }
    
    /**
     * Validate date time range
     */
    private fun validateDateTimeRange() {
        val errors = _validationErrors.value.toMutableMap()
        
        if (_endDateTime.value.isBefore(_startDateTime.value)) {
            errors["endDateTime"] = "Thời gian kết thúc phải sau thời gian bắt đầu"
        } else {
            errors.remove("endDateTime")
        }
        
        _validationErrors.value = errors
    }
    
    /**
     * Save event
     */
    fun saveEvent() {
        if (!validateForm()) {
            return
        }
        
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            
            val startTimestamp = _startDateTime.value.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            val endTimestamp = _endDateTime.value.atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            
            val event = Event(
                id = eventId ?: "",
                title = _eventTitle.value.trim(),
                description = _eventDescription.value.trim(),
                startDateTime = startTimestamp,
                endDateTime = endTimestamp,
                location = _location.value.trim(),
                category = _category.value,
                priority = _priority.value,
                isAllDay = _isAllDayEvent.value,
                reminderEnabled = _isReminderEnabled.value,
                reminderMinutes = _reminderTime.value,
                colorHex = getColorForCategory(_category.value, _priority.value)
            )
            
            val result = if (isEditMode) {
                eventRepository.updateEvent(event)
            } else {
                eventRepository.addEvent(event)
            }
            
            result.fold(
                onSuccess = {
                    _saveSuccess.value = true
                    _isSaving.value = false
                },
                onFailure = { e ->
                    _errorMessage.value = e.message ?: "Failed to save event"
                    _isSaving.value = false
                }
            )
        }
    }
    
    /**
     * Delete event
     */
    fun deleteEvent() {
        if (eventId == null) return
        
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
                .fold(
                    onSuccess = {
                        _saveSuccess.value = true
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Failed to delete event"
                    }
                )
        }
    }
    
    /**
     * Get color hex for category and priority
     */
    private fun getColorForCategory(category: EventCategory, priority: EventPriority): String {
        return when (category) {
            EventCategory.STUDY -> when (priority) {
                EventPriority.HIGH -> "#4CAF50"
                EventPriority.MEDIUM -> "#8BC34A"
                EventPriority.LOW -> "#CDDC39"
            }
            EventCategory.ASSIGNMENT -> when (priority) {
                EventPriority.HIGH -> "#2196F3"
                EventPriority.MEDIUM -> "#64B5F6"
                EventPriority.LOW -> "#90CAF9"
            }
            EventCategory.EXAM -> when (priority) {
                EventPriority.HIGH -> "#F44336"
                EventPriority.MEDIUM -> "#E57373"
                EventPriority.LOW -> "#EF9A9A"
            }
            EventCategory.SEMINAR -> when (priority) {
                EventPriority.HIGH -> "#FF9800"
                EventPriority.MEDIUM -> "#FFB74D"
                EventPriority.LOW -> "#FFCC80"
            }
            EventCategory.WORKSHOP -> when (priority) {
                EventPriority.HIGH -> "#9C27B0"
                EventPriority.MEDIUM -> "#BA68C8"
                EventPriority.LOW -> "#CE93D8"
            }
            EventCategory.OTHER -> "#9E9E9E"
        }
    }
    
    /**
     * Reset save success state
     */
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

