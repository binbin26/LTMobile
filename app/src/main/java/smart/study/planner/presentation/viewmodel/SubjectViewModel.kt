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

    init {
        Log.d(TAG, "SubjectViewModel initialized")
        refreshFromFirebase() // Initial load from Firebase
    }

    fun refreshFromFirebase() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "User not logged in, cannot refresh subjects.")
                _subjectsState.value = emptyList()
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
                } else {
                    Log.d(TAG, "No subjects in Firebase, loading from local")
                    loadSubjectsFromLocal(userId)
                }
            }.onFailure { e ->
                Log.e(TAG, "Error fetching from Firebase, falling back to local. Error: ${e.message}")
                loadSubjectsFromLocal(userId)
            }
        }
    }
    
    private fun loadSubjectsFromLocal(userId: String) {
        viewModelScope.launch {
            subjectRepository.getSubjectsByUserId(userId).onSuccess { localSubjects ->
                Log.d(TAG, "Loaded ${localSubjects.size} subjects from local")
                _subjectsState.value = localSubjects
            }.onFailure { e ->
                Log.e(TAG, "Error loading subjects from local: ${e.message}")
                _subjectsState.value = emptyList()
            }
        }
    }

    suspend fun addSubject(name: String): Result<Subject> {
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                return Result.failure(Exception("Tên môn học không được để trống"))
            }

            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Check if subject already exists
            val existingSubject = subjectRepository.getSubjectByName(trimmedName).getOrNull()

            if (existingSubject != null) {
                Log.d(TAG, "Subject already exists: ${existingSubject.name}")
                return Result.success(existingSubject)
            }

            // Create new subject
            val newSubject = Subject(
                name = trimmedName,
                colorHex = generateColorForSubject(trimmedName),
                userId = userId
            )

            val syncResult = subjectRepository.syncSubjectToFirebase(newSubject)

            if (syncResult.isSuccess) {
                Log.d(TAG, "✅ Subject synced to Firebase: ${newSubject.name}")

                // Save to Local
                subjectRepository.insertSubjectToLocal(newSubject)

                // Reload to update UI
                refreshFromFirebase()

                // Return Subject object
                Result.success(newSubject)
            } else {
                val error = syncResult.exceptionOrNull() ?: Exception("Unknown error")
                Log.e(TAG, "❌ Error adding subject: ${error.message}", error)
                Result.failure(error)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception adding subject: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun searchSubjects(query: String): List<Subject> {
        val userId = authRepository.getCurrentUserId() ?: return emptyList()
        return if (query.isBlank()) {
            _subjectsState.value
        } else {
            // This search should also be by userId, assuming repository handles it
            subjectRepository.searchSubjects(query).getOrElse { emptyList() }
                .filter { it.userId == userId }
        }
    }

    private fun generateColorForSubject(name: String): String {
        val colors = listOf(
            "#4285F4", "#34A853", "#FBBC05", "#EA4335", "#9C27B0",
            "#FF9800", "#00BCD4", "#E91E63", "#795548", "#607D8B"
        )
        val hash = name.hashCode()
        val index = Math.abs(hash) % colors.size
        return colors[index]
    }

    suspend fun getSubjectById(id: String): Subject? {
        val userId = authRepository.getCurrentUserId() ?: return null
        return subjectRepository.getSubjectById(id).getOrNull()?.takeIf { it.userId == userId }
    }
}
