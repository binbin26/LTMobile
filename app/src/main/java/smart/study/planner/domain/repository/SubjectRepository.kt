package smart.study.planner.domain.repository

import smart.study.planner.data.model.Subject
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Subject operations
 * Defines the contract for subject data access
 */
interface SubjectRepository {

    fun getAllSubjects(): Flow<Result<List<Subject>>>

    suspend fun searchSubjects(query: String): Result<List<Subject>>

    suspend fun getSubjectById(id: String): Result<Subject?>

    suspend fun getSubjectByName(name: String): Result<Subject?>

    suspend fun saveSubject(subject: Subject): Result<Unit>

    suspend fun updateSubject(subject: Subject): Result<Unit>

    suspend fun deleteSubject(id: String): Result<Unit>

    suspend fun getSubjectsFromFirebase(userId: String): Result<List<Subject>>

    suspend fun getSubjectsByUserId(userId: String): Result<List<Subject>>

    suspend fun syncSubjectToFirebase(subject: Subject): Result<Unit>

    suspend fun deleteSubjectFromFirebase(userId: String, subjectId: String): Result<Unit>

    suspend fun insertSubjectToLocal(subject: Subject): Result<Unit>

    suspend fun deleteSubjectFromLocal(subjectId: String): Result<Unit>

    suspend fun deleteAllLocalSubjects(userId: String): Result<Unit>
    suspend fun syncFromFirebase(): Result<Unit>

    suspend fun syncPendingSubjects(): Result<Unit>
}
