package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import smart.study.planner.presentation.components.BottomNavigationBar
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.AuthViewModel
import smart.study.planner.presentation.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ProfileScreen"

/**
 * Màn hình Hồ sơ người dùng
 * Hiển thị thông tin người dùng từ Firebase Realtime Database
 * Tự động reload khi có thay đổi từ EditProfileScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Smart Study Planner", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Screen.Profile.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.Home.route,
                        Screen.Calendar.route,
                        Screen.AddEvent.route,
                        Screen.TaskList.route,
                        Screen.Profile.route -> navController.navigate(route)
                    }
                }
            )
        }
    ) { innerPadding ->
        ProfileContent(
            innerPadding,
            navController,
            authViewModel,
            profileViewModel
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ProfileContent(
    innerPadding: PaddingValues,
    navController: NavController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel
) {
    val currentUser by profileViewModel.currentUser.collectAsStateWithLifecycle()
    val userLoadingState by profileViewModel.userLoadingState.collectAsStateWithLifecycle()
    val isLoading by profileViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by profileViewModel.errorMessage.collectAsStateWithLifecycle()

    // Reload user data khi ProfileContent được recompose
    LaunchedEffect(Unit) {
        Log.d(TAG, "[$TAG] ProfileContent composed, reloading user data từ Firebase")
        profileViewModel.reloadUserProfile()
    }

    // Reload data khi navigation back stack thay đổi (khi quay lại từ EditProfileScreen)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        Log.d(TAG, "[$TAG] Back stack entry thay đổi, reloading user profile")
        if (navBackStackEntry?.destination?.route == Screen.Profile.route) {
            Log.d(TAG, "[$TAG] Quay lại ProfileScreen, reload data mới nhất")
            profileViewModel.reloadUserProfile()
        }
    }

    // Logging user data when loaded
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            Log.d(TAG, """
                =================================
                DISPLAYING USER DATA:
                =================================
                ID: ${user.id}
                Email: ${user.email}
                Display Name: ${user.displayName}
                Avatar URL: ${user.avatarUrl ?: "null"}
                Phone: ${user.phoneNumber ?: "null"}
                DOB: ${user.dateOfBirth ?: "null"}
                Gender: ${user.gender ?: "null"}
                Student ID: ${user.studentId ?: "null"}
                School: ${user.school ?: "null"}
                Major: ${user.major ?: "null"}
                Year: ${user.yearOfStudy ?: "null"}
                Bio: ${user.bio ?: "null"}
                Created: ${user.createdAt}
                Updated: ${user.updatedAt}
                =================================
            """.trimIndent())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Loading State
            if (isLoading && userLoadingState is UiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Đang tải thông tin người dùng...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Column
            }

            // Error State
            if (userLoadingState is UiState.Error || errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = "Lỗi",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage ?: "Không thể tải thông tin người dùng",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Button(
                            onClick = { profileViewModel.loadCurrentUser() },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
                return@Column
            }

            // Success State - Display User Info with Animation
            if (currentUser != null) {
                val user = currentUser!!

                AnimatedContent(
                    targetState = user,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "UserDataAnimation"
                ) { animatedUser ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. HEADER SECTION - Avatar
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!animatedUser.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = animatedUser.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = "Avatar mặc định",
                                    modifier = Modifier.size(112.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 1. HEADER SECTION - Display Name
                        Text(
                            text = animatedUser.displayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 1. HEADER SECTION - Email
                        Text(
                            text = animatedUser.email,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 1. HEADER SECTION - User ID (shortened)
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = formatUserIdShort(animatedUser.id),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "User ID",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. PERSONAL INFORMATION CARD
                        val hasPersonalInfo = listOf(
                            animatedUser.phoneNumber,
                            animatedUser.dateOfBirth?.toString(),
                            animatedUser.gender
                        ).any { !it.isNullOrBlank() }

                        if (hasPersonalInfo) {
                            InfoCard(
                                title = "Thông tin cá nhân",
                                icon = Icons.Filled.Person,
                                content = {
                                    animatedUser.phoneNumber?.let {
                                        UserInfoRowNew(
                                            icon = Icons.Filled.Phone,
                                            label = "Số điện thoại",
                                            value = formatPhoneNumber(it)
                                        )
                                    }
                                    animatedUser.dateOfBirth?.let {
                                        UserInfoRowNew(
                                            icon = Icons.Filled.Cake,
                                            label = "Ngày sinh",
                                            value = formatDate(it)
                                        )
                                    }
                                    animatedUser.gender?.let {
                                        if (it.isNotBlank()) {
                                            UserInfoRowNew(
                                                icon = Icons.Filled.Person,
                                                label = "Giới tính",
                                                value = it
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // 3. ACADEMIC INFORMATION CARD
                        val hasAcademicInfo = listOf(
                            animatedUser.school,
                            animatedUser.studentId,
                            animatedUser.major,
                            animatedUser.yearOfStudy?.toString()
                        ).any { !it.isNullOrBlank() }

                        if (hasAcademicInfo) {
                            InfoCard(
                                title = "Thông tin học tập",
                                icon = Icons.Filled.School,
                                content = {
                                    animatedUser.school?.let {
                                        if (it.isNotBlank()) {
                                            UserInfoRowNew(
                                                icon = Icons.Filled.School,
                                                label = "Trường học",
                                                value = it
                                            )
                                        }
                                    }
                                    animatedUser.studentId?.let {
                                        if (it.isNotBlank()) {
                                            UserInfoRowNew(
                                                icon = Icons.Filled.Badge,
                                                label = "Mã sinh viên",
                                                value = it
                                            )
                                        }
                                    }
                                    animatedUser.major?.let {
                                        if (it.isNotBlank()) {
                                            UserInfoRowNew(
                                                icon = Icons.Filled.MenuBook,
                                                label = "Ngành học",
                                                value = it
                                            )
                                        }
                                    }
                                    animatedUser.yearOfStudy?.let {
                                        if (it > 0) {
                                            UserInfoRowNew(
                                                icon = Icons.Filled.Grade,
                                                label = "Năm học",
                                                value = "Năm $it"
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        // 4. BIO CARD
                        if (!animatedUser.bio.isNullOrBlank()) {
                            InfoCard(
                                title = "Giới thiệu",
                                icon = Icons.Filled.Description,
                                content = {
                                    Text(
                                        text = animatedUser.bio ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            )
                        }

                        // 5. SYSTEM INFORMATION CARD
                        InfoCard(
                            title = "Thông tin hệ thống",
                            icon = Icons.Filled.Info,
                            content = {
                                UserInfoRowNew(
                                    icon = Icons.Filled.Fingerprint,
                                    label = "User ID",
                                    value = animatedUser.id
                                )
                                UserInfoRowNew(
                                    icon = Icons.Filled.CalendarToday,
                                    label = "Ngày tạo",
                                    value = formatDateTime(animatedUser.createdAt)
                                )
                                UserInfoRowNew(
                                    icon = Icons.Filled.Update,
                                    label = "Cập nhật lần cuối",
                                    value = formatDateTime(animatedUser.updatedAt)
                                )
                                UserInfoRowNew(
                                    icon = Icons.Filled.Timeline,
                                    label = "Thời gian sử dụng",
                                    value = getAccountAge(animatedUser.createdAt)
                                )
                            }
                        )

                        // 6. EMPTY STATE MESSAGE
                        val isProfileIncomplete = listOf(
                            animatedUser.phoneNumber,
                            animatedUser.dateOfBirth?.toString(),
                            animatedUser.gender,
                            animatedUser.school,
                            animatedUser.studentId,
                            animatedUser.major,
                            animatedUser.yearOfStudy?.toString(),
                            animatedUser.bio
                        ).all { it.isNullOrBlank() }

                        if (isProfileIncomplete) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Hồ sơ của bạn chưa hoàn chỉnh",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Hãy cập nhật thêm thông tin để hoàn thành hồ sơ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 7. ACTION BUTTONS
                        Button(
                            onClick = {
                                Log.d(TAG, "Navigate to EditProfileScreen")
                                navController.navigate(Screen.EditProfile.route)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Chỉnh sửa",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Log.d(TAG, "Bắt đầu đăng xuất")
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "Đăng xuất",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Đăng xuất", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Loading overlay when updating profile
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .width(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Đang đồng bộ...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable component for displaying information cards
 */
@Composable
private fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card title with icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Content
            content()
        }
    }
}

/**
 * Improved user info row with icon
 */
@Composable
private fun UserInfoRowNew(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Helper function to format timestamp to datetime string
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Helper function to format timestamp to date string
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Format phone number to 0xxx xxx xxx format
 */
private fun formatPhoneNumber(phone: String): String {
    return if (phone.length == 10) {
        "${phone.substring(0, 4)} ${phone.substring(4, 7)} ${phone.substring(7)}"
    } else {
        phone
    }
}

/**
 * Format user ID to shortened format (first 4 and last 4 chars)
 */
private fun formatUserIdShort(id: String): String {
    return if (id.length > 8) {
        "${id.substring(0, 4)}...${id.substring(id.length - 4)}"
    } else {
        id
    }
}

/**
 * Calculate account age from creation timestamp
 */
private fun getAccountAge(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val diffMillis = now - createdAt
    val days = diffMillis / (1000 * 60 * 60 * 24)
    
    return when {
        days < 1 -> "Mới tạo hôm nay"
        days < 30 -> "$days ngày"
        days < 365 -> "${days / 30} tháng"
        else -> "${days / 365} năm"
    }
}