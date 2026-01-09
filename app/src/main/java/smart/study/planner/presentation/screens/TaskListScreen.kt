package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import smart.study.planner.presentation.components.BottomNavigationBar
import smart.study.planner.presentation.components.DeleteConfirmationDialog
import smart.study.planner.presentation.components.EventCard
import smart.study.planner.presentation.components.TaskDetailDialog
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.EventViewModel

private const val TAG = "TaskListScreen"

/**
 * Task list screen displaying all events as tasks
 * Sử dụng collectAsStateWithLifecycle để tối ưu performance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    viewModel: EventViewModel = hiltViewModel()
) {
    Log.d(TAG, "TaskListScreen recomposed")
    
    // OPTIMIZATION: Sử dụng collectAsStateWithLifecycle thay vì collectAsState
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    
    // Dialog states for task management
    var showDeleteDialog: Event? by remember { mutableStateOf(null) }
    var showDetailDialog: Event? by remember { mutableStateOf(null) }
    
    // Khi screen được hiển thị, trigger refresh data
    LaunchedEffect(Unit) {
        Log.d(TAG, "TaskListScreen launched, loading data")
        viewModel.forceRefreshEvents()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Smart Study Planner", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Screen.TaskList.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.Home.route -> navController.navigate(route)
                        Screen.Calendar.route -> navController.navigate(route)
                        Screen.AddEvent.route -> {
                            Log.d(TAG, "Navigating to AddEventScreen for NEW event")
                            navController.navigate(route)
                        }
                        Screen.Profile.route -> navController.navigate(route)
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
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            when (val state = eventsState) {
                is UiState.Idle -> {
                    // Initial state, do nothing
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Đang tải...")
                    }
                }
                
                is UiState.Loading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is UiState.Success -> {
                    // Show task list
                    if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Không có nhiệm vụ nào",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Log.d(TAG, "Displaying ${state.data.size} tasks")
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.data) { event ->
                                EventCard(
                                    event = event,
                                    onClick = { 
                                        Log.d(TAG, "Event clicked: ${event.id}")
                                    },
                                    onEdit = {
                                        Log.d(TAG, "Edit event: ${event.id}")
                                        navController.navigate("${Screen.AddEvent.route}?eventId=${event.id}")
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
                    // Show error with retry option
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚠ Lỗi: ${state.error.message ?: "Đã xảy ra lỗi"}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = {
                                Log.d(TAG, "Retry loading tasks")
                                viewModel.loadEvents()
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { event ->
        smart.study.planner.presentation.components.DeleteConfirmationDialog(
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
        smart.study.planner.presentation.components.TaskDetailDialog(
            event = event,
            onDismiss = {
                Log.d(TAG, "Dismissed detail dialog")
                showDetailDialog = null
            },
            onEdit = {
                Log.d(TAG, "Edit from detail dialog: ${event.id}")
                showDetailDialog = null
                navController.navigate("${Screen.AddEvent.route}?eventId=${event.id}")
            }
        )
    }}
