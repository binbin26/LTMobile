package smart.study.planner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.TaskFilter
import smart.study.planner.data.model.TaskSortOption
import smart.study.planner.domain.repository.EventRepository
import javax.inject.Inject

/**
 * ViewModel for Task List screen
 * Handles task filtering, sorting, and status management
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {
    
    private val _allTasks = MutableStateFlow<List<Event>>(emptyList())
    val allTasks: StateFlow<List<Event>> = _allTasks.asStateFlow()
    
    private val _todayTasks = MutableStateFlow<List<Event>>(emptyList())
    val todayTasks: StateFlow<List<Event>> = _todayTasks.asStateFlow()
    
    private val _upcomingTasks = MutableStateFlow<List<Event>>(emptyList())
    val upcomingTasks: StateFlow<List<Event>> = _upcomingTasks.asStateFlow()
    
    private val _overdueTasks = MutableStateFlow<List<Event>>(emptyList())
    val overdueTasks: StateFlow<List<Event>> = _overdueTasks.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()
    
    private val _tasksByCategory = MutableStateFlow<Map<String, List<Event>>>(emptyMap())
    val tasksByCategory: StateFlow<Map<String, List<Event>>> = _tasksByCategory.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadTasks()
    }
    
    /**
     * Load all tasks
     */
    fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            eventRepository.getAllEvents()
                .catch { e ->
                    _errorMessage.value = e.message ?: "Failed to load tasks"
                    _isLoading.value = false
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            _allTasks.value = events.filter { !it.isCompleted }
                            updateFilteredTasks()
                            updateTasksByCategory()
                            _isLoading.value = false
                        },
                        onFailure = { e ->
                            _errorMessage.value = e.message ?: "Failed to load tasks"
                            _isLoading.value = false
                        }
                    )
                }
        }
        
        // Load today's tasks
        viewModelScope.launch {
            eventRepository.getTodayEvents()
                .catch { }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            _todayTasks.value = events.filter { !it.isCompleted }
                        },
                        onFailure = { }
                    )
                }
        }
        
        // Load upcoming tasks
        viewModelScope.launch {
            eventRepository.getUpcomingEvents(7)
                .catch { }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            _upcomingTasks.value = events.filter { !it.isCompleted }
                        },
                        onFailure = { }
                    )
                }
        }
        
        // Load overdue tasks
        viewModelScope.launch {
            eventRepository.getOverdueEvents()
                .catch { }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            _overdueTasks.value = events.filter { !it.isCompleted }
                        },
                        onFailure = { }
                    )
                }
        }
    }
    
    /**
     * Filter tasks
     */
    fun filterTasks(filter: TaskFilter) {
        _selectedFilter.value = filter
        updateFilteredTasks()
    }
    
    /**
     * Update filtered tasks based on current filter
     */
    private fun updateFilteredTasks() {
        val tasks = when (_selectedFilter.value) {
            TaskFilter.DUE_TODAY -> _todayTasks.value
            TaskFilter.DUE_LATER -> _upcomingTasks.value
            TaskFilter.ALL -> _allTasks.value
        }
        // Tasks are already filtered in loadTasks()
    }
    
    /**
     * Update tasks grouped by category
     */
    private fun updateTasksByCategory() {
        val grouped = _allTasks.value.groupBy { it.category.name }
        _tasksByCategory.value = grouped
    }
    
    /**
     * Toggle task completion
     */
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            eventRepository.toggleEventCompletion(taskId)
                .fold(
                    onSuccess = {
                        loadTasks() // Reload tasks
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Failed to update task"
                    }
                )
        }
    }
    
    /**
     * Delete task
     */
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(taskId)
                .fold(
                    onSuccess = {
                        loadTasks() // Reload tasks
                    },
                    onFailure = { e ->
                        _errorMessage.value = e.message ?: "Failed to delete task"
                    }
                )
        }
    }
    
    /**
     * Sort tasks
     */
    fun sortTasks(sortBy: TaskSortOption) {
        val sorted = when (sortBy) {
            TaskSortOption.DATE -> _allTasks.value.sortedBy { it.startDateTime }
            TaskSortOption.PRIORITY -> _allTasks.value.sortedByDescending { 
                when (it.priority) {
                    smart.study.planner.data.model.EventPriority.HIGH -> 3
                    smart.study.planner.data.model.EventPriority.MEDIUM -> 2
                    smart.study.planner.data.model.EventPriority.LOW -> 1
                }
            }
            TaskSortOption.CATEGORY -> _allTasks.value.sortedBy { it.category.name }
            TaskSortOption.TITLE -> _allTasks.value.sortedBy { it.title }
        }
        _allTasks.value = sorted
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

