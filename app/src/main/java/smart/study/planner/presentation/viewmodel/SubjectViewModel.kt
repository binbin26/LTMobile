package smart.study.planner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import smart.study.planner.data.model.Subject
import smart.study.planner.domain.repository.AuthRepository
import smart.study.planner.domain.repository.SubjectRepository
import javax.inject.Inject

private const val TAG = "SubjectViewModel"

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _subjectsState = MutableStateFlow<List<Subject>>(emptyList())
    val subjectsState: StateFlow<List<Subject>> = _subjectsState.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    init {
        Log.d(TAG, "SubjectViewModel initialized")
        refreshFromFirebase()
    }

    /**
     * Refresh subjects from Firebase
     */
    fun refreshFromFirebase() {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading

            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "User not logged in, cannot refresh subjects.")
                _subjectsState.value = emptyList()
                _operationState.value = OperationState.Error("User not logged in")
                return@launch
            }

            Log.d(TAG, "Refreshing subjects for user: $userId")

            subjectRepository.getSubjectsFromFirebase(userId).onSuccess { firebaseSubjects ->
                if (firebaseSubjects.isNotEmpty()) {
                    Log.d(TAG, "✅ Using Firebase data: ${firebaseSubjects.size} subjects")
                    _subjectsState.value = firebaseSubjects

                    // Sync to local
                    subjectRepository.deleteAllLocalSubjects(userId)
                    firebaseSubjects.forEach { subject ->
                        subjectRepository.insertSubjectToLocal(subject)
                    }
                    Log.d(TAG, "✅ Synced ${firebaseSubjects.size} subjects from Firebase to Local")
                    _operationState.value = OperationState.Success("Loaded ${firebaseSubjects.size} subjects")
                } else {
                    Log.d(TAG, "No subjects in Firebase, loading from local")
                    loadSubjectsFromLocal(userId)
                }
            }.onFailure { e ->
                Log.e(TAG, "Error fetching from Firebase, falling back to local. Error: ${e.message}")
                loadSubjectsFromLocal(userId)
                _operationState.value = OperationState.Error("Failed to load from cloud: ${e.message}")
            }
        }
    }

    /**
     * Load subjects from local database
     */
    private fun loadSubjectsFromLocal(userId: String) {
        viewModelScope.launch {
            subjectRepository.getSubjectsByUserId(userId).onSuccess { localSubjects ->
                Log.d(TAG, "Loaded ${localSubjects.size} subjects from local")
                _subjectsState.value = localSubjects
                _operationState.value = OperationState.Success("Loaded ${localSubjects.size} subjects from local")
            }.onFailure { e ->
                Log.e(TAG, "Error loading subjects from local: ${e.message}")
                _subjectsState.value = emptyList()
                _operationState.value = OperationState.Error("Failed to load subjects: ${e.message}")
            }
        }
    }

    /**
     * Add new subject
     * @param name Subject name (required)
     * @param teacherName Teacher name (optional)
     * @param schedule Class schedule (optional)
     * @param classroom Classroom location (optional)
     * @param credits Number of credits (optional)
     * @param semester Academic semester (optional)
     * @param description Additional notes (optional)
     * @param colorHex Color for the subject (optional)
     */
    suspend fun addSubject(
        name: String,
        teacherName: String = "",
        schedule: String = "",
        classroom: String = "",
        credits: Int = 0,
        semester: String = "",
        description: String = "",
        colorHex: String? = null
    ): Result<Subject> {
        return try {
            _operationState.value = OperationState.Loading

            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                _operationState.value = OperationState.Error("Subject name cannot be empty")
                return Result.failure(Exception("Tên môn học không được để trống"))
            }

            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Check if subject already exists
            val existingSubject = subjectRepository.getSubjectByName(trimmedName).getOrNull()

            if (existingSubject != null) {
                Log.d(TAG, "Subject already exists: ${existingSubject.name}")
                _operationState.value = OperationState.Success("Subject already exists")
                return Result.success(existingSubject)
            }

            // Create new subject with all fields
            val newSubject = Subject(
                name = trimmedName,
                colorHex = colorHex ?: generateColorForSubject(trimmedName),
                userId = userId,
                teacherName = teacherName.trim(),
                schedule = schedule.trim(),
                classroom = classroom.trim(),
                credits = credits,
                semester = semester.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Save to Firebase
            val syncResult = subjectRepository.syncSubjectToFirebase(newSubject)

            if (syncResult.isSuccess) {
                Log.d(TAG, "✅ Subject synced to Firebase: ${newSubject.name}")

                // Save to Local
                subjectRepository.insertSubjectToLocal(newSubject)

                // Reload to update UI
                refreshFromFirebase()

                _operationState.value = OperationState.Success("Subject added successfully")
                Result.success(newSubject)
            } else {
                val error = syncResult.exceptionOrNull() ?: Exception("Unknown error")
                Log.e(TAG, "❌ Error adding subject: ${error.message}", error)
                _operationState.value = OperationState.Error("Failed to add subject: ${error.message}")
                Result.failure(error)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception adding subject: ${e.message}", e)
            _operationState.value = OperationState.Error("Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update existing subject
     */
    suspend fun updateSubject(subject: Subject): Result<Unit> {
        return try {
            _operationState.value = OperationState.Loading

            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Update timestamp
            val updatedSubject = subject.copy(
                userId = userId,
                updatedAt = System.currentTimeMillis()
            )

            // Save to Firebase
            val syncResult = subjectRepository.syncSubjectToFirebase(updatedSubject)

            if (syncResult.isSuccess) {
                // Update local
                subjectRepository.insertSubjectToLocal(updatedSubject)

                // Refresh UI
                refreshFromFirebase()

                Log.d(TAG, "✅ Subject updated: ${updatedSubject.name}")
                _operationState.value = OperationState.Success("Subject updated successfully")
                Result.success(Unit)
            } else {
                val error = syncResult.exceptionOrNull() ?: Exception("Unknown error")
                Log.e(TAG, "❌ Error updating subject: ${error.message}")
                _operationState.value = OperationState.Error("Failed to update: ${error.message}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception updating subject: ${e.message}", e)
            _operationState.value = OperationState.Error("Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete subject by ID
     */
    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        return try {
            _operationState.value = OperationState.Loading

            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Delete from Firebase
            subjectRepository.deleteSubjectFromFirebase(userId, subjectId)

            // Delete from Local
            subjectRepository.deleteSubjectFromLocal(subjectId)

            // Refresh UI
            refreshFromFirebase()

            Log.d(TAG, "✅ Subject deleted: $subjectId")
            _operationState.value = OperationState.Success("Subject deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting subject: ${e.message}", e)
            _operationState.value = OperationState.Error("Failed to delete: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Search subjects by query
     */
    suspend fun searchSubjects(query: String): List<Subject> {
        val userId = authRepository.getCurrentUserId() ?: return emptyList()
        return if (query.isBlank()) {
            _subjectsState.value
        } else {
            subjectRepository.searchSubjects(query).getOrElse { emptyList() }
                .filter { it.userId == userId }
        }
    }

    /**
     * Get subject by ID
     */
    suspend fun getSubjectById(id: String): Subject? {
        val userId = authRepository.getCurrentUserId() ?: return null
        return subjectRepository.getSubjectById(id).getOrNull()?.takeIf { it.userId == userId }
    }

    /**
     * Get subjects by semester
     */
    suspend fun getSubjectsBySemester(semester: String): List<Subject> {
        return _subjectsState.value.filter { it.semester == semester }
    }

    /**
     * Get total credits
     */
    fun getTotalCredits(): Int {
        return _subjectsState.value.sumOf { it.credits }
    }

    /**
     * Generate color for subject based on name hash
     */
    private fun generateColorForSubject(name: String): String {
        val colors = listOf(
            "#4285F4", // Blue
            "#34A853", // Green
            "#FBBC05", // Yellow
            "#EA4335", // Red
            "#9C27B0", // Purple
            "#FF9800", // Orange
            "#00BCD4", // Cyan
            "#E91E63", // Pink
            "#795548", // Brown
            "#607D8B"  // Blue Grey
        )
        val hash = name.hashCode()
        val index = Math.abs(hash) % colors.size
        return colors[index]
    }

    /**
     * Clear operation state
     */
    fun clearOperationState() {
        _operationState.value = OperationState.Idle
    }
}

/**
 * Operation state for UI feedback
 */
sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}