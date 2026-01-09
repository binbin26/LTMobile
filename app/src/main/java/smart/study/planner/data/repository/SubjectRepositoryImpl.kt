package smart.study.planner.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import smart.study.planner.data.local.dao.SubjectDao
import smart.study.planner.data.model.Subject
import smart.study.planner.domain.repository.SubjectRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SubjectRepositoryImpl"

@Singleton
class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao,
    private val firebaseAuth: FirebaseAuth
) : SubjectRepository {

    private fun subjectsRef() = firebaseAuth.currentUser?.uid?.let { uid ->
        FirebaseDatabase.getInstance().reference.child("users").child(uid).child("subjects")
    }

    override fun getAllSubjects(): Flow<Result<List<Subject>>> {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            return flow { emit(Result.failure(Exception("User not logged in"))) }
        }
        return subjectDao.getSubjectsByUserIdFlow(userId)
            .map { subjects -> Result.success(subjects) }
            .catch { e ->
                Log.e(TAG, "Error getting subjects from local DB", e)
                emit(Result.failure(e))
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun searchSubjects(query: String): Result<List<Subject>> = withContext(Dispatchers.IO) {
        try {
            val userId = firebaseAuth.currentUser?.uid ?: return@withContext Result.success(emptyList())
            val results = subjectDao.searchSubjects(userId, query.trim(), 10)
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching subjects: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getSubjectById(id: String): Result<Subject?> = withContext(Dispatchers.IO) {
        try {
            Result.success(subjectDao.getSubjectById(id))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subject by id: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getSubjectByName(name: String): Result<Subject?> = withContext(Dispatchers.IO) {
        try {
            val userId = firebaseAuth.currentUser?.uid ?: return@withContext Result.success(null)
            Result.success(subjectDao.getSubjectByName(userId, name.trim()))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subject by name: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun saveSubject(subject: Subject): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            insertSubjectToLocal(subject).getOrThrow()
            syncSubjectToFirebase(subject).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving subject: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSubject(subject: Subject): Result<Unit> {
        return saveSubject(subject)
    }

    override suspend fun deleteSubject(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
            deleteSubjectFromLocal(id).getOrThrow()
            deleteSubjectFromFirebase(userId, id).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subject: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getSubjectsFromFirebase(userId: String): Result<List<Subject>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = subjectsRef()?.get()?.await()
            val subjects = snapshot?.children?.mapNotNull { it.getValue(Subject::class.java) } ?: emptyList()
            if (subjects.isEmpty()) {
                // Fallback to local
                return@withContext getSubjectsByUserId(userId)
            }
            Result.success(subjects)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subjects from Firebase: ${e.message}", e)
            // Fallback to local on error
            getSubjectsByUserId(userId)
        }
    }

    override suspend fun getSubjectsByUserId(userId: String): Result<List<Subject>> = withContext(Dispatchers.IO) {
        try {
            Result.success(subjectDao.getSubjectsByUserId(userId))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subjects by user ID from local DB: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncSubjectToFirebase(subject: Subject): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            subjectsRef()?.child(subject.id)?.setValue(subject)?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing subject to Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteSubjectFromFirebase(userId: String, subjectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth.currentUser?.uid != userId) {
                throw SecurityException("Unauthorized to delete this subject.")
            }
            subjectsRef()?.child(subjectId)?.removeValue()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subject from Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun insertSubjectToLocal(subject: Subject): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            subjectDao.insertSubject(subject)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting subject to local DB: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteSubjectFromLocal(subjectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            subjectDao.deleteSubjectById(subjectId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subject from local DB: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAllLocalSubjects(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            subjectDao.deleteAllByUserId(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all local subjects for user: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromFirebase(): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val firebaseSubjects = getSubjectsFromFirebase(userId).getOrElse {
                    // If firebase fetch fails, try to get from local, otherwise throw
                    return@withContext getSubjectsByUserId(userId).map { }
                }
                deleteAllLocalSubjects(userId).getOrThrow()
                firebaseSubjects.forEach { subject ->
                    insertSubjectToLocal(subject).getOrThrow()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error in syncFromFirebase: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun syncPendingSubjects(): Result<Unit> {
        val userId = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
        return withContext(Dispatchers.IO) {
            try {
                val localSubjects = getSubjectsByUserId(userId).getOrThrow()
                for (subject in localSubjects) {
                    syncSubjectToFirebase(subject).getOrThrow()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error in syncPendingSubjects: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
