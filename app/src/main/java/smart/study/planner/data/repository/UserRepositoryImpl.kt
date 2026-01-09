package smart.study.planner.data.repository

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import smart.study.planner.data.model.User
import smart.study.planner.data.remote.firebase.FirebaseUserDataSource
import smart.study.planner.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository
 * Handles user operations with Firebase integration
 * 
 * Operations:
 * - Get current user with full profile from Firebase Realtime Database
 * - Update user information (phone, email, gender, school, major, bio, etc.)
 * - Upload user avatar to Firebase Storage
 * - Observe real-time user changes
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseUserDataSource: FirebaseUserDataSource
) : UserRepository {

    companion object {
        private const val TAG = "UserRepositoryImpl"
    }

    /**
     * Get current user with full profile
     * 
     * Fetches user data from Firebase Realtime Database including:
     * - Basic info: id, email, displayName, avatarUrl
     * - Profile info: phone, gender, dateOfBirth
     * - Academic info: studentId, school, major, yearOfStudy
     * - Other: bio, createdAt, updatedAt
     */
    override suspend fun getCurrentUser(): Result<User?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching current user from Firebase")
                firebaseUserDataSource.getCurrentUser()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current user", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update user profile
     * 
     * Saves updated user information to Firebase Realtime Database at: users/{userId}
     * Updates createdAt remains the same, updatedAt is set to current time
     */
    override suspend fun updateUser(user: User): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating user profile: ${user.email}")
                firebaseUserDataSource.updateUser(user)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Upload user avatar
     * 
     * Uploads image to Firebase Storage and returns the download URL
     */
    override suspend fun uploadAvatar(uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Uploading avatar: $uri")
                firebaseUserDataSource.uploadAvatar(uri)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading avatar", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Observe current user changes in real-time
     * 
     * Returns a Flow that emits user updates whenever data changes in Firebase
     * Useful for keeping the profile screen synchronized with database changes
     */
    override fun observeCurrentUser(): Flow<Result<User?>> {
        Log.d(TAG, "Starting to observe current user changes")
        return firebaseUserDataSource.observeCurrentUser()
    }
}
