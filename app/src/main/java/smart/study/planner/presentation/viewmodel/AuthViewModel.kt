package smart.study.planner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import smart.study.planner.data.model.User
import smart.study.planner.domain.repository.AuthRepository
import smart.study.planner.presentation.util.UiState
import javax.inject.Inject

/**
 * ViewModel cho quản lý xác thực người dùng
 * Xử lý đăng nhập, đăng ký, đăng xuất
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    // Trạng thái xác thực
    private val _authState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val authState: StateFlow<UiState<User>> = _authState.asStateFlow()

    // Trạng thái đăng nhập
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Lỗi xác thực
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        // Kiểm tra trạng thái đăng nhập khi ViewModel được tạo
        checkLoginStatus()
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập chưa
     */
    private fun checkLoginStatus() {
        _isLoggedIn.value = authRepository.isUserLoggedIn()
        Log.d(TAG, "Login status: ${_isLoggedIn.value}")
    }

    /**
     * Đăng nhập bằng email và password
     */
    fun login(email: String, password: String) {
        if (!validateEmail(email)) {
            _authError.value = "Email không hợp lệ"
            return
        }
        
        if (password.length < 6) {
            _authError.value = "Mật khẩu phải có ít nhất 6 ký tự"
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading
            _authError.value = null
            
            try {
                val result = authRepository.login(email, password)
                result.fold(
                    onSuccess = { user ->
                        _authState.value = UiState.Success(user)
                        _isLoggedIn.value = true
                        _authError.value = null
                        Log.d(TAG, "Đăng nhập thành công: ${user.email}")
                    },
                    onFailure = { error ->
                        val errorMessage = when {
                            error.message?.contains("user-not-found") == true -> 
                                "Email không tồn tại"
                            error.message?.contains("wrong-password") == true -> 
                                "Mật khẩu không chính xác"
                            error.message?.contains("too-many-requests") == true -> 
                                "Quá nhiều lần đăng nhập thất bại, vui lòng thử lại sau"
                            else -> error.message ?: "Đăng nhập thất bại"
                        }
                        _authState.value = UiState.Error(error)
                        _authError.value = errorMessage
                        Log.e(TAG, "Lỗi đăng nhập", error)
                    }
                )
            } catch (e: Exception) {
                _authState.value = UiState.Error(e)
                _authError.value = "Lỗi: ${e.message}"
                Log.e(TAG, "Ngoại lệ đăng nhập", e)
            }
        }
    }

    /**
     * Đăng ký tài khoản mới
     */
    fun register(
        email: String,
        password: String,
        name: String,
        phoneNumber: String? = null,
        dateOfBirth: String? = null,
        gender: String? = null,
        studentId: String? = null,
        school: String? = null,
        major: String? = null,
        yearOfStudy: Int? = null,
        bio: String? = null
    ) {
        // Validation: Tên không được trống
        if (name.isBlank()) {
            _authError.value = "Tên không được để trống"
            return
        }

        // Validation: Email phải đúng định dạng
        if (!validateEmail(email)) {
            _authError.value = "Email không hợp lệ"
            return
        }
        
        // Validation: Mật khẩu tối thiểu 6 ký tự
        if (password.length < 6) {
            _authError.value = "Mật khẩu phải có ít nhất 6 ký tự"
            return
        }

        // Validation: Số điện thoại (nếu có) phải đúng định dạng
        if (!phoneNumber.isNullOrBlank() && !validatePhoneNumber(phoneNumber)) {
            _authError.value = "Số điện thoại không hợp lệ"
            return
        }

        // Validation: Ngày sinh phải có định dạng dd/MM/yyyy (nếu có)
        if (!dateOfBirth.isNullOrBlank() && !validateDateFormat(dateOfBirth)) {
            _authError.value = "Ngày sinh phải có định dạng dd/MM/yyyy"
            return
        }

        viewModelScope.launch {
            _authState.value = UiState.Loading
            _authError.value = null
            
            try {
                val result = authRepository.register(
                    email = email,
                    password = password,
                    name = name,
                    phoneNumber = phoneNumber,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    studentId = studentId,
                    school = school,
                    major = major,
                    yearOfStudy = yearOfStudy,
                    bio = bio
                )
                result.fold(
                    onSuccess = { user ->
                        _authState.value = UiState.Success(user)
                        _isLoggedIn.value = true
                        _authError.value = null
                        Log.d(TAG, "Đăng ký thành công: ${user.email}")
                    },
                    onFailure = { error ->
                        val errorMessage = when {
                            error.message?.contains("email-already-in-use") == true -> 
                                "Email đã được sử dụng"
                            error.message?.contains("weak-password") == true -> 
                                "Mật khẩu quá yếu"
                            error.message?.contains("invalid-email") == true -> 
                                "Email không hợp lệ"
                            else -> error.message ?: "Đăng ký thất bại"
                        }
                        _authState.value = UiState.Error(error)
                        _authError.value = errorMessage
                        Log.e(TAG, "Lỗi đăng ký", error)
                    }
                )
            } catch (e: Exception) {
                _authState.value = UiState.Error(e)
                _authError.value = "Lỗi: ${e.message}"
                Log.e(TAG, "Ngoại lệ đăng ký", e)
            }
        }
    }

    /**
     * Đăng xuất người dùng
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _isLoggedIn.value = false
                _authState.value = UiState.Idle
                _authError.value = null
                Log.d(TAG, "Đăng xuất thành công")
            } catch (e: Exception) {
                _authError.value = "Lỗi đăng xuất: ${e.message}"
                Log.e(TAG, "Lỗi đăng xuất", e)
            }
        }
    }

    /**
     * Xóa thông báo lỗi
     */
    fun clearError() {
        _authError.value = null
    }

    /**
     * Xóa trạng thái xác thực
     */
    fun clearAuthState() {
        _authState.value = UiState.Idle
    }

    /**
     * Xác thực định dạng email
     */
    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Xác thực định dạng số điện thoại (Việt Nam)
     */
    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^[0-9]{9,11}$"))
    }

    /**
     * Xác thực định dạng ngày sinh dd/MM/yyyy
     */
    private fun validateDateFormat(date: String): Boolean {
        return date.matches(Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[012])/\\d{4}$"))
    }
}
