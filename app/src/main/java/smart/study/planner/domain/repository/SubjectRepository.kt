package smart.study.planner.domain.repository

import kotlinx.coroutines.flow.Flow
import smart.study.planner.data.model.Subject

/**
 * Repository interface for Subject operations
 * Handles both local (Room) and remote (Firebase) data sources
 */
interface SubjectRepository {

    // ============================================
    // READ OPERATIONS
    // ============================================

    /**
     * Get all subjects as Flow for real-time updates
     * @return Flow of Result containing list of subjects
     */
    fun getAllSubjects(): Flow<Result<List<Subject>>>

    /**
     * Search subjects by query string
     * @param query Search string
     * @return Result containing filtered list of subjects
     */
    suspend fun searchSubjects(query: String): Result<List<Subject>>

    /**
     * Get subject by ID
     * @param id Subject ID
     * @return Result containing Subject or null if not found
     */
    suspend fun getSubjectById(id: String): Result<Subject?>

    /**
     * Get subject by name
     * @param name Subject name
     * @return Result containing Subject or null if not found
     */
    suspend fun getSubjectByName(name: String): Result<Subject?>

    /**
     * Get subjects by user ID
     * @param userId User ID
     * @return Result containing list of subjects for the user
     */
    suspend fun getSubjectsByUserId(userId: String): Result<List<Subject>>

    // ============================================
    // WRITE OPERATIONS
    // ============================================

    /**
     * Save new subject (creates if not exists, updates if exists)
     * @param subject Subject to save
     * @return Result of the operation
     */
    suspend fun saveSubject(subject: Subject): Result<Unit>

    /**
     * Update existing subject
     * @param subject Subject with updated data
     * @return Result of the operation
     */
    suspend fun updateSubject(subject: Subject): Result<Unit>

    /**
     * Delete subject by ID
     * @param id Subject ID to delete
     * @return Result of the operation
     */
    suspend fun deleteSubject(id: String): Result<Unit>

    // ============================================
    // FIREBASE OPERATIONS
    // ============================================

    /**
     * Get subjects from Firebase
     * @param userId User ID
     * @return Result containing list of subjects from Firebase
     */
    suspend fun getSubjectsFromFirebase(userId: String): Result<List<Subject>>

    /**
     * Sync subject to Firebase
     * @param subject Subject to sync
     * @return Result of the operation
     */
    suspend fun syncSubjectToFirebase(subject: Subject): Result<Unit>

    /**
     * Delete subject from Firebase
     * @param userId User ID
     * @param subjectId Subject ID to delete
     * @return Result of the operation
     */
    suspend fun deleteSubjectFromFirebase(userId: String, subjectId: String): Result<Unit>

    // ============================================
    // LOCAL DATABASE OPERATIONS
    // ============================================

    /**
     * Insert subject to local database
     * @param subject Subject to insert
     * @return Result of the operation
     */
    suspend fun insertSubjectToLocal(subject: Subject): Result<Unit>

    /**
     * Delete subject from local database
     * @param subjectId Subject ID to delete
     * @return Result of the operation
     */
    suspend fun deleteSubjectFromLocal(subjectId: String): Result<Unit>

    /**
     * Delete all local subjects for a user
     * @param userId User ID
     * @return Result of the operation
     */
    suspend fun deleteAllLocalSubjects(userId: String): Result<Unit>

    // ============================================
    // SYNC OPERATIONS
    // ============================================

    /**
     * Sync subjects from Firebase to local database
     * Replaces all local data with Firebase data
     * @return Result of the operation
     */
    suspend fun syncFromFirebase(): Result<Unit>

    /**
     * Sync pending local subjects to Firebase
     * Uploads all local subjects to Firebase
     * @return Result of the operation
     */
    suspend fun syncPendingSubjects(): Result<Unit>
}