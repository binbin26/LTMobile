package smart.study.planner.data.local.datasource

import android.util.Log
import smart.study.planner.data.local.dao.UserCredentialDao
import smart.study.planner.data.local.entity.UserCredentialEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source for user credentials
 * Handles caching user email/password for forgot password verification
 */
@Singleton
class UserCredentialLocalDataSource @Inject constructor(
    private val userCredentialDao: UserCredentialDao
) {
    
    companion object {
        private const val TAG = "UserCredentialLocalDataSource"
    }
    
    /**
     * Save user credentials locally
     */
    suspend fun saveUserCredential(credential: UserCredentialEntity) {
        try {
            userCredentialDao.insertUserCredential(credential)
            Log.d(TAG, "Saved user credential for email: ${credential.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user credential", e)
        }
    }
    
    /**
     * Save multiple user credentials
     */
    suspend fun saveUserCredentials(credentials: List<UserCredentialEntity>) {
        try {
            userCredentialDao.insertUserCredentials(credentials)
            Log.d(TAG, "Saved ${credentials.size} user credentials")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user credentials", e)
        }
    }
    
    /**
     * Get user credential by email
     */
    suspend fun getUserCredentialByEmail(email: String): UserCredentialEntity? {
        return try {
            val credential = userCredentialDao.getUserCredentialByEmail(email)
            Log.d(TAG, "Retrieved credential for email: $email - Found: ${credential != null}")
            credential
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user credential by email", e)
            null
        }
    }
    
    /**
     * Get all user credentials
     */
    suspend fun getAllUserCredentials(): List<UserCredentialEntity> {
        return try {
            val credentials = userCredentialDao.getAllUserCredentials()
            Log.d(TAG, "Retrieved ${credentials.size} user credentials")
            credentials
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all user credentials", e)
            emptyList()
        }
    }
    
    /**
     * Delete user credential by email
     */
    suspend fun deleteUserCredentialByEmail(email: String) {
        try {
            userCredentialDao.deleteUserCredentialByEmail(email)
            Log.d(TAG, "Deleted credential for email: $email")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user credential", e)
        }
    }
    
    /**
     * Clear all user credentials
     */
    suspend fun clearAllUserCredentials() {
        try {
            userCredentialDao.deleteAllUserCredentials()
            Log.d(TAG, "Cleared all user credentials")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user credentials", e)
        }
    }
}
