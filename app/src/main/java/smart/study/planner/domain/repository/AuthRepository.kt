package smart.study.planner.domain.repository

import smart.study.planner.data.model.User

/**
 * Repository interface for Authentication operations
 * 
 * Handles user registration, login, logout, and password reset.
 * User data is stored in Firebase Realtime Database with structure:
 * users/{userId}/{all user fields including phone, email, gender, etc.}
 */
interface AuthRepository {
    /**
     * Login with email and password
     * 
     * @param email User email address
     * @param password User password
     * @return Result containing User object on success
     */
    suspend fun login(email: String, password: String): Result<User>
    
    /**
     * Register new user with complete profile information
     * 
     * Saves user data to:
     * 1. Firebase Authentication (email + password)
     * 2. Firebase Realtime Database (users/{userId})
     * 
     * Error cases:
     * - Email đã tồn tại
     * - Lỗi kết nối Firebase
     * - Lỗi validation
     * 
     * @param email User email address
     * @param password User password (minimum 6 characters)
     * @param name User display name
     * @param phoneNumber User phone number (optional)
     * @param dateOfBirth User date of birth (optional, format: dd/MM/yyyy)
     * @param gender User gender (optional: Nam, Nữ, Khác)
     * @param studentId Student ID (optional)
     * @param school School name (optional)
     * @param major Major/Field of study (optional)
     * @param yearOfStudy Year of study (optional: 1, 2, 3, 4)
     * @param bio User bio/introduction (optional)
     * @return Result containing User object with all fields on success
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
    ): Result<User>
    
    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Send password reset email (Forgot Password flow)
     * Used when user is NOT logged in and needs to reset password
     * Sends email with password reset link to Firebase console
     * 
     * @param email User email address
     * @return Result with success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Update password for current logged-in user (Change Password flow)
     * ONLY works when user is already logged in
     * Requires recent login for security
     * 
     * @param newPassword New password (minimum 6 characters)
     * @return Result with success or failure
     */
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /**
     * Complete password reset for forgot password flow
     * Requires email only - gets old password from local database
     * After verification, updates to new password
     * 
     * @param email User email
     * @param newPassword New password (minimum 6 characters)
     * @return Result with success or failure
     */
    suspend fun completePasswordReset(email: String, newPassword: String): Result<Unit>
    
    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String?
}

