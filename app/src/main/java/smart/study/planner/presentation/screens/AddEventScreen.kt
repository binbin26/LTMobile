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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
    // ✅ Filter placeholder eventId and validate UUID format
    val actualEventId = remember(eventId) {
        if (eventId.isNullOrBlank() || eventId == "{eventId}") {
            null
        } else {
            // Validate UUID format to avoid malformed IDs from navigation
            val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            if (!uuidRegex.matches(eventId)) {
                Log.e(TAG, "Invalid eventId format received: $eventId")
                null
            } else {
                eventId
            }
        }
    }
    
    Log.d(TAG, "============================================")
    Log.d(TAG, "AddEventScreen opened")
    Log.d(TAG, "Received eventId parameter: $eventId")
    Log.d(TAG, "Actual eventId after filtering: $actualEventId")
    Log.d(TAG, "Mode: ${if (actualEventId == null) "CREATE NEW" else "EDIT EXISTING"}")
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
    var selectedHour by rememberSaveable { mutableStateOf(0) }
    var selectedMinute by rememberSaveable { mutableStateOf(0) }
    val datePickerBackground = MaterialTheme.colorScheme.surfaceVariant

    val saveState by viewModel.saveEventState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateEventState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()

    var showError by rememberSaveable { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(actualEventId != null) }
    
    // ✅ Lưu original event để preserve createdAt và các fields khác khi edit
    var originalEvent by remember { mutableStateOf<Event?>(null) }

    // Subjects will auto-load via ViewModel's init block
    // which calls refreshFromFirebase()

    LaunchedEffect(actualEventId) {
        if (actualEventId != null && actualEventId.isNotEmpty()) {
            Log.d(TAG, "Loading event for edit: $actualEventId")
            isEditMode = true
            viewModel.loadEvents()
        }
    }

    LaunchedEffect(eventsState, actualEventId, subjectsState) {
        if (actualEventId != null && actualEventId.isNotEmpty() && eventsState is UiState.Success) {
            val event = (eventsState as UiState.Success<List<Event>>).data.find { it.id == actualEventId }
            if (event != null) {
                Log.d(TAG, "Event found for editing: ${event.title}")
                originalEvent = event // ✅ Lưu original event
                title = event.title
                description = event.description
                selectedCategory = event.category
                selectedDate = event.startDateTime
                
                // ✅ THÊM: Load giờ và phút từ event
                val cal = java.util.Calendar.getInstance().apply {
                    timeInMillis = event.startDateTime
                }
                selectedHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                selectedMinute = cal.get(java.util.Calendar.MINUTE)
                
                event.subjectId?.let {
                    selectedSubjectId = it
                }
                event.subjectName?.let {
                    subjectInput = it
                }
            } else {
                Log.w(TAG, "Event not found: $actualEventId")
            }
        }
    }

    // Filter subjects based on input
    LaunchedEffect(subjectInput, subjectsState) {
        Log.d(TAG, "Filtering subjects. Input: '$subjectInput', Total subjects: ${subjectsState.size}")
        if (subjectInput.isBlank()) {
            filteredSubjects = subjectsState.take(10)
        } else {
            // searchSubjects is suspend function, need to call in coroutine
            filteredSubjects = subjectViewModel.searchSubjects(subjectInput)
        }
        Log.d(TAG, "Filtered subjects count: ${filteredSubjects.size}")
    }

    // Debug subjects state
    LaunchedEffect(subjectsState) {
        Log.d(TAG, "=== SUBJECTS STATE UPDATED ===")
        Log.d(TAG, "Total subjects: ${subjectsState.size}")
        subjectsState.forEach { subject ->
            Log.d(TAG, "Subject: ${subject.name} (ID: ${subject.id})")
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
                        text = if (actualEventId == null) "Tạo Task Mới" else "Chỉnh Sửa Task",
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
                        Screen.Tasks.route -> navController.navigate(route)
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

            // Subject Section - FIXED
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
                            modifier = Modifier.fillMaxWidth(),
                            value = subjectInput,
                            onValueChange = { newValue ->
                                subjectInput = newValue
                                selectedSubjectId = null
                                // Chỉ mở dropdown khi user đang gõ
                                if (newValue.isNotBlank()) {
                                    subjectDropdownExpanded = true
                                }
                            },
                            placeholder = { Text("Nhập tên môn học...") },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        subjectDropdownExpanded = !subjectDropdownExpanded
                                        Log.d(TAG, "Dropdown toggled: $subjectDropdownExpanded")
                                        Log.d(TAG, "Filtered subjects: ${filteredSubjects.size}")
                                        Log.d(TAG, "All subjects: ${subjectsState.size}")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        DropdownMenu(
                            expanded = subjectDropdownExpanded,
                            onDismissRequest = {
                                subjectDropdownExpanded = false
                                Log.d(TAG, "Dropdown dismissed")
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            // Show loading message if subjects not loaded yet
                            if (subjectsState.isEmpty() && subjectInput.isBlank()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.padding(4.dp))
                                            Text("Đang tải môn học...")
                                        }
                                    },
                                    onClick = { }
                                )
                            }

                            // Existing subjects
                            if (filteredSubjects.isNotEmpty()) {
                                filteredSubjects.forEach { subject ->
                                    DropdownMenuItem(
                                        text = { Text(subject.name) },
                                        onClick = {
                                            selectedSubjectId = subject.id
                                            subjectInput = subject.name
                                            subjectDropdownExpanded = false
                                            Log.d(TAG, "Subject selected: ${subject.name}")
                                        }
                                    )
                                }
                            } else if (subjectInput.isBlank() && subjectsState.isNotEmpty()) {
                                // Show all subjects when no filter
                                subjectsState.take(10).forEach { subject ->
                                    DropdownMenuItem(
                                        text = { Text(subject.name) },
                                        onClick = {
                                            selectedSubjectId = subject.id
                                            subjectInput = subject.name
                                            subjectDropdownExpanded = false
                                            Log.d(TAG, "Subject selected: ${subject.name}")
                                        }
                                    )
                                }
                            }

                            // Add new subject option - ONLY if user has typed something
                            if (subjectInput.isNotBlank() &&
                                filteredSubjects.none { it.name.equals(subjectInput, ignoreCase = true) }
                            ) {
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
                                            Log.d(TAG, "Adding new subject: $subjectInput")
                                            val result = subjectViewModel.addSubject(subjectInput)
                                            result.fold(
                                                onSuccess = { subject ->
                                                    Log.d(TAG, "Subject added successfully: ${subject.name}")
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

                            // Show "No results" message
                            if (subjectInput.isNotBlank() &&
                                filteredSubjects.isEmpty() &&
                                subjectsState.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Không tìm thấy môn học phù hợp",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = { }
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

            // Time Picker Section
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
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Thời Gian",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour input
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Giờ", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = String.format("%02d", selectedHour),
                                onValueChange = { input ->
                                    if (input.isBlank()) {
                                        selectedHour = 0
                                    } else {
                                        val hour = input.toIntOrNull() ?: selectedHour
                                        selectedHour = when {
                                            hour < 0 -> 0
                                            hour > 23 -> 23
                                            else -> hour
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(vertical = 8.dp))

                        // Minute input
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phút", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(
                                value = String.format("%02d", selectedMinute),
                                onValueChange = { input ->
                                    if (input.isBlank()) {
                                        selectedMinute = 0
                                    } else {
                                        val minute = input.toIntOrNull() ?: selectedMinute
                                        selectedMinute = when {
                                            minute < 0 -> 0
                                            minute > 59 -> 59
                                            else -> minute
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Display selected time
                    Text(
                        text = "Thời gian đã chọn: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
                            modifier = Modifier.size(24.dp),
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

                        val isCreatingNew = actualEventId == null || actualEventId.isEmpty()
                        val finalEventId = if (isCreatingNew) {
                            UUID.randomUUID().toString()
                        } else {
                            actualEventId
                        }

                        Log.d(TAG, "Is creating new: $isCreatingNew, Final Event ID: $finalEventId")

                        // ✅ Thêm giờ và phút vào timestamp
                        val finalTimestamp = java.util.Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
                            set(java.util.Calendar.MINUTE, selectedMinute)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        // ✅ Preserve original event fields when editing
                        val event = if (isCreatingNew) {
                            // Create new event
                            Event(
                                id = finalEventId,
                                userId = currentUserId,
                                title = title.trim(),
                                description = description.trim(),
                                startDateTime = finalTimestamp,
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
                        } else {
                            // Update existing event - preserve createdAt and other fields
                            val original = originalEvent
                            if (original != null) {
                                original.copy(
                                    id = finalEventId, // Keep original ID
                                    title = title.trim(),
                                    description = description.trim(),
                                    startDateTime = finalTimestamp,
                                    category = selectedCategory,
                                    updatedAt = System.currentTimeMillis(), // Update timestamp
                                    subjectId = selectedSubject?.id,
                                    subjectName = selectedSubject?.name,
                                    // Preserve: createdAt, userId, priority, isCompleted, etc.
                                )
                            } else {
                                // Fallback if original event not loaded
                                Log.w(TAG, "Original event not found, creating new event instead")
                                Event(
                                    id = finalEventId,
                                    userId = currentUserId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    startDateTime = finalTimestamp,
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
                            }
                        }

                        Log.d(TAG, """
                            Preparing to ${if (isCreatingNew) "CREATE" else "UPDATE"} event:
                            - ID: ${event.id}
                            - User ID: ${event.userId}
                            - Title: ${event.title}
                            - Start Date: ${event.startDateTime}
                            - Category: ${event.category}
                            - Subject: ${event.subjectName ?: "None"}
                        """.trimIndent())

                        if (isCreatingNew) {
                            Log.d(TAG, "Calling saveEvent for new event")
                            viewModel.saveEvent(event)
                        } else {
                            Log.d(TAG, "Calling updateEvent for existing event: $actualEventId")
                            viewModel.updateEvent(event)
                        }
                    },
                    enabled = title.isNotEmpty() &&
                            saveState !is UiState.Loading &&
                            updateState !is UiState.Loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (actualEventId == null) "💾 Lưu Task" else "✏️ Cập Nhật",
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
                            navController.navigate(Screen.Tasks.route)
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