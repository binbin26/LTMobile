package smart.study.planner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.TaskFilter
import smart.study.planner.data.model.TaskSortOption
import smart.study.planner.data.model.EventPriority
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

    companion object {
        private const val TAG = "TaskViewModel"
    }

    private val _allTasks = MutableStateFlow<List<Event>>(emptyList())
    val allTasks: StateFlow<List<Event>> = _allTasks.asStateFlow()

    // ✅ THÊM: StateFlow cho tất cả tasks (bao gồm cả completed)
    private val _allTasksIncludingCompleted = MutableStateFlow<List<Event>>(emptyList())
    val allTasksIncludingCompleted: StateFlow<List<Event>> = _allTasksIncludingCompleted.asStateFlow()

    private val _todayTasks = MutableStateFlow<List<Event>>(emptyList())
    val todayTasks: StateFlow<List<Event>> = _todayTasks.asStateFlow()

    private val _upcomingTasks = MutableStateFlow<List<Event>>(emptyList())
    val upcomingTasks: StateFlow<List<Event>> = _upcomingTasks.asStateFlow()

    private val _overdueTasks = MutableStateFlow<List<Event>>(emptyList())
    val overdueTasks: StateFlow<List<Event>> = _overdueTasks.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    // ✅ THÊM: StateFlow cho filtered tasks để UI observe
    private val _filteredTasks = MutableStateFlow<List<Event>>(emptyList())
    val filteredTasks: StateFlow<List<Event>> = _filteredTasks.asStateFlow()

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
            
            // ✅ BƯỚC 1: Sync pending events VÀ ĐỢI hoàn thành
            try {
                Log.d(TAG, "🔄 Syncing pending events...")
                eventRepository.syncPendingEvents().getOrThrow()
                Log.d(TAG, "✅ Sync pending events completed")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Sync pending events failed: ${e.message}", e)
            }
            
            // ✅ BƯỚC 2: Sync từ Firebase VÀ ĐỢI hoàn thành
            // Note: getAllEvents() sẽ tự động sync Firebase, nhưng chúng ta có thể gọi trước để chắc chắn
            try {
                Log.d(TAG, "🔄 Syncing from Firebase...")
                eventRepository.syncWithFirebase().getOrThrow()
                Log.d(TAG, "✅ Firebase sync completed")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Firebase sync failed: ${e.message}, will try again in getAllEvents()", e)
            }
            
            // ✅ BƯỚC 3: Sau đó mới load tasks (getAllEvents() sẽ sync lại nếu cần)
            eventRepository.getAllEvents()
                .catch { e ->
                    Log.e(TAG, "❌ Error loading tasks: ${e.message}", e)
                    _errorMessage.value = e.message ?: "Failed to load tasks"
                    _isLoading.value = false
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { events ->
                            Log.d(TAG, "📋 Loaded ${events.size} tasks")
                            // ✅ Store all events (including completed)
                            _allTasksIncludingCompleted.value = events
                            // ✅ Store only incomplete events for other filters
                            _allTasks.value = events.filter { !it.isCompleted }
                            updateFilteredTasks()
                            updateTasksByCategory()
                            _isLoading.value = false
                        },
                        onFailure = { e ->
                            Log.e(TAG, "❌ Failed to load tasks: ${e.message}", e)
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
                            updateFilteredTasks()
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
                            updateFilteredTasks()
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
                            updateFilteredTasks()
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
     * ✅ FIX: TaskFilter.ALL now shows all tasks including completed
     */
    private fun updateFilteredTasks() {
        _filteredTasks.value = when (_selectedFilter.value) {
            TaskFilter.ALL -> _allTasksIncludingCompleted.value  // ✅ Show all tasks including completed
            TaskFilter.DUE_TODAY -> _todayTasks.value
            TaskFilter.DUE_LATER -> _upcomingTasks.value
        }
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
     * ✅ FIX: Update local state immediately, then sync in background with proper verification
     */
    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Toggle task completion: $taskId")
            
            // ✅ BƯỚC 1: Update local state immediately for instant UI feedback
            val currentTasks = _allTasks.value.toMutableList()
            val currentAllTasks = _allTasksIncludingCompleted.value.toMutableList()
            val taskIndex = currentTasks.indexOfFirst { it.id == taskId }
            val allTasksIndex = currentAllTasks.indexOfFirst { it.id == taskId }
            
            if (taskIndex != -1 || allTasksIndex != -1) {
                val task = currentTasks.getOrNull(taskIndex) ?: currentAllTasks.getOrNull(allTasksIndex)
                if (task != null) {
                    val updatedTask = task.copy(isCompleted = !task.isCompleted)
                    
                    Log.d(TAG, "📝 Optimistic UI update: taskId=$taskId, isCompleted=${updatedTask.isCompleted}")
                    
                    // Update _allTasksIncludingCompleted (always update this)
                    if (allTasksIndex != -1) {
                        currentAllTasks[allTasksIndex] = updatedTask
                    } else {
                        currentAllTasks.add(updatedTask)
                    }
                    _allTasksIncludingCompleted.value = currentAllTasks
                    
                    // Remove completed tasks from _allTasks (filtered list)
                    if (updatedTask.isCompleted) {
                        if (taskIndex != -1) {
                            currentTasks.removeAt(taskIndex)
                        }
                    } else {
                        if (taskIndex != -1) {
                            currentTasks[taskIndex] = updatedTask
                        } else {
                            currentTasks.add(updatedTask)
                        }
                    }
                    _allTasks.value = currentTasks
                    updateFilteredTasks()
                    
                    // ✅ Update today/upcoming tasks if needed
                    val todayTasks = _todayTasks.value.toMutableList()
                    val todayIndex = todayTasks.indexOfFirst { it.id == taskId }
                    if (todayIndex != -1) {
                        if (updatedTask.isCompleted) {
                            todayTasks.removeAt(todayIndex)
                        } else {
                            todayTasks[todayIndex] = updatedTask
                        }
                        _todayTasks.value = todayTasks
                    }
                    
                    val upcomingTasks = _upcomingTasks.value.toMutableList()
                    val upcomingIndex = upcomingTasks.indexOfFirst { it.id == taskId }
                    if (upcomingIndex != -1) {
                        if (updatedTask.isCompleted) {
                            upcomingTasks.removeAt(upcomingIndex)
                        } else {
                            upcomingTasks[upcomingIndex] = updatedTask
                        }
                        _upcomingTasks.value = upcomingTasks
                    }
                    
                    updateFilteredTasks()
                }
            }
            
            // ✅ BƯỚC 2: Sync to Room + Firebase in background (non-blocking)
            eventRepository.toggleEventCompletion(taskId)
                .fold(
                    onSuccess = { updatedEvent ->
                        Log.d(TAG, "✅ Toggle persisted to Room: taskId=$taskId, isCompleted=${updatedEvent.isCompleted}")
                        // ✅ Room is updated, Firebase sync is queued or completed
                        // Local UI state is already correct from optimistic update
                    },
                    onFailure = { e ->
                        Log.e(TAG, "❌ Failed to persist toggle: taskId=$taskId, error=${e.message}", e)
                        _errorMessage.value = e.message ?: "Failed to update task"
                        // ✅ Reload to revert optimistic update
                        Log.d(TAG, "🔄 Reloading tasks due to toggle failure")
                        loadTasks()
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
                    EventPriority.HIGH -> 3
                    EventPriority.MEDIUM -> 2
                    EventPriority.LOW -> 1
                }
            }
            TaskSortOption.CATEGORY -> _allTasks.value.sortedBy { it.category.name }
            TaskSortOption.TITLE -> _allTasks.value.sortedBy { it.title }
        }
        _allTasks.value = sorted
        updateFilteredTasks() // ✅ Cập nhật filtered tasks sau khi sort
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}