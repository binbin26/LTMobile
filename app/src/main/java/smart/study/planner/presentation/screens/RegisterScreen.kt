package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.AuthViewModel

private const val TAG = "RegisterScreen"

/**
 * Màn hình đăng ký tài khoản với hình thức từng bước
 * Step 1: Thông tin cơ bản (Tên, Email, Mật khẩu)
 * Step 2: Thông tin học tập (Trường, Mã sinh viên, Ngành học, Năm học)
 * Step 3: Thông tin cá nhân (Số điện thoại, Ngày sinh, Giới tính)
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val authError by authViewModel.authError.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    // Form states
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var studentId by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3
    val genderOptions = listOf("Nam", "Nữ", "Khác")
    val yearOptions = listOf("1", "2", "3", "4")

    // Chuyển hướng khi đăng ký thành công
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            Log.d(TAG, "Đăng ký thành công, chuyển sang HomeScreen")
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 1) currentStep--
                        else navController.popBackStack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Quay lại"
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "Bước $currentStep/$totalSteps",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Đăng ký tài khoản",
                        fontSize = 24.sp,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            // Progress indicator
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp)
                    ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Step title
            Text(
                text = when (currentStep) {
                    1 -> "Thông tin cơ bản"
                    2 -> "Thông tin học tập"
                    3 -> "Thông tin cá nhân"
                    else -> ""
                },
                fontSize = 20.sp,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            // Form content with animation
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (step) {
                        1 -> RegisterStepOne(
                            name = name,
                            onNameChange = { name = it },
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            showPassword = showPassword,
                            onShowPasswordChange = { showPassword = it },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = { confirmPassword = it },
                            showConfirmPassword = showConfirmPassword,
                            onShowConfirmPasswordChange = { showConfirmPassword = it },
                            authError = authError
                        )

                        2 -> RegisterStepTwo(
                            school = school,
                            onSchoolChange = { school = it },
                            studentId = studentId,
                            onStudentIdChange = { studentId = it },
                            major = major,
                            onMajorChange = { major = it },
                            yearOfStudy = yearOfStudy,
                            onYearOfStudyChange = { yearOfStudy = it },
                            yearDropdownExpanded = yearDropdownExpanded,
                            onYearDropdownChange = { yearDropdownExpanded = it },
                            yearOptions = yearOptions
                        )

                        3 -> RegisterStepThree(
                            phoneNumber = phoneNumber,
                            onPhoneNumberChange = { phoneNumber = it },
                            dateOfBirth = dateOfBirth,
                            onDateOfBirthChange = { dateOfBirth = it },
                            gender = gender,
                            onGenderChange = { gender = it },
                            genderDropdownExpanded = genderDropdownExpanded,
                            onGenderDropdownChange = { genderDropdownExpanded = it },
                            genderOptions = genderOptions,
                            bio = bio,
                            onBioChange = { bio = it },
                            authError = authError
                        )
                    }
                }
            }

            // Error message
            if (authError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Lỗi",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = authError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous button
                if (currentStep > 1) {
                    TextButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quay lại")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Next/Submit button
                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            Log.d(TAG, "Registration data: name=$name, email=$email, password.length=${password.length}")
                            Log.d(TAG, "Academic info: school=$school, studentId=$studentId, major=$major, year=$yearOfStudy")
                            Log.d(TAG, "Personal info: phone=$phoneNumber, dob=$dateOfBirth, gender=$gender")
                            authViewModel.register(
                                email = email,
                                password = password,
                                name = name,
                                phoneNumber = phoneNumber.ifBlank { null },
                                dateOfBirth = dateOfBirth.ifBlank { null },
                                gender = gender,
                                studentId = studentId.ifBlank { null },
                                school = school.ifBlank { null },
                                major = major.ifBlank { null },
                                yearOfStudy = if (yearOfStudy.isNotBlank()) yearOfStudy.toIntOrNull() else null,
                                bio = bio.ifBlank { null }
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = when (currentStep) {
                        1 -> authState !is UiState.Loading &&
                                name.isNotBlank() &&
                                email.isNotBlank() &&
                                password.isNotBlank() &&
                                confirmPassword.isNotBlank() &&
                                password == confirmPassword

                        2 -> authState !is UiState.Loading
                        3 -> authState !is UiState.Loading &&
                                password == confirmPassword
                        else -> false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (authState is UiState.Loading && currentStep == totalSteps) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Đang đăng ký...")
                    } else {
                        Text(
                            if (currentStep < totalSteps) "Tiếp theo" else "Hoàn thành",
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign in link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Đã có tài khoản? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Đăng nhập",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        Log.d(TAG, "Quay về LoginScreen")
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

// ============================================
// Composables for Step 1: Basic Information
// ============================================

@Composable
private fun RegisterStepOne(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: (Boolean) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showConfirmPassword: Boolean,
    onShowConfirmPasswordChange: (Boolean) -> Unit,
    authError: String?
) {
    // Tên
    FormTextField(
        value = name,
        onValueChange = onNameChange,
        label = "Họ và tên",
        icon = Icons.Filled.Person,
        helperText = "Nhập tên đầy đủ của bạn",
        keyboardType = KeyboardType.Text,
        isError = authError != null && authError.contains("Tên")
    )

    // Email
    FormTextField(
        value = email,
        onValueChange = onEmailChange,
        label = "Email",
        icon = Icons.Filled.Email,
        helperText = "Sử dụng email có thể truy cập được",
        keyboardType = KeyboardType.Email,
        isError = authError != null && authError.contains("Email")
    )

    // Password
    FormPasswordField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Mật khẩu",
        helperText = "Tối thiểu 6 ký tự",
        showPassword = showPassword,
        onShowPasswordChange = onShowPasswordChange,
        isError = authError != null && authError.contains("mật khẩu")
    )

    // Confirm Password
    FormPasswordField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "Xác nhận mật khẩu",
        helperText = "Nhập lại mật khẩu của bạn",
        showPassword = showConfirmPassword,
        onShowPasswordChange = onShowConfirmPasswordChange,
        isError = password != confirmPassword && confirmPassword.isNotEmpty()
    )

    // Password mismatch warning
    if (password != confirmPassword && confirmPassword.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "Lỗi",
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Mật khẩu không khớp",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ============================================
// Composables for Step 2: Academic Information
// ============================================

@Composable
private fun RegisterStepTwo(
    school: String,
    onSchoolChange: (String) -> Unit,
    studentId: String,
    onStudentIdChange: (String) -> Unit,
    major: String,
    onMajorChange: (String) -> Unit,
    yearOfStudy: String,
    onYearOfStudyChange: (String) -> Unit,
    yearDropdownExpanded: Boolean,
    onYearDropdownChange: (Boolean) -> Unit,
    yearOptions: List<String>
) {
    // Trường học
    FormTextField(
        value = school,
        onValueChange = onSchoolChange,
        label = "Trường học",
        icon = Icons.Filled.School,
        helperText = "Tên trường học của bạn (không bắt buộc)",
        keyboardType = KeyboardType.Text
    )

    // Mã sinh viên
    FormTextField(
        value = studentId,
        onValueChange = onStudentIdChange,
        label = "Mã sinh viên",
        icon = Icons.Filled.Person,
        helperText = "Mã sinh viên (không bắt buộc)",
        keyboardType = KeyboardType.Text
    )

    // Ngành học
    FormTextField(
        value = major,
        onValueChange = onMajorChange,
        label = "Ngành học",
        icon = Icons.Filled.School,
        helperText = "VD: Công nghệ thông tin (không bắt buộc)",
        keyboardType = KeyboardType.Text
    )

    // Năm học
    FormDropdownField(
        value = yearOfStudy,
        label = "Năm học",
        helperText = "Chọn năm học (không bắt buộc)",
        options = yearOptions,
        expanded = yearDropdownExpanded,
        onExpandedChange = onYearDropdownChange,
        onValueChange = onYearOfStudyChange,
        icon = Icons.Filled.School
    )
}

// ============================================
// Composables for Step 3: Personal Information
// ============================================

@Composable
private fun RegisterStepThree(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    dateOfBirth: String,
    onDateOfBirthChange: (String) -> Unit,
    gender: String?,
    onGenderChange: (String?) -> Unit,
    genderDropdownExpanded: Boolean,
    onGenderDropdownChange: (Boolean) -> Unit,
    genderOptions: List<String>,
    bio: String,
    onBioChange: (String) -> Unit,
    authError: String?
) {
    // Số điện thoại
    FormTextField(
        value = phoneNumber,
        onValueChange = onPhoneNumberChange,
        label = "Số điện thoại",
        icon = Icons.Filled.Phone,
        helperText = "Số điện thoại (không bắt buộc)",
        keyboardType = KeyboardType.Phone,
        isError = authError != null && authError.contains("Số điện thoại")
    )

    // Ngày sinh
    FormTextField(
        value = dateOfBirth,
        onValueChange = onDateOfBirthChange,
        label = "Ngày sinh",
        icon = Icons.Filled.Person,
        helperText = "Định dạng: dd/MM/yyyy (không bắt buộc)",
        keyboardType = KeyboardType.Text,
        isError = authError != null && authError.contains("Ngày sinh")
    )

    // Giới tính
    FormDropdownField(
        value = gender ?: "",
        label = "Giới tính",
        helperText = "Chọn giới tính (không bắt buộc)",
        options = genderOptions,
        expanded = genderDropdownExpanded,
        onExpandedChange = onGenderDropdownChange,
        onValueChange = { onGenderChange(if (it.isEmpty()) null else it) },
        icon = Icons.Filled.Person
    )

    // Bio
    FormTextField(
        value = bio,
        onValueChange = onBioChange,
        label = "Giới thiệu bản thân",
        icon = Icons.Filled.Person,
        helperText = "Giới thiệu ngắn gọn về bản thân (không bắt buộc)",
        keyboardType = KeyboardType.Text,
        maxLines = 4
    )
}

// ============================================
// Reusable Form Components
// ============================================

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    helperText: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    maxLines: Int = 1
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = maxLines == 1,
            maxLines = maxLines,
            isError = isError
        )
        if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                fontSize = 12.sp,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun FormPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helperText: String = "",
    showPassword: Boolean,
    onShowPasswordChange: (Boolean) -> Unit,
    isError: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onShowPasswordChange(!showPassword) }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (showPassword) "Ẩn mật khẩu" else "Hiện mật khẩu"
                    )
                }
            },
            isError = isError
        )
        if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                fontSize = 12.sp,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun FormDropdownField(
    value: String,
    label: String,
    helperText: String = "",
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    icon: ImageVector
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                readOnly = true,
                singleLine = true
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
        if (helperText.isNotEmpty()) {
            Text(
                text = helperText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
