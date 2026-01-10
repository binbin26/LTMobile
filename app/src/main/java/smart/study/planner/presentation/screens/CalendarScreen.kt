package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import smart.study.planner.data.model.Event
import smart.study.planner.presentation.components.BottomNavigationBar
import smart.study.planner.presentation.components.CalendarGrid
import smart.study.planner.presentation.components.MonthSelector
import smart.study.planner.presentation.components.SelectedDateDetail
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.EventViewModel
import java.time.LocalDate
import java.time.YearMonth

private const val TAG = "CalendarScreen"

/**
 * Calendar screen with grid layout displaying:
 * - Monthly calendar grid
 * - Vietnamese holidays from API
 * - User events/tasks
 * - Visual indicators for holidays and days with tasks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: EventViewModel = hiltViewModel()
) {
    Log.d(TAG, "CalendarScreen recomposed")
    
    // States
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Collect states from ViewModel
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val holidaysState by viewModel.holidaysState.collectAsStateWithLifecycle()
    
    // Load data when screen is displayed
    LaunchedEffect(Unit) {
        Log.d(TAG, "CalendarScreen launched, loading data")
        viewModel.loadEvents()
        viewModel.loadHolidays()
    }
    
    // Group events by date
    val eventsMap = remember(eventsState) {
        when (val state = eventsState) {
            is UiState.Success -> {
                state.data.groupBy { event ->
                    // Convert timestamp to LocalDate
                    val dateTime = java.time.Instant.ofEpochMilli(event.startDateTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    dateTime
                }
            }
            else -> emptyMap<LocalDate, List<Event>>()
        }
    }
    
    // Get holidays list
    val holidays = remember(holidaysState) {
        when (val state = holidaysState) {
            is UiState.Success -> state.data
            else -> emptyList()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Smart Study Planner",
                        color = Color.White
                    ) 
                },
                actions = {
                    // "Today" button
                    Button(
                        onClick = {
                            selectedDate = LocalDate.now()
                            currentMonth = YearMonth.now()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Hôm nay")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentRoute = Screen.Calendar.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.Home.route -> navController.navigate(route)
                        Screen.AddEvent.route -> {
                            Log.d(TAG, "Navigating to AddEventScreen for NEW event")
                            navController.navigate(route)
                        }
                        Screen.Tasks.route -> navController.navigate(route)
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
        ) {
            // Month selector
            MonthSelector(
                currentMonth = currentMonth,
                onPreviousMonth = { 
                    currentMonth = currentMonth.minusMonths(1)
                },
                onNextMonth = { 
                    currentMonth = currentMonth.plusMonths(1)
                }
            )
            
            // Calendar grid or loading/error state
            when (val state = holidaysState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠ Lỗi: ${state.error.message ?: "Đã xảy ra lỗi khi tải ngày lễ"}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.loadHolidays()
                                }
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
                
                else -> {
                    // Calendar grid
                    CalendarGrid(
                        currentMonth = currentMonth,
                        selectedDate = selectedDate,
                        holidays = holidays,
                        eventsMap = eventsMap,
                        onDateClick = { date ->
                            selectedDate = date
                            Log.d(TAG, "Date selected: $date")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detail section for selected date
            SelectedDateDetail(
                date = selectedDate,
                holiday = viewModel.getHolidayForDate(selectedDate),
                events = eventsMap[selectedDate] ?: emptyList()
            )
        }
    }
}
