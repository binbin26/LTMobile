package smart.study.planner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import smart.study.planner.data.local.database.AppDatabase
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventCategory
import smart.study.planner.data.model.EventPriority
import smart.study.planner.util.debug.DebugLogger
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val db: AppDatabase,
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {

    private val _testResult = MutableStateFlow("")
    val testResult = _testResult.asStateFlow()

    fun testFirebaseConnection() {
        viewModelScope.launch {
            firebaseDatabase.getReference(".info/connected")
                .get()
                .addOnSuccessListener { 
                    val connected = it.getValue(Boolean::class.java) ?: false
                    _testResult.value = "Firebase connected: $connected"
                    DebugLogger.logFirebaseSave(connected)
                }
                .addOnFailureListener {
                    _testResult.value = "Firebase connection failed: ${it.message}"
                    DebugLogger.logError("Firebase Test", it)
                }
        }
    }

    fun testRoomOperations() {
        viewModelScope.launch {
            try {
                val event = Event(id = "test_event", title = "Test Event", description = "Test Description", startDateTime = System.currentTimeMillis(), endDateTime = System.currentTimeMillis() + 3600000, category = EventCategory.STUDY, priority = EventPriority.HIGH, isAllDay = false)
                db.eventDao().insertEvent(event)
                DebugLogger.logRoomInsert(1)
                val retrievedEvent = db.eventDao().getEventById("test_event")
                _testResult.value = "Room test successful. Retrieved event: $retrievedEvent"
                db.eventDao().deleteEvent(event)
            } catch (e: Exception) {
                _testResult.value = "Room test failed: ${e.message}"
                DebugLogger.logError("Room Test", e)
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            try {
                db.clearAllTables()
                _testResult.value = "Database cleared."
            } catch (e: Exception) {
                _testResult.value = "Failed to clear database: ${e.message}"
            }
        }
    }
}