package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.TaskFilter
import smart.study.planner.presentation.components.BottomNavigationBar
import smart.study.planner.presentation.components.DeleteConfirmationDialog
import smart.study.planner.presentation.components.EventCard
import smart.study.planner.presentation.components.TaskDetailDialog
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.viewmodel.TaskViewModel

private const val TAG = "TaskListScreen"

/**
 * Task list screen displaying all events as tasks
 * Sử dụng TaskViewModel để quản lý tasks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: TaskViewModel = hiltViewModel()
) {
    Log.d(TAG, "TaskListScreen recomposed")

    // Collect states từ TaskViewModel
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    // Dialog states for task management
    var showDeleteDialog: Event? by remember { mutableStateOf(null) }
    var showDetailDialog: Event? by remember { mutableStateOf(null) }

    // Load tasks khi screen hiển thị
    LaunchedEffect(Unit) {
        Log.d(TAG, "TaskListScreen launched, loading tasks")
        viewModel.loadTasks()
    }

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
                currentRoute = Screen.Tasks.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.Home.route -> navController.navigate(route) {
                            launchSingleTop = true
                        }
                        Screen.Calendar.route -> navController.navigate(route) {
                            launchSingleTop = true
                        }
                        Screen.AddEvent.route -> {
                            Log.d(TAG, "Navigating to AddEventScreen for NEW event")
                            navController.navigate(Screen.AddEvent.createRoute())
                        }
                        Screen.Profile.route -> navController.navigate(route) {
                            launchSingleTop = true
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
                .padding(16.dp)
        ) {
            Text(
                text = "Danh sách Task",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == TaskFilter.ALL,
                    onClick = { viewModel.filterTasks(TaskFilter.ALL) },
                    label = {
                        Text(
                            "Tất cả",
                            fontWeight = if (selectedFilter == TaskFilter.ALL) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                FilterChip(
                    selected = selectedFilter == TaskFilter.DUE_TODAY,
                    onClick = { viewModel.filterTasks(TaskFilter.DUE_TODAY) },
                    label = {
                        Text(
                            "Hôm nay",
                            fontWeight = if (selectedFilter == TaskFilter.DUE_TODAY) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                FilterChip(
                    selected = selectedFilter == TaskFilter.DUE_LATER,
                    onClick = { viewModel.filterTasks(TaskFilter.DUE_LATER) },
                    label = {
                        Text(
                            "Sắp tới",
                            fontWeight = if (selectedFilter == TaskFilter.DUE_LATER) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            // Hiển thị tasks dựa trên states
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚠️ Lỗi: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                Log.d(TAG, "Retry loading tasks")
                                viewModel.clearError()
                                viewModel.loadTasks()
                            }
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                filteredTasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📋",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (selectedFilter) {
                                    TaskFilter.DUE_TODAY -> "Không có task hôm nay"
                                    TaskFilter.DUE_LATER -> "Không có task sắp tới"
                                    else -> "Không có nhiệm vụ nào"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    Log.d(TAG, "Displaying ${filteredTasks.size} tasks (filter: $selectedFilter)")
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = filteredTasks,
                            key = { event -> event.id }
                        ) { event ->
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
                                    viewModel.toggleTaskCompletion(event.id)
                                }
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
                viewModel.deleteTask(event.id)
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