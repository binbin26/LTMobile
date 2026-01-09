package smart.study.planner.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import smart.study.planner.data.model.Subject

@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun getSubjectsByUserIdFlow(userId: String): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    suspend fun getSubjectsByUserId(userId: String): List<Subject>

    @Query("SELECT * FROM subjects WHERE userId = :userId AND name LIKE :query || '%' ORDER BY name ASC LIMIT 10")
    suspend fun searchSubjects(userId: String, query: String): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: String): Subject?

    @Query("SELECT * FROM subjects WHERE userId = :userId AND name = :name")
    suspend fun getSubjectByName(userId: String, name: String): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    @Query("DELETE FROM subjects WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String): Int
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    suspend fun getSubjectsByUserIdSync(userId: String): List<Subject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Delete
    suspend fun deleteSubject(subject: Subject)
    @Query("SELECT * FROM subjects WHERE userId = :userId AND LOWER(name) LIKE LOWER(:query) || '%' ORDER BY name ASC LIMIT :limit")
    suspend fun searchSubjects(userId: String, query: String, limit: Int = 10): List<Subject>
}
