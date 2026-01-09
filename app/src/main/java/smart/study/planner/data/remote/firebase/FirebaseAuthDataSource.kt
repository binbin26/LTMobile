package smart.study.planner.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import smart.study.planner.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase Authentication operations
 * Handles user authentication, registration, logout, and password reset
 */
@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val databaseReference: DatabaseReference
) {

    companion object {
        private const val TAG = "FirebaseAuthDataSource"
    }

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: throw IllegalStateException("Không thể đăng nhập: người dùng null")

            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                displayName = firebaseUser.displayName ?: email.substringBefore("@"),
                avatarUrl = firebaseUser.photoUrl?.toString()
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register new user
     */
    suspend fun register(
        email: String,
        password: String,
        name: String,
        phoneNumber: String? = null,
        dateOfBirth: String? = null,
        gender: String? = null,
        studentId: String? = null,
        school: String? = null,
        major: String? = null,
        yearOfStudy: Int? = null,
        bio: String? = null
    ): Result<User> {
        return try {
            // Create Firebase Auth account
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Không thể tạo tài khoản: người dùng null")

            // Update profile with display name
            val profileUpdate = userProfileChangeRequest {
                displayName = name
            }
            firebaseUser.updateProfile(profileUpdate).await()

            // Convert dateOfBirth string to timestamp if provided
            var dateOfBirthTimestamp: Long? = null
            if (!dateOfBirth.isNullOrBlank()) {
                try {
                    val parts = dateOfBirth.split("/")
                    if (parts.size == 3) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt() - 1 // Calendar months are 0-indexed
                        val year = parts[2].toInt()
                        val calendar = java.util.Calendar.getInstance()
                        calendar.set(year, month, day, 0, 0, 0)
                        dateOfBirthTimestamp = calendar.timeInMillis
                    }
                } catch (e: Exception) {
                    // If parsing fails, just leave it null
                }
            }

            // Create user record in Realtime Database with all fields
            val currentTimeMillis = System.currentTimeMillis()
            val newUser = User(
                id = firebaseUser.uid,
                email = email,
                displayName = name,
                avatarUrl = null,
                createdAt = currentTimeMillis,
                phoneNumber = phoneNumber.takeIf { !it.isNullOrBlank() },
                dateOfBirth = dateOfBirthTimestamp,
                gender = gender.takeIf { !it.isNullOrBlank() },
                studentId = studentId.takeIf { !it.isNullOrBlank() },
                school = school.takeIf { !it.isNullOrBlank() },
                major = major.takeIf { !it.isNullOrBlank() },
                yearOfStudy = yearOfStudy,
                bio = bio.takeIf { !it.isNullOrBlank() },
                updatedAt = currentTimeMillis
            )

            // Save user data to Firebase Realtime Database: users/{userId}
            databaseReference.child("users").child(firebaseUser.uid)
                .setValue(newUser).await()

            Result.success(newUser)
        } catch (e: Exception) {
            // Handle specific Firebase exceptions
            val errorMessage = when {
                e.message?.contains("email-already-in-use") == true -> 
                    IllegalStateException("Email đã được sử dụng")
                e.message?.contains("weak-password") == true -> 
                    IllegalStateException("Mật khẩu quá yếu")
                e.message?.contains("invalid-email") == true -> 
                    IllegalStateException("Email không hợp lệ")
                e.message?.contains("network") == true -> 
                    IllegalStateException("Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet")
                else -> e
            }
            Result.failure(errorMessage)
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    /**
     * Get current user with full profile from Firebase Realtime Database
     * 
     * Tries to fetch user data from Database first.
     * If not found, creates User object from FirebaseAuth.
     * Logs all steps for debugging.
     */
    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            
            if (firebaseUser != null) {
                Log.d(TAG, "Lấy thông tin user hiện tại: ${firebaseUser.uid}")
                
                // Try to get full user data from database
                val userResult = getUserFromDatabase(firebaseUser.uid)
                return userResult.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            Log.d(TAG, "Lấy user từ database thành công: ${user.email}")
                            Result.success(user)
                        } else {
                            Log.d(TAG, "User không tồn tại trong database, tạo từ FirebaseAuth")
                            // If not in database, create from FirebaseAuth
                            val fallbackUser = User(
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                                avatarUrl = firebaseUser.photoUrl?.toString(),
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            Log.d(TAG, "Tạo user fallback từ FirebaseAuth")
                            Result.success(fallbackUser)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Lỗi lấy user từ database, tạo fallback", error)
                        // On database error, create from FirebaseAuth as fallback
                        val fallbackUser = User(
                            id = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                            avatarUrl = firebaseUser.photoUrl?.toString(),
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        Result.success(fallbackUser)
                    }
                )
            } else {
                Log.d(TAG, "Không có user hiện tại")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ngoại lệ khi lấy current user", e)
            Result.failure(e)
        }
    }

    /**
     * Get user profile from Firebase Realtime Database
     * 
     * Fetches complete user information from path: users/{userId}
     * Includes all user fields: phone, email, gender, school, major, etc.
     * 
     * @param userId Firebase user ID
     * @return Result containing complete User object with all fields, or null if not found
     */
    suspend fun getUserFromDatabase(userId: String): Result<User?> {
        return try {
            Log.d(TAG, "Bắt đầu lấy user từ database: $userId")
            
            val snapshot = databaseReference.child("users").child(userId).get().await()
            
            if (snapshot.exists()) {
                Log.d(TAG, "Tìm thấy user data trong database")
                
                try {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        Log.d(TAG, "Parse user object thành công: ${user.email}")
                        Result.success(user)
                    } else {
                        Log.d(TAG, "User data null sau khi parse")
                        Result.success(null)
                    }
                } catch (parseError: Exception) {
                    Log.e(TAG, "Lỗi parse user object từ snapshot", parseError)
                    Result.failure(parseError)
                }
            } else {
                Log.d(TAG, "User không tồn tại trong database")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi fetch user từ database", e)
            Result.failure(e)
        }
    }
}
