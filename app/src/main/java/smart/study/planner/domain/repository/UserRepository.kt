package smart.study.planner.domain.repository

import android.net.Uri
import smart.study.planner.data.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for User operations
 * 
 * Handles user profile management:
 * - Fetch current user with full profile (phone, email, gender, school, major, etc.)
 * - Update user information
 * - Upload avatar
 * - Observe real-time user changes from Firebase Realtime Database
 */
interface UserRepository {
    /**
     * Get current user with full profile
     * 
     * @return Result containing User object with all fields:
     *         id, email, displayName, avatarUrl, phoneNumber, dateOfBirth,
     *         gender, studentId, school, major, yearOfStudy, bio, createdAt, updatedAt
     */
    suspend fun getCurrentUser(): Result<User?>
    
    /**
     * Update user profile information
     * 
     * Saves updated user data to Firebase Realtime Database at: users/{userId}
     * Updates all user fields including phone, email, gender, school, major, bio, etc.
     * 
     * @param user User object with updated fields
     * @return Result containing Unit on success
     */
    suspend fun updateUser(user: User): Result<Unit>
    
    /**
     * Upload user avatar
     * 
     * @param uri Image URI to upload
     * @return Result containing avatar URL in Firebase Storage
     */
    suspend fun uploadAvatar(uri: Uri): Result<String>
    
    /**
     * Observe current user changes in real-time
     * 
     * Emits user updates whenever data changes in Firebase Realtime Database
     * Useful for keeping profile screen in sync with database changes
     * 
     * @return Flow emitting Result<User?> for each update
     */
    fun observeCurrentUser(): Flow<Result<User?>>
}

