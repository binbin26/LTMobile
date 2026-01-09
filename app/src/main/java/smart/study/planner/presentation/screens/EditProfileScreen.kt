package smart.study.planner.presentation.screens

import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import smart.study.planner.data.model.User
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "EditProfileScreen"

/**
 * Edit Profile screen
 * Allows users to update their profile information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val currentUser by profileViewModel.currentUser.collectAsStateWithLifecycle()
    val profileUpdateState by profileViewModel.profileUpdateState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form states - pre-filled with current user data
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var phoneNumber by remember { mutableStateOf(currentUser?.phoneNumber ?: "") }
    var dateOfBirth by remember { mutableStateOf(currentUser?.dateOfBirth?.let { formatTimestampToDate(it) } ?: "") }
    var gender by remember { mutableStateOf(currentUser?.gender ?: "") }
    var studentId by remember { mutableStateOf(currentUser?.studentId ?: "") }
    var school by remember { mutableStateOf(currentUser?.school ?: "") }
    var major by remember { mutableStateOf(currentUser?.major ?: "") }
    var yearOfStudy by remember { mutableIntStateOf(currentUser?.yearOfStudy ?: 0) }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }

    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    val genderOptions = listOf("Nam", "Nữ", "Khác")
    val yearOptions = listOf(1, 2, 3, 4)

    // Update form when user data loads
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d(TAG, "Pre-filling form with user data: ${currentUser!!.displayName}")
            displayName = currentUser!!.displayName
            phoneNumber = currentUser!!.phoneNumber ?: ""
            dateOfBirth = currentUser!!.dateOfBirth?.let { formatTimestampToDate(it) } ?: ""
            gender = currentUser!!.gender ?: ""
            studentId = currentUser!!.studentId ?: ""
            school = currentUser!!.school ?: ""
            major = currentUser!!.major ?: ""
            yearOfStudy = currentUser!!.yearOfStudy ?: 0
            bio = currentUser!!.bio ?: ""
        }
    }

    // Show snackbar on success/error
    LaunchedEffect(profileUpdateState) {
        when (profileUpdateState) {
            is UiState.Success -> {
                Log.d(TAG, "Profile updated successfully")
                scope.launch {
                    snackbarHostState.showSnackbar("Thông tin hồ sơ đã được cập nhật")
                    navController.popBackStack()
                }
            }
            is UiState.Error -> {
                Log.e(TAG, "Error updating profile")
                scope.launch {
                    snackbarHostState.showSnackbar("Lỗi cập nhật hồ sơ")
                }
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Tên hiển thị") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Phone Number
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Số điện thoại (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            // Date of Birth
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Ngày sinh: dd/MM/yyyy (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Gender Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    label = { Text("Giới tính (không bắt buộc)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { genderDropdownExpanded = true },
                    readOnly = true,
                    singleLine = true
                )
                DropdownMenu(
                    expanded = genderDropdownExpanded,
                    onDismissRequest = { genderDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                genderDropdownExpanded = false
                                Log.d(TAG, "Selected gender: $option")
                            }
                        )
                    }
                }
            }

            // Student ID
            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("Mã sinh viên (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // School
            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                label = { Text("Trường học (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Major
            OutlinedTextField(
                value = major,
                onValueChange = { major = it },
                label = { Text("Ngành học (không bắt buộc)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Year of Study
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (yearOfStudy > 0) "Năm $yearOfStudy" else "Năm học (không bắt buộc)",
                    onValueChange = {},
                    label = { Text("Năm học (không bắt buộc)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { yearDropdownExpanded = true },
                    readOnly = true,
                    singleLine = true
                )
                DropdownMenu(
                    expanded = yearDropdownExpanded,
                    onDismissRequest = { yearDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    yearOptions.forEach { year ->
                        DropdownMenuItem(
                            text = { Text("Năm $year") },
                            onClick = {
                                yearOfStudy = year
                                yearDropdownExpanded = false
                                Log.d(TAG, "Selected year of study: $year")
                            }
                        )
                    }
                }
            }

            // Bio
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Giới thiệu bản thân (không bắt buộc)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                Button(
                    onClick = {
                        Log.d(TAG, "Cancel editing profile")
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text("Hủy")
                }

                // Save Button
                Button(
                    onClick = {
                        Log.d(TAG, "Saving profile changes")
                        if (displayName.isNotBlank()) {
                            // Convert date string to timestamp if provided
                            var dateOfBirthTimestamp: Long? = null
                            if (dateOfBirth.isNotBlank()) {
                                dateOfBirthTimestamp = dateOfBirth.let { convertDateToTimestamp(it) }
                            }

                            val updatedUser = User(
                                id = currentUser!!.id,
                                email = currentUser!!.email,
                                displayName = displayName,
                                avatarUrl = currentUser!!.avatarUrl,
                                createdAt = currentUser!!.createdAt,
                                phoneNumber = phoneNumber.ifBlank { null },
                                dateOfBirth = dateOfBirthTimestamp,
                                gender = gender.ifBlank { null },
                                studentId = studentId.ifBlank { null },
                                school = school.ifBlank { null },
                                major = major.ifBlank { null },
                                yearOfStudy = if (yearOfStudy > 0) yearOfStudy else null,
                                bio = bio.ifBlank { null },
                                updatedAt = System.currentTimeMillis()
                            )

                            Log.d(TAG, "Updating user: ${updatedUser.displayName}")
                            profileViewModel.updateUserProfile(updatedUser)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Tên không được để trống")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = profileUpdateState !is UiState.Loading
                ) {
                    if (profileUpdateState is UiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Đang lưu...")
                    } else {
                        Text("Lưu thay đổi")
                    }
                }
            }
        }
    }
}

/**
 * Format timestamp to date string (dd/MM/yyyy)
 */
private fun formatTimestampToDate(timestamp: Long): String {
    return try {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.format(calendar.time)
    } catch (e: Exception) {
        Log.e(TAG, "Error formatting date", e)
        ""
    }
}

/**
 * Convert date string (dd/MM/yyyy) to timestamp
 */
private fun convertDateToTimestamp(dateString: String): Long {
    return try {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        date?.time ?: 0L
    } catch (e: Exception) {
        Log.e(TAG, "Error converting date to timestamp", e)
        0L
    }
}

/**
 * Clickable modifier extension
 */
private fun Modifier.clickableExt(onClick: () -> Unit) =
    this.clickable(onClick = onClick)
