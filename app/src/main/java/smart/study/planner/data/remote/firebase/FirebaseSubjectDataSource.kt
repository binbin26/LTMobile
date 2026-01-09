package smart.study.planner.data.remote.firebase

import android.util.Log
import smart.study.planner.data.model.Subject
import smart.study.planner.data.remote.dto.SubjectDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseSubjectDataSource"

/**
 * Data source for Firebase Realtime Database operations for Subjects
 * Handles all Firebase-specific subject operations
 */
@Singleton
class FirebaseSubjectDataSource @Inject constructor(
    private val databaseReference: DatabaseReference,
    private val firebaseAuth: FirebaseAuth
) {
    
    /**
     * Get current user ID or throw exception if not authenticated
     */
    private fun getUserId(): String {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "User not authenticated!")
            throw IllegalStateException("Người dùng chưa được xác thực. Vui lòng đăng nhập trước.")
        }
        Log.d(TAG, "User ID: $uid")
        return uid
    }
    
    /**
     * Get the subjects reference for current user
     * Path: users/{userId}/subjects/{subjectId}
     */
    private fun getSubjectsReference(): DatabaseReference {
        return databaseReference.child("users").child(getUserId()).child("subjects")
    }
    
    /**
     * Verify and refresh auth token before operations
     */
    private suspend fun ensureAuthTokenValid(): Boolean {
        val user = firebaseAuth.currentUser
        if (user == null) {
            Log.e(TAG, "No authenticated user")
            return false
        }
        
        return try {
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token
            Log.d(TAG, "Auth token verified - Token exists: ${token != null}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify auth token", e)
            false
        }
    }
    
    /**
     * Observe all subjects from Firebase as a Flow
     */
    fun observeAllSubjects(): Flow<Result<List<Subject>>> = callbackFlow {
        try {
            val subjectsRef = getSubjectsReference()
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val subjects = snapshot.children.mapNotNull { child ->
                            child.getValue(SubjectDto::class.java)?.toSubject()
                        }
                        Log.d(TAG, "Observed ${subjects.size} subjects from Firebase")
                        trySend(Result.success(subjects))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing subjects", e)
                        trySend(Result.failure(e))
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase error: ${error.message}")
                    trySend(Result.failure(Exception(error.message)))
                }
            }
            
            subjectsRef.addValueEventListener(listener)
            
            awaitClose {
                subjectsRef.removeEventListener(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in observeAllSubjects", e)
            close(e)
        }
    }
    
    /**
     * Get all subjects once (non-reactive)
     */
    suspend fun getAllSubjects(): Result<List<Subject>> {
        return try {
            val snapshot = getSubjectsReference().get().await()
            val subjects = snapshot.children.mapNotNull { child ->
                child.getValue(SubjectDto::class.java)?.toSubject()
            }
            Log.d(TAG, "Retrieved ${subjects.size} subjects from Firebase")
            Result.success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all subjects", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get subject by ID
     */
    suspend fun getSubjectById(id: String): Result<Subject?> {
        return try {
            val snapshot = getSubjectsReference().child(id).get().await()
            val subject = snapshot.getValue(SubjectDto::class.java)?.toSubject()
            Log.d(TAG, "Retrieved subject: $id")
            Result.success(subject)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subject by id", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save subject to Firebase
     */
    suspend fun saveSubject(subject: Subject): Result<Unit> {
        return try {
            // Validate subject ID
            if (subject.id.isEmpty()) {
                val error = Exception("Subject ID không hợp lệ")
                Log.e(TAG, "Invalid subject ID detected!")
                return Result.failure(error)
            }
            
            // Validate userId
            if (subject.userId.isEmpty()) {
                val error = Exception("User ID không được để trống")
                Log.e(TAG, "Empty userId!")
                return Result.failure(error)
            }
            
            // Verify token first
            if (!ensureAuthTokenValid()) {
                val error = Exception("Authentication token không hợp lệ. Vui lòng đăng nhập lại.")
                Log.e(TAG, "Token validation failed before save")
                return Result.failure(error)
            }
            
            val userId = getUserId()
            
            val subjectDto = SubjectDto.fromSubject(subject.copy(userId = userId))
            val subjectMap = subjectDto.toMap()
            val path = "users/$userId/subjects/${subject.id}"
            
            Log.d(TAG, "Saving subject to Firebase: $path")
            Log.d(TAG, "Subject ID: ${subject.id}, Name: ${subject.name}")
            
            getSubjectsReference().child(subject.id).setValue(subjectMap).await()
            
            Log.d(TAG, "✅ Subject saved successfully to Firebase!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED TO SAVE SUBJECT TO FIREBASE")
            Log.e(TAG, "Subject ID: ${subject.id}")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update subject in Firebase
     */
    suspend fun updateSubject(subject: Subject): Result<Unit> {
        return try {
            // Validate subject ID
            if (subject.id.isEmpty()) {
                val error = Exception("Subject ID không hợp lệ")
                Log.e(TAG, "Invalid subject ID for update!")
                return Result.failure(error)
            }
            
            // Validate userId
            if (subject.userId.isEmpty()) {
                val error = Exception("User ID không được để trống")
                Log.e(TAG, "Empty userId!")
                return Result.failure(error)
            }
            
            // Verify token first
            if (!ensureAuthTokenValid()) {
                val error = Exception("Authentication token không hợp lệ. Vui lòng đăng nhập lại.")
                Log.e(TAG, "Token validation failed before update")
                return Result.failure(error)
            }
            
            val userId = getUserId()
            
            val subjectDto = SubjectDto.fromSubject(subject.copy(userId = userId))
            val subjectMap = subjectDto.toMap()
            val path = "users/$userId/subjects/${subject.id}"
            
            Log.d(TAG, "Updating subject in Firebase: $path")
            Log.d(TAG, "Subject ID: ${subject.id}, Name: ${subject.name}")
            
            getSubjectsReference().child(subject.id).setValue(subjectMap).await()
            
            Log.d(TAG, "✅ Subject updated successfully in Firebase!")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED TO UPDATE SUBJECT IN FIREBASE")
            Log.e(TAG, "Subject ID: ${subject.id}")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete subject from Firebase
     */
    suspend fun deleteSubject(id: String): Result<Unit> {
        return try {
            getSubjectsReference().child(id).removeValue().await()
            Log.d(TAG, "✅ Subject deleted from Firebase: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting subject from Firebase: ${e.message}")
            Result.failure(e)
        }
    }
}
