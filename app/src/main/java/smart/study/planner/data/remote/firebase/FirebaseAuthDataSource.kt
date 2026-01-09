package smart.study.planner.data.remote.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import smart.study.planner.data.local.datasource.UserCredentialLocalDataSource
import smart.study.planner.data.local.entity.UserCredentialEntity
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
    private val databaseReference: DatabaseReference,
    private val userCredentialLocalDataSource: UserCredentialLocalDataSource
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

            // Save credentials to local database for forgot password verification
            val credential = UserCredentialEntity(
                email = email,
                password = password,
                userId = firebaseUser.uid,
                displayName = user.displayName
            )
            userCredentialLocalDataSource.saveUserCredential(credential)
            Log.d(TAG, "Saved user credential locally for: $email")

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

            // Save credentials to local database for forgot password verification
            val credential = UserCredentialEntity(
                email = email,
                password = password,
                userId = firebaseUser.uid,
                displayName = name
            )
            userCredentialLocalDataSource.saveUserCredential(credential)
            Log.d(TAG, "Saved user credential locally for: $email")

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
     * Send password reset email (Forgot Password flow)
     * Works when user is NOT logged in
     * Sends email with password reset link via Firebase console
     *
     * @param email User email address
     * @return Result with success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Gửi email đặt lại mật khẩu thành công cho $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi email đặt lại mật khẩu", e)
            // Map exception to appropriate error message
            val errorMessage = when {
                e.message?.contains("too-many-requests") == true ->
                    IllegalStateException("Quá nhiều yêu cầu. Vui lòng thử lại sau 15 phút")
                e.message?.contains("user-not-found") == true ->
                    IllegalStateException("Email không tồn tại")
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
     * Update password for current user (Change Password flow)
     * ONLY works when user is logged in (currentUser != null)
     * Updates password in Firebase Authentication
     *
     * @param newPassword The new password (minimum 6 characters)
     * @return Result with success or failure
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("Không có user hiện tại")

            firebaseUser.updatePassword(newPassword).await()
            Log.d(TAG, "Cập nhật mật khẩu thành công")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi cập nhật mật khẩu", e)
            val errorMessage = when {
                e.message?.contains("weak-password") == true ->
                    IllegalStateException("Mật khẩu quá yếu")
                e.message?.contains("requires-recent-login") == true ->
                    IllegalStateException("Vui lòng đăng nhập lại trước khi thay đổi mật khẩu")
                else -> e
            }
            Result.failure(errorMessage)
        }
    }

    /**
     * Complete password reset for forgot password flow
     * Sends password reset email via Firebase
     * Firebase Auth is the source of truth - does not rely on local database
     * Returns success to prevent user enumeration (doesn't reveal if email exists)
     *
     * @param email User email (from forgot password flow)
     * @param newPassword The new password to set (user will set this via email link)
     * @return Result with success or failure
     */
    suspend fun completePasswordReset(email: String, newPassword: String): Result<Unit> {
        return try {
            Log.d(TAG, "Initiating password reset for email: $email")

            // Send password reset email via Firebase
            // Firebase Auth is the source of truth - only Firebase knows if email exists
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent successfully for: $email")

            // Always return success to prevent user enumeration
            // User will receive email if address is registered in Firebase
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset email", e)

            // Handle specific Firebase errors
            val errorMessage = when {
                e.message?.contains("too-many-requests") == true ->
                    IllegalStateException("Quá nhiều yêu cầu. Vui lòng thử lại sau 15 phút")
                e.message?.contains("invalid-email") == true ->
                    IllegalStateException("Email không hợp lệ")
                e.message?.contains("network") == true ->
                    IllegalStateException("Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet")
                // For user-not-found and other errors, return success to prevent enumeration
                // User will only know if email is registered by checking their inbox
                else -> {
                    Log.w(TAG, "Password reset request sent (Firebase response: ${e.message})")
                    // Return success anyway - user will know by checking email
                    return Result.success(Unit)
                }
            }
            Result.failure(errorMessage)
        }
    }

    /**
     * Re-authenticate user with email and password
     * Required for sensitive operations like password update
     *
     * @param email User email
     * @param password Current password
     * @return Result with success or failure
     */
    suspend fun reauthenticate(email: String, password: String): Result<Unit> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: throw IllegalStateException("Không có user hiện tại")

            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
            firebaseUser.reauthenticate(credential).await()
            Log.d(TAG, "Xác thực lại thành công")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xác thực lại", e)
            Result.failure(e)
        }
    }

    /**
     * Map Firebase authentication exceptions to appropriate error messages
     * Handles FirebaseTooManyRequestsException explicitly
     *
     * @param exception The Firebase exception
     * @return An Exception with an appropriate error message
     */
    private fun mapPasswordResetException(exception: Exception): Exception {
        return when {
            exception.message?.contains("too-many-requests") == true -> {
                Log.w(TAG, "Too many password reset requests from this email")
                IllegalStateException("Quá nhiều yêu cầu. Vui lòng thử lại sau 15 phút")
            }
            exception.message?.contains("invalid-email") == true ->
                IllegalStateException("Email không hợp lệ")
            exception.message?.contains("network") == true ->
                IllegalStateException("Lỗi kết nối mạng. Vui lòng kiểm tra kết nối internet")
            // For user-not-found and other errors, return generic message to prevent enumeration
            exception.message?.contains("user-not-found") == true -> {
                Log.w(TAG, "User not found for password reset (preventing enumeration)")
                IllegalStateException("Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau")
            }
            else -> {
                Log.w(TAG, "Password reset error: ${exception.message}")
                exception
            }
        }
    }
}