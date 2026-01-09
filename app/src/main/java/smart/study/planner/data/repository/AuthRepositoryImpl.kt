package smart.study.planner.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import smart.study.planner.data.model.User
import smart.study.planner.data.remote.firebase.FirebaseAuthDataSource
import smart.study.planner.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository
 * Handles authentication operations with Firebase
 * Saves user data to Firebase Realtime Database: users/{userId}/{user_fields}
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }

    /**
     * Login with email and password
     */
    override suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuthDataSource.login(email, password)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi đăng nhập", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Register new user
     * Saves user information to Firebase:
     * - Firebase Authentication (email + password)
     * - Firebase Realtime Database (users/{userId})
     * 
     * Error cases handled:
     * - Email đã tồn tại
     * - Lỗi kết nối Firebase
     * - Lỗi validation
     */
    override suspend fun register(
        email: String,
        password: String,
        name: String,
        phoneNumber: String?,
        dateOfBirth: String?,
        gender: String?,
        studentId: String?,
        school: String?,
        major: String?,
        yearOfStudy: Int?,
        bio: String?
    ): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Bắt đầu đăng ký người dùng: $email")
                
                val result = firebaseAuthDataSource.register(
                    email = email,
                    password = password,
                    name = name,
                    phoneNumber = phoneNumber,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    studentId = studentId,
                    school = school,
                    major = major,
                    yearOfStudy = yearOfStudy,
                    bio = bio
                )
                
                result.fold(
                    onSuccess = { user ->
                        Log.d(TAG, "Đăng ký thành công người dùng: ${user.email}, ID: ${user.id}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Lỗi đăng ký", error)
                    }
                )
                
                result
            } catch (e: Exception) {
                Log.e(TAG, "Ngoại lệ trong quá trình đăng ký", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Logout current user
     */
    override suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuthDataSource.logout()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi đăng xuất", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Send password reset email (Forgot Password flow)
     * Used when user is NOT logged in
     * Sends email with password reset link
     */
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuthDataSource.sendPasswordResetEmail(email)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi gửi email đặt lại mật khẩu", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update password for current logged-in user (Change Password flow)
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuthDataSource.updatePassword(newPassword)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi cập nhật mật khẩu", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Complete password reset for forgot password flow
     */
    override suspend fun completePasswordReset(email: String, newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firebaseAuthDataSource.completePasswordReset(email, newPassword)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi hoàn tất reset password", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Check if user is logged in
     */
    override fun isUserLoggedIn(): Boolean {
        return firebaseAuthDataSource.isUserLoggedIn()
    }

    /**
     * Get current user ID
     */
    override fun getCurrentUserId(): String? {
        return firebaseAuthDataSource.getCurrentUserId()
    }
}
