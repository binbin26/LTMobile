package smart.study.planner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import smart.study.planner.data.local.entity.UserCredentialEntity

/**
 * DAO for UserCredentialEntity
 */
@Dao
interface UserCredentialDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCredential(credential: UserCredentialEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCredentials(credentials: List<UserCredentialEntity>)
    
    @Query("SELECT * FROM user_credentials WHERE email = :email")
    suspend fun getUserCredentialByEmail(email: String): UserCredentialEntity?
    
    @Query("SELECT * FROM user_credentials")
    suspend fun getAllUserCredentials(): List<UserCredentialEntity>
    
    @Query("DELETE FROM user_credentials WHERE email = :email")
    suspend fun deleteUserCredentialByEmail(email: String)
    
    @Delete
    suspend fun deleteUserCredential(credential: UserCredentialEntity)
    
    @Query("DELETE FROM user_credentials")
    suspend fun deleteAllUserCredentials()
}
