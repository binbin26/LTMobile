package smart.study.planner.presentation.screens

import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventCategory
import smart.study.planner.data.model.Subject
import smart.study.planner.presentation.components.BottomNavigationBar
import smart.study.planner.presentation.navigation.Screen
import smart.study.planner.presentation.util.UiState
import smart.study.planner.presentation.viewmodel.EventViewModel
import smart.study.planner.presentation.viewmodel.SubjectViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "AddEventScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    navController: NavController,
    eventId: String? = null,
    viewModel: EventViewModel = hiltViewModel(),
    subjectViewModel: SubjectViewModel = hiltViewModel()
) {
    Log.d(TAG, "============================================")
    Log.d(TAG, "AddEventScreen opened")
    Log.d(TAG, "Received eventId parameter: $eventId")
    Log.d(TAG, "Is null: ${eventId == null}")
    Log.d(TAG, "Is empty: ${eventId?.isEmpty()}")
    Log.d(TAG, "Mode: ${if (eventId == null || eventId.isEmpty()) "CREATE NEW" else "EDIT EXISTING"}")
    Log.d(TAG, "============================================")

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(EventCategory.STUDY) }
    val categories = remember { EventCategory.values().toList() }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }

    var subjectInput by rememberSaveable { mutableStateOf("") }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var subjectDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val subjectsState by subjectViewModel.subjectsState.collectAsStateWithLifecycle()

    val selectedSubject = remember(selectedSubjectId, subjectsState) {
        subjectsState.find { it.id == selectedSubjectId }
    }

    var filteredSubjects by remember { mutableStateOf<List<Subject>>(emptyList()) }

    val currentUserId = remember {
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    val currentTime = System.currentTimeMillis()
    var selectedDate by rememberSaveable { mutableStateOf(currentTime) }
    val datePickerBackground = MaterialTheme.colorScheme.surfaceVariant

    val saveState by viewModel.saveEventState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateEventState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    var showError by rememberSaveable { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(eventId != null && eventId.isNotEmpty()) }

    LaunchedEffect(eventId) {
        if (eventId != null && eventId.isNotEmpty()) {
            Log.d(TAG, "Loading event for edit: $eventId")
            isEditMode = true
            viewModel.loadEvents()
        }
    }

    LaunchedEffect(eventsState, eventId, subjectsState) {
        if (eventId != null && eventId.isNotEmpty() && eventsState is UiState.Success) {
            val event = (eventsState as UiState.Success<List<Event>>).data.find { it.id == eventId }
            if (event != null) {
                Log.d(TAG, "Event found for editing: ${event.title}")
                title = event.title
                description = event.description
                selectedCategory = event.category
                selectedDate = event.startDateTime
                event.subjectId?.let {
                    selectedSubjectId = it
                }
                event.subjectName?.let {
                    subjectInput = it
                }
            } else {
                Log.w(TAG, "Event not found: $eventId")
            }
        }
    }

    LaunchedEffect(subjectInput, subjectsState) {
        if (subjectInput.isBlank()) {
            filteredSubjects = subjectsState.take(10)
        } else {
            filteredSubjects = subjectViewModel.searchSubjects(subjectInput)
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success<*>) {
            Log.d(TAG, "Save successful, navigating back")
            viewModel.clearSaveEventState()
            navController.popBackStack()
        }
    }

    LaunchedEffect(updateState) {
        if (updateState is UiState.Success<*>) {
            Log.d(TAG, "Update successful, navigating back")
            viewModel.clearUpdateEventState()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (eventId == null) "Tạo Task Mới" else "Chỉnh Sửa Task",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                currentRoute = Screen.AddEvent.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.Home.route -> navController.navigate(route)
                        Screen.Calendar.route -> navController.navigate(route)
                        Screen.TaskList.route -> navController.navigate(route)
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Title,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Tên Bài Tập",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (showError.isNotEmpty()) showError = ""
                        },
                        placeholder = { Text("Nhập tên bài tập...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showError.isNotEmpty() && title.isBlank(),
                        supportingText = if (showError.isNotEmpty() && title.isBlank()) {
                            { Text("Vui lòng nhập tên bài tập", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Subject Section - IMPROVED
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Môn Học",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { subjectDropdownExpanded = true },
                            value = subjectInput,
                            onValueChange = { newValue ->
                                subjectInput = newValue
                                subjectDropdownExpanded = true
                                selectedSubjectId = null
                            },
                            placeholder = { Text("Chọn hoặc thêm môn học...") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = true
                        )

                        DropdownMenu(
                            expanded = subjectDropdownExpanded && (filteredSubjects.isNotEmpty() || subjectInput.isNotBlank()),
                            onDismissRequest = { subjectDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            // Existing subjects
                            filteredSubjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name) },
                                    onClick = {
                                        selectedSubjectId = subject.id
                                        subjectInput = subject.name
                                        subjectDropdownExpanded = false
                                    }
                                )
                            }

                            // Add new subject option - ONLY if no exact match
                            if (subjectInput.isNotBlank() &&
                                filteredSubjects.none { it.name.equals(subjectInput, ignoreCase = true) }) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "Thêm môn mới: \"$subjectInput\"",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            val result = subjectViewModel.addSubject(subjectInput)
                                            result.fold(
                                                onSuccess = { subject ->
                                                    selectedSubjectId = subject.id
                                                    subjectInput = subject.name
                                                    subjectDropdownExpanded = false
                                                },
                                                onFailure = { e ->
                                                    Log.e(TAG, "Error adding subject: ${e.message}")
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Deadline Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Deadline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            factory = { context ->
                                DatePicker(context).apply {
                                    calendarViewShown = false
                                    setBackgroundColor(datePickerBackground.toArgb())
                                    setOnDateChangedListener { _, year, month, day ->
                                        val cal = java.util.Calendar.getInstance().apply {
                                            set(year, month, day, 0, 0, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        selectedDate = cal.timeInMillis
                                    }
                                }
                            },
                            update = { picker ->
                                val cal = java.util.Calendar.getInstance().apply {
                                    timeInMillis = selectedDate
                                }
                                if (picker.year != cal.get(java.util.Calendar.YEAR) ||
                                    picker.month != cal.get(java.util.Calendar.MONTH) ||
                                    picker.dayOfMonth != cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ) {
                                    picker.updateDate(
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH),
                                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Description Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Nội Dung",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Mô tả chi tiết về bài tập...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Loading indicator
            if (saveState is UiState.Loading || updateState is UiState.Loading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            "Đang lưu...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error message
            val currentError = when {
                showError.isNotEmpty() -> showError
                saveState is UiState.Error -> (saveState as UiState.Error).error.message ?: "Đã xảy ra lỗi khi lưu"
                updateState is UiState.Error -> (updateState as UiState.Error).error.message ?: "Đã xảy ra lỗi khi lưu"
                else -> null
            }

            if (currentError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⚠️ $currentError",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    onClick = {
                        Log.d(TAG, "Save button clicked")

                        if (title.isBlank()) {
                            showError = "Vui lòng nhập tên bài tập"
                            Log.w(TAG, "Validation failed: Empty title")
                            return@Button
                        }

                        if (selectedDate <= 0) {
                            showError = "Vui lòng chọn ngày"
                            Log.w(TAG, "Validation failed: Invalid date")
                            return@Button
                        }

                        if (currentUserId.isEmpty()) {
                            showError = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
                            Log.e(TAG, "Validation failed: No authenticated user")
                            return@Button
                        }

                        showError = ""

                        val isCreatingNew = eventId == null || eventId.isEmpty()
                        val finalEventId = if (isCreatingNew) {
                            UUID.randomUUID().toString()
                        } else {
                            eventId
                        }

                        Log.d(TAG, "Is creating new: $isCreatingNew, Final Event ID: $finalEventId")

                        val event = Event(
                            id = finalEventId,
                            userId = currentUserId,
                            title = title.trim(),
                            description = description.trim(),
                            startDateTime = selectedDate,
                            endDateTime = null,
                            location = "",
                            category = selectedCategory,
                            priority = smart.study.planner.data.model.EventPriority.MEDIUM,
                            isCompleted = false,
                            isAllDay = false,
                            reminderEnabled = false,
                            reminderMinutes = 15,
                            colorHex = "#4285F4",
                            isSynced = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            subjectId = selectedSubject?.id,
                            subjectName = selectedSubject?.name
                        )

                        Log.d(TAG, """
                            Preparing to ${if (isCreatingNew) "CREATE" else "UPDATE"} event:
                            - ID: ${event.id}
                            - User ID: ${event.userId}
                            - Title: ${event.title}
                            - Start Date: ${event.startDateTime}
                            - Category: ${event.category}
                        """.trimIndent())

                        if (isCreatingNew) {
                            Log.d(TAG, "Calling saveEvent for new event")
                            viewModel.saveEvent(event)
                        } else {
                            Log.d(TAG, "Calling updateEvent for existing event: $eventId")
                            viewModel.updateEvent(event)
                        }
                    },
                    enabled = title.isNotEmpty() &&
                            saveState !is UiState.Loading &&
                            updateState !is UiState.Loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (eventId == null) "💾 Lưu Task" else "✏️ Cập Nhật",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = { navController.popBackStack() },
                        enabled = saveState !is UiState.Loading && updateState !is UiState.Loading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Medium)
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = {
                            Log.d(TAG, "Navigate to task list")
                            navController.navigate(Screen.TaskList.route)
                        },
                        enabled = saveState !is UiState.Loading && updateState !is UiState.Loading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📋 Xem Task", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}