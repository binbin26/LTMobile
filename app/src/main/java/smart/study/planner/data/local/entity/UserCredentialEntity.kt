package smart.study.planner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local entity to store user credentials for verification
 * Used in forgot password flow to verify email and get password for reset
 */
@Entity(tableName = "user_credentials")
data class UserCredentialEntity(
    @PrimaryKey
    val email: String,
    val password: String,
    val userId: String = "",
    val displayName: String = "",
    val syncedAt: Long = System.currentTimeMillis()
)
