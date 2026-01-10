package smart.study.planner.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import smart.study.planner.data.model.Subject

@Dao
interface SubjectDao {

    // ============================================
    // QUERIES - READ OPERATIONS
    // ============================================

    /**
     * Get subjects as Flow for real-time updates
     */
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun getSubjectsByUserIdFlow(userId: String): Flow<List<Subject>>

    /**
     * Get subjects synchronously
     */
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    suspend fun getSubjectsByUserId(userId: String): List<Subject>

    /**
     * Get subject by ID
     */
    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: String): Subject?

    /**
     * Get subject by name (exact match)
     */
    @Query("SELECT * FROM subjects WHERE userId = :userId AND name = :name")
    suspend fun getSubjectByName(userId: String, name: String): Subject?

    /**
     * Search subjects by name (case-insensitive, prefix match)
     */
    @Query("""
        SELECT * FROM subjects 
        WHERE userId = :userId 
        AND LOWER(name) LIKE LOWER(:query) || '%' 
        ORDER BY name ASC 
        LIMIT :limit
    """)
    suspend fun searchSubjects(
        userId: String,
        query: String,
        limit: Int = 10
    ): List<Subject>

    /**
     * Get subjects by semester
     */
    @Query("""
        SELECT * FROM subjects 
        WHERE userId = :userId 
        AND semester = :semester 
        ORDER BY name ASC
    """)
    suspend fun getSubjectsBySemester(
        userId: String,
        semester: String
    ): List<Subject>

    /**
     * Get subjects by teacher name
     */
    @Query("""
        SELECT * FROM subjects 
        WHERE userId = :userId 
        AND teacherName LIKE '%' || :teacherName || '%' 
        ORDER BY name ASC
    """)
    suspend fun getSubjectsByTeacher(
        userId: String,
        teacherName: String
    ): List<Subject>

    /**
     * Get total credits for a user
     */
    @Query("SELECT SUM(credits) FROM subjects WHERE userId = :userId")
    suspend fun getTotalCredits(userId: String): Int?

    /**
     * Get subjects count
     */
    @Query("SELECT COUNT(*) FROM subjects WHERE userId = :userId")
    suspend fun getSubjectsCount(userId: String): Int

    /**
     * Check if subject name exists
     */
    @Query("""
        SELECT COUNT(*) > 0 
        FROM subjects 
        WHERE userId = :userId 
        AND LOWER(name) = LOWER(:name)
    """)
    suspend fun isSubjectNameExists(userId: String, name: String): Boolean

    // ============================================
    // INSERT OPERATIONS
    // ============================================

    /**
     * Insert single subject (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)

    /**
     * Insert multiple subjects (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    // ============================================
    // UPDATE OPERATIONS
    // ============================================

    /**
     * Update subject
     */
    @Update
    suspend fun updateSubject(subject: Subject): Int

    /**
     * Update subject details
     */
    @Query("""
        UPDATE subjects 
        SET name = :name,
            teacherName = :teacherName,
            schedule = :schedule,
            classroom = :classroom,
            credits = :credits,
            semester = :semester,
            description = :description,
            colorHex = :colorHex,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateSubjectDetails(
        id: String,
        name: String,
        teacherName: String,
        schedule: String,
        classroom: String,
        credits: Int,
        semester: String,
        description: String,
        colorHex: String,
        updatedAt: Long
    ): Int

    /**
     * Update subject color
     */
    @Query("""
        UPDATE subjects 
        SET colorHex = :colorHex,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateSubjectColor(
        id: String,
        colorHex: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    // ============================================
    // DELETE OPERATIONS
    // ============================================

    /**
     * Delete subject by ID
     */
    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String): Int

    /**
     * Delete subject entity
     */
    @Delete
    suspend fun deleteSubject(subject: Subject): Int

    /**
     * Delete all subjects for a user
     */
    @Query("DELETE FROM subjects WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String): Int

    /**
     * Delete subjects by semester
     */
    @Query("""
        DELETE FROM subjects 
        WHERE userId = :userId 
        AND semester = :semester
    """)
    suspend fun deleteSubjectsBySemester(
        userId: String,
        semester: String
    ): Int

    // ============================================
    // TRANSACTION OPERATIONS
    // ============================================

    /**
     * Replace all subjects for a user (used for sync)
     */
    @Transaction
    suspend fun replaceAllSubjects(userId: String, subjects: List<Subject>) {
        deleteAllByUserId(userId)
        insertSubjects(subjects)
    }

    /**
     * Batch update subjects
     */
    @Transaction
    suspend fun batchUpdateSubjects(subjects: List<Subject>) {
        subjects.forEach { subject ->
            updateSubject(subject)
        }
    }
}