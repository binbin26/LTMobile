package smart.study.planner.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import smart.study.planner.R
import smart.study.planner.data.model.Event
import smart.study.planner.presentation.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    navController: NavController,
    eventId: String,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        // Load event details - this would require updating CalendarViewModel
        // For now, we'll use a placeholder
        isLoading = false
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.event_delete)) },
            text = { Text(stringResource(R.string.event_confirm_delete)) },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteEvent(eventId)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Smart Study Planner", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    Log.d("EventDetailScreen", "Navigating to AddEventScreen to EDIT event: $eventId")
                    navController.navigate("add_event?eventId=$eventId")
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = Color.White
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = Color.White
                    )
                }
            }
            ,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        )

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (event != null) {
            EventDetailContent(
                event = event!!,
                onToggleCompletion = { viewModel.toggleEventCompletion(eventId) },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun EventDetailContent(
    event: Event,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Completion status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = event.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(
                    text = stringResource(R.string.event_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        if (event.description.isNotEmpty()) {
            Text(
                text = stringResource(R.string.event_description),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Date & Time
        EventDetailRow(
            label = stringResource(R.string.event_date),
            value = formatDateTime(event.startDateTime)
        )

        EventDetailRow(
            label = stringResource(R.string.event_start_time),
            value = formatTime(event.startDateTime)
        )

        event.endDateTime?.let {
            EventDetailRow(
                label = stringResource(R.string.event_end_time),
                value = formatTime(it)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location
        if (event.location.isNotEmpty()) {
            EventDetailRow(
                label = stringResource(R.string.event_location),
                value = event.location,
                icon = Icons.Default.LocationOn
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Subject (Môn học) - hiển thị subjectName nếu có
        if (event.subjectName != null && event.subjectName.isNotBlank()) {
            EventDetailRow(
                label = "Môn học",
                value = event.subjectName
            )
        }
        
        // Category (chỉ hiển thị nếu không có subject)
        if (event.subjectName == null || event.subjectName.isBlank()) {
            EventDetailRow(
                label = stringResource(R.string.event_category),
                value = when (event.category.name) {
                    "STUDY" -> stringResource(R.string.category_study)
                    "ASSIGNMENT" -> stringResource(R.string.category_assignment)
                    "EXAM" -> stringResource(R.string.category_exam)
                    "SEMINAR" -> stringResource(R.string.category_seminar)
                    "WORKSHOP" -> stringResource(R.string.category_workshop)
                    else -> stringResource(R.string.category_other)
                }
            )
        }

        // Priority
        EventDetailRow(
            label = stringResource(R.string.event_priority),
            value = when (event.priority.name) {
                "HIGH" -> stringResource(R.string.priority_high)
                "MEDIUM" -> stringResource(R.string.priority_medium)
                "LOW" -> stringResource(R.string.priority_low)
                else -> stringResource(R.string.priority_medium)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reminder info
        if (event.reminderEnabled) {
            EventDetailRow(
                label = stringResource(R.string.event_reminder),
                value = "${event.reminderMinutes} " + stringResource(R.string.reminder_5_minutes)
            )
        }
    }
}

@Composable
private fun EventDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDateTime(timeMillis: Long): String {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timeMillis),
        ZoneId.systemDefault()
    )
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return dateTime.format(formatter)
}

private fun formatTime(timeMillis: Long): String {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timeMillis),
        ZoneId.systemDefault()
    )
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return dateTime.format(formatter)
}
