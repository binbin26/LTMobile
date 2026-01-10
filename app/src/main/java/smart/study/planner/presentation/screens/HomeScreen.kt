package smart.study.planner.presentation.screens

import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import smart.study.planner.data.model.Event
import smart.study.planner.presentation.components.BottomNavigationBar
import smart.study.planner.presentation.components.DeleteConfirmationDialog
import smart.study.planner.presentation.components.EventCard
import smart.study.planner.presentation.components.TaskDetailDialog
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.EventViewModel
import smart.study.planner.presentation.viewmodel.ProfileViewModel
import smart.study.planner.presentation.viewmodel.SubjectViewModel

private const val TAG = "HomeScreen"

/**
 * Home screen displaying dashboard with greeting, stats, and upcoming events
 * Sử dụng collectAsStateWithLifecycle để tối ưu performance
 * Hiển thị tên người dùng thật từ Firebase
 * Hiển thị số lượng môn học thực tế từ SubjectViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: EventViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
    subjectViewModel: SubjectViewModel = hiltViewModel()
) {
    Log.d(TAG, "HomeScreen recomposed")

    // Collect user data from ProfileViewModel
    val currentUser by profileViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoadingUser by profileViewModel.isLoading.collectAsStateWithLifecycle()

    // OPTIMIZATION: Sử dụng collectAsStateWithLifecycle thay vì collectAsState
    // Điều này giúp tự động cancel flow khi screen không visible
    val upcomingEventsState by viewModel.upcomingEventsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    // Collect subjects data from SubjectViewModel
    val subjectsState by subjectViewModel.subjectsState.collectAsStateWithLifecycle()

    // Collect random motivation (stable across recompositions)
    val randomMotivation by viewModel.randomMotivation.collectAsStateWithLifecycle()

    // Khi screen được hiển thị, trigger refresh data từ ViewModel
    LaunchedEffect(Unit) {
        Log.d(TAG, "HomeScreen launched, loading data")
        // Load user data from Firebase
        profileViewModel.loadCurrentUser()
        // Load events
        viewModel.forceRefreshEvents()
        // Load motivations for random quote
        viewModel.loadMotivations()
        // Load subjects from Firebase and local
        subjectViewModel.refreshFromFirebase()
    }

    // Log user data khi được load
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d(TAG, "User data loaded: ${currentUser!!.displayName} (${currentUser!!.email})")
        } else if (!isLoadingUser) {
            Log.d(TAG, "User data is null and not loading")
        }
    }

    // Log subjects data khi được load
    LaunchedEffect(subjectsState) {
        Log.d(TAG, "Subjects loaded: ${subjectsState.size} subjects")
    }

    // Filter state
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Not completed, 2: In progress

    // Dialog states for task management
    var showDeleteDialog: Event? by remember { mutableStateOf(null) }
    var showDetailDialog: Event? by remember { mutableStateOf(null) }

    // Calculate stats from events and subjects
    val stats = remember(eventsState, subjectsState) {
        when (val state = eventsState) {
            is UiState.Success -> {
                val events = state.data
                val urgentCount = events.count {
                    !it.isCompleted && it.startDateTime <= System.currentTimeMillis() + 24 * 60 * 60 * 1000
                }
                val completedCount = events.count { it.isCompleted }
                val totalCount = events.size
                val progress = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
                // Use actual subject count from SubjectViewModel
                val subjectCount = subjectsState.size

                Log.d(TAG, "Stats calculated: urgent=$urgentCount, completed=$completedCount, total=$totalCount, subjects=$subjectCount")
                Triple(urgentCount, progress, subjectCount)
            }
            else -> {
                // Even if events are not loaded, show subject count
                val subjectCount = subjectsState.size
                Triple(0, 0, subjectCount)
            }
        }
    }

    // Dynamic user name from Firebase
    val displayName = if (currentUser != null && currentUser!!.displayName.isNotBlank()) {
        Log.d(TAG, "Using Firebase user name: ${currentUser!!.displayName}")
        currentUser!!.displayName
    } else if (!isLoadingUser) {
        Log.d(TAG, "User data not available, using default")
        "Bạn"
    } else {
        Log.d(TAG, "User data is loading")
        "..."
    }

    // Greeting message
    val greetingMessage = "Chào $displayName! 👋"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Smart Study Planner",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Screen.Home.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.AddEvent.route,
                        Screen.Calendar.route,
                        Screen.Tasks.route,
                        Screen.Profile.route -> {
                            if (route == Screen.Profile.route) {
                                Log.d(TAG, "Profile nav click from BottomNavigationBar")
                            }
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                        else -> {}
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Section with Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column {
                    Text(
                        text = greetingMessage,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hôm nay bạn có ${stats.first} bài tập cần hoàn thành",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                }
            }

            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 🆕 SUBJECT MANAGEMENT BUTTON - Quick Access Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Log.d(TAG, "Navigating to Subject Management")
                            navController.navigate(Screen.SubjectManagement.route)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Icon with background
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Quản lý Môn học",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Bạn đang có ${stats.third} môn học",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quote Card with Elegant Design - Dynamic Motivation Quote
                val motivationQuote = remember(randomMotivation) {
                    randomMotivation?.content ?: "Success is the sum of small efforts repeated day in and day out."
                }
                val motivationAuthor = remember(randomMotivation) {
                    randomMotivation?.author
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "\"$motivationQuote\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 24.sp
                            )

                            if (motivationAuthor != null && motivationAuthor.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "— $motivationAuthor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dashboard Cards with Modern Design
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Urgent Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "⏰", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.first}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sắp đến hạn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Progress Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(text = "📊", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.second}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { stats.second / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tiến độ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Subjects Card - Clickable to navigate
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                Log.d(TAG, "Stats card clicked - Navigating to Subject Management")
                                navController.navigate(Screen.SubjectManagement.route)
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4ECDC4).copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📚", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.third}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ECDC4)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Môn học",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Filter Section with Modern Chips
                Column {
                    Text(
                        text = "Lọc theo trạng thái",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == 0,
                            onClick = { selectedFilter = 0 },
                            label = {
                                Text(
                                    "Tất cả",
                                    fontWeight = if (selectedFilter == 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(40.dp)
                        )

                        FilterChip(
                            selected = selectedFilter == 1,
                            onClick = { selectedFilter = 1 },
                            label = {
                                Text(
                                    "Chưa hoàn thành",
                                    fontWeight = if (selectedFilter == 1) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(40.dp)
                        )

                        FilterChip(
                            selected = selectedFilter == 2,
                            onClick = { selectedFilter = 2 },
                            label = {
                                Text(
                                    "Đang thực hiện",
                                    fontWeight = if (selectedFilter == 2) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Upcoming Deadlines Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deadline khẩn cấp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Events List
                when (val state = eventsState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Success -> {
                        val today = java.time.LocalDate.now()
                        val startOfToday = today.atStartOfDay()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

                        val filteredEvents = when (selectedFilter) {
                            1 -> state.data.filter {
                                !it.isCompleted && it.startDateTime >= startOfToday
                            }
                            2 -> {
                                val sevenDaysFromNow = startOfToday + (7 * 24 * 60 * 60 * 1000L)
                                state.data.filter {
                                    !it.isCompleted &&
                                            it.startDateTime >= startOfToday &&
                                            it.startDateTime <= sevenDaysFromNow
                                }
                            }
                            else -> state.data.filter {
                                !it.isCompleted && it.startDateTime >= startOfToday
                            }
                        }

                        if (filteredEvents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "📋", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = when (selectedFilter) {
                                            1 -> "Không có deadline chưa hoàn thành"
                                            2 -> "Không có deadline đang thực hiện"
                                            else -> "Không có deadline khẩn cấp"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredEvents.forEach { event ->
                                    EventCard(
                                        event = event,
                                        onClick = {
                                            Log.d(TAG, "Event clicked: ${event.id}")
                                            showDetailDialog = event
                                        },
                                        onEdit = {
                                            Log.d(TAG, "Edit event: ${event.id}")
                                            navController.navigate(Screen.AddEvent.createRoute(event.id))
                                        },
                                        onDelete = {
                                            Log.d(TAG, "Delete event: ${event.id}")
                                            showDeleteDialog = event
                                        },
                                        onViewDetail = {
                                            Log.d(TAG, "View detail: ${event.id}")
                                            showDetailDialog = event
                                        },
                                        onToggleComplete = {
                                            Log.d(TAG, "Toggle completion: ${event.id}")
                                            viewModel.toggleEventCompletion(event.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "❌", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Lỗi: ${state.error.message ?: "Lỗi không xác định"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is UiState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Đang tải dữ liệu...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { event ->
        DeleteConfirmationDialog(
            taskTitle = event.title,
            onConfirm = {
                Log.d(TAG, "Confirmed delete for event: ${event.id}")
                viewModel.deleteEvent(event.id)
                showDeleteDialog = null
            },
            onDismiss = {
                Log.d(TAG, "Dismissed delete dialog")
                showDeleteDialog = null
            }
        )
    }

    // Task detail dialog
    showDetailDialog?.let { event ->
        TaskDetailDialog(
            event = event,
            onDismiss = {
                Log.d(TAG, "Dismissed detail dialog")
                showDetailDialog = null
            },
            onEdit = {
                Log.d(TAG, "Edit from detail dialog: ${event.id}")
                showDetailDialog = null
                navController.navigate(Screen.AddEvent.createRoute(event.id))
            }
        )
    }
}