package smart.study.planner.data.manager

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthStateManager"

@Singleton
class AuthStateManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Observe authentication state changes
     */
    fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                Log.d(TAG, "Auth State: AUTHENTICATED - UID: ${user.uid}")
                // Force token refresh to ensure it's valid
                user.getIdToken(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Token refreshed successfully")
                        trySend(AuthState.Authenticated(user.uid))
                    } else {
                        Log.e(TAG, "Token refresh failed", task.exception)
                        trySend(AuthState.TokenExpired)
                    }
                }
            } else {
                Log.d(TAG, "Auth State: UNAUTHENTICATED")
                trySend(AuthState.Unauthenticated)
            }
        }
        
        firebaseAuth.addAuthStateListener(listener)
        
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }
    
    /**
     * Force refresh current auth token
     */
    suspend fun refreshToken(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                user.getIdToken(true).await()
                Log.d(TAG, "Token force refreshed")
                Result.success(Unit)
            } else {
                Result.failure(Exception("No authenticated user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get current user ID with logging
     */
    fun getCurrentUserId(): String? {
        val user = firebaseAuth.currentUser
        if (user != null) {
            Log.d(TAG, "Current User - UID: ${user.uid}, Email: ${user.email}")
        } else {
            Log.w(TAG, "No authenticated user found")
        }
        return user?.uid
    }
}

sealed class AuthState {
    data class Authenticated(val userId: String) : AuthState()
    object Unauthenticated : AuthState()
    object TokenExpired : AuthState()
}
