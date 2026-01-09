package smart.study.planner.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import smart.study.planner.data.model.User
import smart.study.planner.domain.repository.AuthRepository
import smart.study.planner.domain.repository.UserRepository
import smart.study.planner.presentation.util.UiState
import javax.inject.Inject

/**
 * ViewModel for Profile screen
 * Handles user profile operations: load, update, avatar upload, logout
 * Manages real-time user data synchronization with Firebase
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
        
        // Helper function to get formatted timestamp
        private fun getTimestamp(): String {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            return sdf.format(java.util.Date())
        }
    }

    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // User loading state
    private val _userLoadingState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userLoadingState: StateFlow<UiState<User>> = _userLoadingState.asStateFlow()

    // User profile state
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Logout state
    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState: StateFlow<UiState<Unit>> = _logoutState.asStateFlow()

    // Avatar upload state
    private val _avatarUploadState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val avatarUploadState: StateFlow<UiState<String>> = _avatarUploadState.asStateFlow()

    // Profile update state
    private val _profileUpdateState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val profileUpdateState: StateFlow<UiState<Unit>> = _profileUpdateState.asStateFlow()

    // Real-time sync state
    private val _isObservingChanges = MutableStateFlow(false)
    val isObservingChanges: StateFlow<Boolean> = _isObservingChanges.asStateFlow()

    init {
        loadCurrentUser()
        observeUserChanges()
    }

    /**
     * Load current user profile
     * Fetches full user data including phone, email, gender, school, major, etc.
     */
    fun loadCurrentUser() {
        val timestamp = getTimestamp()
        Log.d(TAG, "[$timestamp] LOAD_START: Bắt đầu tải thông tin user hiện tại")
        viewModelScope.launch {
            _userLoadingState.value = UiState.Loading
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val loadStartTime = System.currentTimeMillis()
                val result = userRepository.getCurrentUser()
                result.fold(
                    onSuccess = { user ->
                        val loadDuration = System.currentTimeMillis() - loadStartTime
                        if (user != null) {
                            Log.d(TAG, "[$timestamp] LOAD_SUCCESS: Tải user thành công trong ${loadDuration}ms")
                            Log.d(TAG, "[$timestamp] USER_DATA: " +
                                "id=${user.id}, " +
                                "email=${user.email}, " +
                                "displayName=${user.displayName}, " +
                                "phoneNumber=${user.phoneNumber}, " +
                                "gender=${user.gender}, " +
                                "school=${user.school}, " +
                                "studentId=${user.studentId}, " +
                                "major=${user.major}, " +
                                "yearOfStudy=${user.yearOfStudy}, " +
                                "dateOfBirth=${user.dateOfBirth}, " +
                                "avatarUrl=${user.avatarUrl}, " +
                                "bio=${user.bio}, " +
                                "createdAt=${user.createdAt}, " +
                                "updatedAt=${user.updatedAt}")
                            _currentUser.value = user
                            _userProfile.value = user
                            _userLoadingState.value = UiState.Success(user)
                            _errorMessage.value = null
                        } else {
                            Log.d(TAG, "[$timestamp] LOAD_NULL: User data là null")
                            _currentUser.value = null
                            _userProfile.value = null
                            _userLoadingState.value = UiState.Error(Exception("User data không tồn tại"))
                            _errorMessage.value = "Không thể tải thông tin người dùng"
                        }
                    },
                    onFailure = { error ->
                        val loadDuration = System.currentTimeMillis() - loadStartTime
                        Log.e(TAG, "[$timestamp] LOAD_ERROR: Lỗi tải user trong ${loadDuration}ms. " +
                            "Error: ${error.message}", error)
                        _userLoadingState.value = UiState.Error(error)
                        _errorMessage.value = error.message ?: "Không thể tải thông tin người dùng"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "[$timestamp] LOAD_EXCEPTION: Ngoại lệ khi tải user. " +
                    "Error: ${e.message}", e)
                _userLoadingState.value = UiState.Error(e)
                _errorMessage.value = e.message ?: "Lỗi không xác định"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "[$timestamp] LOAD_END: Kết thúc quá trình tải user")
            }
        }
    }

    /**
     * Observe real-time user changes from Firebase
     * Automatically updates currentUser when data changes
     * Filters out duplicate emissions with distinctUntilChanged()
     */
    fun observeUserChanges() {
        val timestamp = getTimestamp()
        Log.d(TAG, "[$timestamp] OBSERVE_START: Bắt đầu observe user changes từ Firebase")
        _isObservingChanges.value = true
        var changeCount = 0
        
        userRepository.observeCurrentUser()
            .distinctUntilChanged()
            .onEach { result ->
                changeCount++
                val currentTimestamp = getTimestamp()
                Log.d(TAG, "[$currentTimestamp] OBSERVE_EMIT: Nhận data mới từ Firebase (Change #$changeCount)")
                
                result.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            Log.d(TAG, "[$currentTimestamp] DATA_CHANGED: Change #$changeCount - User data thay đổi")
                            
                            // Detailed logging as per requirements
                            Log.d(TAG, """
                                ┌─────────────────────────────────────
                                │ FIREBASE DATA RECEIVED
                                ├─────────────────────────────────────
                                │ ID: ${user.id}
                                │ Email: ${user.email}
                                │ Display Name: ${user.displayName}
                                │ Avatar: ${user.avatarUrl ?: "NOT SET"}
                                │ Phone: ${user.phoneNumber ?: "NOT SET"}
                                │ DOB: ${user.dateOfBirth?.let { java.util.Date(it) } ?: "NOT SET"}
                                │ Gender: ${user.gender ?: "NOT SET"}
                                │ Student ID: ${user.studentId ?: "NOT SET"}
                                │ School: ${user.school ?: "NOT SET"}
                                │ Major: ${user.major ?: "NOT SET"}
                                │ Year: ${user.yearOfStudy ?: "NOT SET"}
                                │ Bio: ${user.bio?.take(50) ?: "NOT SET"}
                                │ Created: ${java.util.Date(user.createdAt)}
                                │ Updated: ${java.util.Date(user.updatedAt)}
                                └─────────────────────────────────────
                            """.trimIndent())
                            
                            Log.d(TAG, "[$currentTimestamp] STATE_UPDATE: Cập nhật _currentUser ngay lập tức")
                            _currentUser.value = user
                            Log.d(TAG, "[$currentTimestamp] STATE_UPDATE: Cập nhật _userProfile ngay lập tức")
                            _userProfile.value = user
                            _errorMessage.value = null
                            Log.d(TAG, "[$currentTimestamp] STATE_SYNC_COMPLETE: Đồng bộ state hoàn tất (Change #$changeCount)")
                        } else {
                            Log.d(TAG, "[$currentTimestamp] DATA_NULL: Change #$changeCount - User data thay đổi thành null")
                            _currentUser.value = null
                            _userProfile.value = null
                            Log.d(TAG, "[$currentTimestamp] STATE_SYNC_COMPLETE: Đồng bộ state hoàn tất - User null (Change #$changeCount)")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "[$currentTimestamp] OBSERVE_ERROR: Change #$changeCount - Lỗi observe user changes. " +
                            "Error: ${error.message}", error)
                        Log.d(TAG, "[$currentTimestamp] ERROR_LOG_DETAILS: StackTrace: ${error.stackTrace.joinToString("\n")}")
                        _errorMessage.value = error.message ?: "Lỗi đồng bộ dữ liệu"
                    }
                )
            }
            .launchIn(viewModelScope)
        
        Log.d(TAG, "[$timestamp] OBSERVE_SETUP_COMPLETE: Flow observer đã được thiết lập thành công")
    }

    /**
     * Update user profile with new information
     * Saves changes to Firebase Realtime Database
     */
    fun updateUserProfile(user: User) {
        val timestamp = getTimestamp()
        Log.d(TAG, "[$timestamp] UPDATE_START: Bắt đầu cập nhật user profile")
        Log.d(TAG, "[$timestamp] UPDATE_DATA_BEFORE: " +
            "id=${user.id}, " +
            "email=${user.email}, " +
            "displayName=${user.displayName}, " +
            "phoneNumber=${user.phoneNumber}, " +
            "gender=${user.gender}, " +
            "school=${user.school}, " +
            "studentId=${user.studentId}, " +
            "major=${user.major}, " +
            "yearOfStudy=${user.yearOfStudy}, " +
            "dateOfBirth=${user.dateOfBirth}, " +
            "avatarUrl=${user.avatarUrl}, " +
            "bio=${user.bio}")
        
        viewModelScope.launch {
            _profileUpdateState.value = UiState.Loading

            try {
                val updateStartTime = System.currentTimeMillis()
                val result = userRepository.updateUser(user)
                result.fold(
                    onSuccess = {
                        val updateDuration = System.currentTimeMillis() - updateStartTime
                        Log.d(TAG, "[$timestamp] UPDATE_SUCCESS: Cập nhật user profile thành công trong ${updateDuration}ms")
                        
                        // Update local state with new user data
                        val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
                        Log.d(TAG, "[$timestamp] UPDATE_DATA_AFTER: " +
                            "id=${updatedUser.id}, " +
                            "email=${updatedUser.email}, " +
                            "displayName=${updatedUser.displayName}, " +
                            "phoneNumber=${updatedUser.phoneNumber}, " +
                            "gender=${updatedUser.gender}, " +
                            "school=${updatedUser.school}, " +
                            "studentId=${updatedUser.studentId}, " +
                            "major=${updatedUser.major}, " +
                            "yearOfStudy=${updatedUser.yearOfStudy}, " +
                            "dateOfBirth=${updatedUser.dateOfBirth}, " +
                            "avatarUrl=${updatedUser.avatarUrl}, " +
                            "bio=${updatedUser.bio}, " +
                            "updatedAt=${updatedUser.updatedAt}")
                        
                        _currentUser.value = updatedUser
                        _userProfile.value = updatedUser
                        _profileUpdateState.value = UiState.Success(Unit)
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        val updateDuration = System.currentTimeMillis() - updateStartTime
                        Log.e(TAG, "[$timestamp] UPDATE_ERROR: Lỗi cập nhật user profile trong ${updateDuration}ms. " +
                            "Error: ${error.message}", error)
                        _profileUpdateState.value = UiState.Error(error)
                        _errorMessage.value = error.message ?: "Không thể cập nhật thông tin"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "[$timestamp] UPDATE_EXCEPTION: Ngoại lệ khi cập nhật user profile. " +
                    "Error: ${e.message}", e)
                _profileUpdateState.value = UiState.Error(e)
                _errorMessage.value = e.message ?: "Lỗi không xác định"
            } finally {
                Log.d(TAG, "[$timestamp] UPDATE_END: Kết thúc quá trình cập nhật user profile")
            }
        }
    }

    /**
     * Upload user avatar
     */
    fun uploadAvatar(uri: Uri) {
        Log.d(TAG, "Bắt đầu upload avatar")
        viewModelScope.launch {
            _avatarUploadState.value = UiState.Loading

            try {
                val result = userRepository.uploadAvatar(uri)
                result.fold(
                    onSuccess = { avatarUrl ->
                        Log.d(TAG, "Upload avatar thành công: $avatarUrl")
                        // Update user profile with new avatar URL
                        val updatedUser = _userProfile.value?.copy(
                            avatarUrl = avatarUrl,
                            updatedAt = System.currentTimeMillis()
                        )
                        if (updatedUser != null) {
                            _currentUser.value = updatedUser
                            _userProfile.value = updatedUser
                        }
                        _avatarUploadState.value = UiState.Success(avatarUrl)
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Lỗi upload avatar", error)
                        _avatarUploadState.value = UiState.Error(error)
                        _errorMessage.value = error.message ?: "Không thể tải ảnh đại diện"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ngoại lệ khi upload avatar", e)
                _avatarUploadState.value = UiState.Error(e)
                _errorMessage.value = e.message ?: "Lỗi không xác định"
            }
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        Log.d(TAG, "Bắt đầu đăng xuất")
        viewModelScope.launch {
            _logoutState.value = UiState.Loading

            try {
                val result = authRepository.logout()
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Đăng xuất thành công")
                        _logoutState.value = UiState.Success(Unit)
                        _currentUser.value = null
                        _userProfile.value = null
                        _isObservingChanges.value = false
                        _errorMessage.value = null
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Lỗi đăng xuất", error)
                        _logoutState.value = UiState.Error(error)
                        _errorMessage.value = error.message ?: "Không thể đăng xuất"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ngoại lệ khi đăng xuất", e)
                _logoutState.value = UiState.Error(e)
                _errorMessage.value = e.message ?: "Lỗi không xác định"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        Log.d(TAG, "Xóa error message")
        _errorMessage.value = null
    }

    /**
     * Reset logout state
     */
    fun resetLogoutState() {
        Log.d(TAG, "Reset logout state")
        _logoutState.value = UiState.Idle
    }

    /**
     * Reset avatar upload state
     */
    fun resetAvatarUploadState() {
        Log.d(TAG, "Reset avatar upload state")
        _avatarUploadState.value = UiState.Idle
    }

    /**
     * Reset profile update state
     */
    fun resetProfileUpdateState() {
        Log.d(TAG, "Reset profile update state")
        _profileUpdateState.value = UiState.Idle
    }

    /**
     * Reload user profile from database
     */
    fun reloadUserProfile() {
        Log.d(TAG, "Reload user profile")
        loadCurrentUser()
    }
}
