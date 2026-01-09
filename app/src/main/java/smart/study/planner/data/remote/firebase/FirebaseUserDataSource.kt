package smart.study.planner.data.remote.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import smart.study.planner.data.model.User
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase User operations
 * Handles user profile updates and avatar uploads
 */
@Singleton
class FirebaseUserDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val databaseReference: DatabaseReference,
    private val firebaseStorage: FirebaseStorage
) {

    companion object {
        private const val TAG = "FirebaseUserDataSource"
    }

    /**
     * Get current user with full profile from Firebase Realtime Database
     */
    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
            if (userId == null) {
                Log.d(TAG, "No authenticated user, returning null")
                return Result.success(null)
            }
            
            val snapshot = databaseReference.child("users").child(userId).get().await()
            
            Log.d(TAG, "Firebase snapshot exists: ${snapshot.exists()}")
            Log.d(TAG, "Firebase raw data keys: ${snapshot.children.map { it.key }}")
            
            val user = snapshot.getValue(User::class.java)
            
            if (user != null) {
                Log.d(TAG, """
                    ┌─────────────────────────────────────
                    │ PARSED USER FROM FIREBASE
                    ├─────────────────────────────────────
                    │ All fields present:
                    │ id: ${user.id}
                    │ email: ${user.email}
                    │ displayName: ${user.displayName}
                    │ avatarUrl: ${user.avatarUrl}
                    │ phoneNumber: ${user.phoneNumber}
                    │ dateOfBirth: ${user.dateOfBirth}
                    │ gender: ${user.gender}
                    │ studentId: ${user.studentId}
                    │ school: ${user.school}
                    │ major: ${user.major}
                    │ yearOfStudy: ${user.yearOfStudy}
                    │ bio: ${user.bio}
                    │ createdAt: ${user.createdAt} (${Date(user.createdAt)})
                    │ updatedAt: ${user.updatedAt} (${Date(user.updatedAt)})
                    └─────────────────────────────────────
                """.trimIndent())
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user from Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: throw IllegalStateException("Người dùng chưa đăng nhập")

            Log.d(TAG, """
                ┌─────────────────────────────────────
                │ UPDATING USER IN FIREBASE
                ├─────────────────────────────────────
                │ Data to save:
            """.trimIndent())
            
            // Log từng field
            Log.d(TAG, "│ id: ${user.id}")
            Log.d(TAG, "│ email: ${user.email}")
            Log.d(TAG, "│ displayName: ${user.displayName}")
            Log.d(TAG, "│ avatarUrl: ${user.avatarUrl}")
            Log.d(TAG, "│ phoneNumber: ${user.phoneNumber}")
            Log.d(TAG, "│ dateOfBirth: ${user.dateOfBirth}")
            Log.d(TAG, "│ gender: ${user.gender}")
            Log.d(TAG, "│ studentId: ${user.studentId}")
            Log.d(TAG, "│ school: ${user.school}")
            Log.d(TAG, "│ major: ${user.major}")
            Log.d(TAG, "│ yearOfStudy: ${user.yearOfStudy}")
            Log.d(TAG, "│ bio: ${user.bio}")
            Log.d(TAG, "│ createdAt: ${user.createdAt}")
            Log.d(TAG, "│ updatedAt: ${user.updatedAt}")
            Log.d(TAG, "└─────────────────────────────────────")
            
            val userMap = mapOf(
                "id" to user.id,
                "email" to user.email,
                "displayName" to user.displayName,
                "avatarUrl" to user.avatarUrl,
                "phoneNumber" to user.phoneNumber,
                "dateOfBirth" to user.dateOfBirth,
                "gender" to user.gender,
                "studentId" to user.studentId,
                "school" to user.school,
                "major" to user.major,
                "yearOfStudy" to user.yearOfStudy,
                "bio" to user.bio,
                "createdAt" to user.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            
            databaseReference.child("users").child(userId).setValue(userMap).await()
            Log.d(TAG, "✓ User data saved successfully to Firebase")
            
            // Verify data was saved
            val verifySnapshot = databaseReference.child("users").child(userId).get().await()
            Log.d(TAG, "✓ Verification - Data in Firebase: ${verifySnapshot.value}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error updating user in Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Upload user avatar
     */
    suspend fun uploadAvatar(uri: Uri): Result<String> {
        return try {
            val userId = firebaseAuth.currentUser?.uid
                ?: throw IllegalStateException("Người dùng chưa đăng nhập")

            val fileName = "avatars/$userId/${System.currentTimeMillis()}.jpg"
            val storageRef = firebaseStorage.reference.child(fileName)

            // Upload file
            storageRef.putFile(uri).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()
            val avatarUrl = downloadUrl.toString()

            // Update user record in database
            databaseReference.child("users").child(userId).child("avatarUrl")
                .setValue(avatarUrl).await()

            Result.success(avatarUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe current user changes in real-time with detailed logging
     */
    fun observeCurrentUser(): Flow<Result<User?>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            Log.d(TAG, "No authenticated user for real-time observation")
            trySend(Result.success(null))
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Starting real-time observation for user: $userId")
        
        val userRef = databaseReference.child("users").child(userId)
        var eventCount = 0
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                eventCount++
                try {
                    Log.d(TAG, "┌─ REAL-TIME UPDATE #$eventCount")
                    Log.d(TAG, "│ Snapshot exists: ${snapshot.exists()}")
                    Log.d(TAG, "│ Raw data: ${snapshot.value}")
                    
                    val user = snapshot.getValue(User::class.java)
                    
                    if (user != null) {
                        Log.d(TAG, """
                            │ 
                            │ FIREBASE DATA RECEIVED
                            ├─────────────────────────────────────
                            │ ID: ${user.id}
                            │ Email: ${user.email}
                            │ Display Name: ${user.displayName}
                            │ Avatar: ${user.avatarUrl ?: "NOT SET"}
                            │ Phone: ${user.phoneNumber ?: "NOT SET"}
                            │ DOB: ${user.dateOfBirth?.let { Date(it) } ?: "NOT SET"}
                            │ Gender: ${user.gender ?: "NOT SET"}
                            │ Student ID: ${user.studentId ?: "NOT SET"}
                            │ School: ${user.school ?: "NOT SET"}
                            │ Major: ${user.major ?: "NOT SET"}
                            │ Year: ${user.yearOfStudy ?: "NOT SET"}
                            │ Bio: ${user.bio?.take(50) ?: "NOT SET"}
                            │ Created: ${Date(user.createdAt)}
                            │ Updated: ${Date(user.updatedAt)}
                            └─────────────────────────────────────
                        """.trimIndent())
                    } else {
                        Log.d(TAG, "│ User data is null")
                        Log.d(TAG, "└─ End of update #$eventCount")
                    }
                    
                    trySend(Result.success(user))
                } catch (e: Exception) {
                    Log.e(TAG, "│ Error parsing user data in real-time update #$eventCount", e)
                    Log.d(TAG, "└─ Error in update #$eventCount")
                    trySend(Result.failure(e))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Real-time observation cancelled: ${error.message}", error.toException())
                trySend(Result.failure(Exception(error.message)))
            }
        }

        userRef.addValueEventListener(listener)

        awaitClose {
            Log.d(TAG, "Removing real-time listener for user: $userId")
            userRef.removeEventListener(listener)
        }
    }
}
