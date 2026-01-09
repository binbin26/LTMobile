package smart.study.planner.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventCategory
import smart.study.planner.data.model.EventPriority
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PermissionTest"

@Singleton
class FirebasePermissionTester @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val databaseReference: DatabaseReference
) {
    
    suspend fun testBasicWrite(): TestResult {
        val user = firebaseAuth.currentUser
        if (user == null) {
            return TestResult.Failed("No authenticated user")
        }
        
        Log.d(TAG, "Testing with UID: ${user.uid}")
        
        try {
            val testPath = "users/${user.uid}/test_write_${System.currentTimeMillis()}"
            val testData = mapOf("test" to "value", "timestamp" to System.currentTimeMillis())
            
            Log.d(TAG, "Attempting write to: $testPath")
            databaseReference.child(testPath).setValue(testData).await()
            
            Log.d(TAG, "✅ Basic write successful!")
            
            // Clean up
            databaseReference.child(testPath).removeValue().await()
            
            return TestResult.Success("Basic write permission OK")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Basic write failed", e)
            return TestResult.Failed("Write failed: ${e.message}")
        }
    }
    
    suspend fun testEventWrite(): TestResult {
        val user = firebaseAuth.currentUser ?: return TestResult.Failed("No user")
        
        try {
            val eventId = "test_${System.currentTimeMillis()}"
            val testEvent = mapOf(
                "id" to eventId,
                "userId" to user.uid,
                "title" to "Test Event",
                "description" to "Test Description",
                "startDateTime" to System.currentTimeMillis(),
                "endDateTime" to null,
                "location" to "",
                "category" to "STUDY",
                "priority" to "MEDIUM",
                "isCompleted" to false,
                "isAllDay" to false,
                "reminderEnabled" to false,
                "reminderMinutes" to 15,
                "colorHex" to "#4285F4",
                "isSynced" to true,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            
            val path = "users/${user.uid}/events/$eventId"
            
            Log.d(TAG, "Testing event write to: $path")
            Log.d(TAG, "Event data: $testEvent")
            
            databaseReference.child(path).setValue(testEvent).await()
            
            Log.d(TAG, "✅ Event write successful!")
            
            // Clean up
            databaseReference.child(path).removeValue().await()
            
            return TestResult.Success("Event write OK")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Event write failed", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            return TestResult.Failed("Event write failed: ${e.message}")
        }
    }
    
    suspend fun runAllTests(): List<TestResult> {
        Log.d(TAG, "=== Starting Firebase Permission Tests ===")
        
        val user = firebaseAuth.currentUser
        Log.d(TAG, "Current User: ${user?.email} (${user?.uid})")
        
        // Force token refresh
        try {
            user?.getIdToken(true)?.await()
            Log.d(TAG, "✅ Token refreshed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Token refresh failed", e)
        }
        
        return listOf(
            testBasicWrite(),
            testEventWrite()
        )
    }
}

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Failed(val message: String) : TestResult()
}
