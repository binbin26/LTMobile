package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import smart.study.planner.data.model.Subject
import smart.study.planner.presentation.viewmodel.SubjectViewModel

private const val TAG = "SubjectManagementScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectManagementScreen(
    navController: NavController,
    viewModel: SubjectViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjectsState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Subject?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quản lý Môn học",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Thêm môn học")
            }
        }
    ) { padding ->
        if (subjects.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Chưa có môn học nào",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        "Nhấn nút + để thêm môn học mới",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subjects) { subject ->
                    SubjectItemCard(
                        subject = subject,
                        onEdit = { selectedSubject = it },
                        onDelete = { showDeleteDialog = it }
                    )
                }
            }
        }
    }

    // Dialog thêm môn học mới
    if (showAddDialog) {
        SubjectEditDialog(
            subject = null,
            onDismiss = { showAddDialog = false },
            onSave = { subject ->
                coroutineScope.launch {
                    val result = viewModel.addSubject(subject.name)
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "✅ Subject added successfully")
                            showAddDialog = false
                        },
                        onFailure = { e ->
                            Log.e(TAG, "❌ Error adding subject: ${e.message}")
                        }
                    )
                }
            }
        )
    }

    // Dialog chỉnh sửa môn học
    if (selectedSubject != null) {
        SubjectEditDialog(
            subject = selectedSubject,
            onDismiss = { selectedSubject = null },
            onSave = { updatedSubject ->
                coroutineScope.launch {
                    val result = viewModel.updateSubject(updatedSubject)
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "✅ Subject updated successfully")
                            selectedSubject = null
                        },
                        onFailure = { e ->
                            Log.e(TAG, "❌ Error updating subject: ${e.message}")
                        }
                    )
                }
            }
        )
    }

    // Dialog xác nhận xóa
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Xác nhận xóa") },
            text = {
                Text("Bạn có chắc chắn muốn xóa môn học \"${showDeleteDialog?.name}\"? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            showDeleteDialog?.let { subject ->
                                val result = viewModel.deleteSubject(subject.id)
                                result.fold(
                                    onSuccess = {
                                        Log.d(TAG, "✅ Subject deleted successfully")
                                    },
                                    onFailure = { e ->
                                        Log.e(TAG, "❌ Error deleting subject: ${e.message}")
                                    }
                                )
                            }
                            showDeleteDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun SubjectItemCard(
    subject: Subject,
    onEdit: (Subject) -> Unit,
    onDelete: (Subject) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header với màu sắc và tên môn học
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Color indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(subject.colorHex)))
                    )

                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subject.semester.isNotEmpty()) {
                            Text(
                                text = subject.semester,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onEdit(subject) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onDelete(subject) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Details
            if (subject.teacherName.isNotEmpty() ||
                subject.schedule.isNotEmpty() ||
                subject.classroom.isNotEmpty() ||
                subject.credits > 0) {

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subject.teacherName.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Giảng viên",
                            value = subject.teacherName
                        )
                    }

                    if (subject.schedule.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Default.Schedule,
                            label = "Thời gian",
                            value = subject.schedule
                        )
                    }

                    if (subject.classroom.isNotEmpty()) {
                        InfoRow(
                            icon = Icons.Default.LocationOn,
                            label = "Phòng học",
                            value = subject.classroom
                        )
                    }

                    if (subject.credits > 0) {
                        InfoRow(
                            icon = Icons.Default.Star,
                            label = "Tín chỉ",
                            value = "${subject.credits} TC"
                        )
                    }
                }
            }

            // Description
            if (subject.description.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = subject.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectEditDialog(
    subject: Subject?,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var teacherName by remember { mutableStateOf(subject?.teacherName ?: "") }
    var schedule by remember { mutableStateOf(subject?.schedule ?: "") }
    var classroom by remember { mutableStateOf(subject?.classroom ?: "") }
    var credits by remember { mutableStateOf(subject?.credits?.toString() ?: "") }
    var semester by remember { mutableStateOf(subject?.semester ?: "") }
    var description by remember { mutableStateOf(subject?.description ?: "") }
    var selectedColor by remember { mutableStateOf(subject?.colorHex ?: "#4285F4") }

    val colors = remember {
        listOf(
            "#4285F4", "#34A853", "#FBBC05", "#EA4335", "#9C27B0",
            "#FF9800", "#00BCD4", "#E91E63", "#795548", "#607D8B"
        )
    }

    var showError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = if (subject == null) "Thêm môn học mới" else "Chỉnh sửa môn học",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tên môn học (required)
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            showError = ""
                        },
                        label = { Text("Tên môn học *") },
                        placeholder = { Text("VD: Toán cao cấp") },
                        leadingIcon = {
                            Icon(Icons.Default.School, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showError.isNotEmpty(),
                        supportingText = if (showError.isNotEmpty()) {
                            { Text(showError, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )

                    // Giảng viên
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("Giảng viên") },
                        placeholder = { Text("VD: TS. Nguyễn Văn A") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Thời gian học
                    OutlinedTextField(
                        value = schedule,
                        onValueChange = { schedule = it },
                        label = { Text("Thời gian học") },
                        placeholder = { Text("VD: Thứ 2, 7:30 - 9:30") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Phòng học
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("Phòng học") },
                        placeholder = { Text("VD: A101") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Số tín chỉ
                        OutlinedTextField(
                            value = credits,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    credits = it
                                }
                            },
                            label = { Text("Tín chỉ") },
                            placeholder = { Text("3") },
                            leadingIcon = {
                                Icon(Icons.Default.Star, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Học kỳ
                        OutlinedTextField(
                            value = semester,
                            onValueChange = { semester = it },
                            label = { Text("Học kỳ") },
                            placeholder = { Text("HK1 2024") },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Mô tả
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả") },
                        placeholder = { Text("Ghi chú về môn học...") },
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    // Color picker
                    Text(
                        text = "Chọn màu:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .then(
                                        if (color == selectedColor) {
                                            Modifier.padding(4.dp)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { selectedColor = color },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (color == selectedColor) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                showError = "Vui lòng nhập tên môn học"
                                return@Button
                            }

                            val newSubject = Subject(
                                id = subject?.id ?: java.util.UUID.randomUUID().toString(),
                                userId = subject?.userId ?: "",
                                name = name.trim(),
                                colorHex = selectedColor,
                                teacherName = teacherName.trim(),
                                schedule = schedule.trim(),
                                classroom = classroom.trim(),
                                credits = credits.toIntOrNull() ?: 0,
                                semester = semester.trim(),
                                description = description.trim(),
                                createdAt = subject?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            onSave(newSubject)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (subject == null) "Thêm" else "Lưu")
                    }
                }
            }
        }
    }
}