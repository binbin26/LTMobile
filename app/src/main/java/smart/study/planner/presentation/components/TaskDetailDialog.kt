package smart.study.planner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventPriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

/**
 * Dialog hiển thị chi tiết đầy đủ của task/event
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    event: Event,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Description
                if (event.description.isNotEmpty()) {
                    Text(
                        text = "📝 Mô tả",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Start DateTime
                Text(
                    text = "📅 Thời gian bắt đầu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = dateFormat.format(Date(event.startDateTime)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // End DateTime
                if (event.endDateTime != null) {
                    Text(
                        text = "⏰ Thời gian kết thúc",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = dateFormat.format(Date(event.endDateTime!!)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Location
                if (event.location.isNotEmpty()) {
                    Text(
                        text = "📍 Địa điểm",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = event.location,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Category
                Text(
                    text = "🏷️ Loại",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = getCategoryName(event.category),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Priority with badge
                Text(
                    text = "⚡ Độ ưu tiên",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = getPriorityColor(event.priority)
                        )
                    ) {
                        Text(
                            text = getPriorityName(event.priority),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Completion Status
                Text(
                    text = "✔️ Trạng thái",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = if (event.isCompleted) "Đã hoàn thành" else "Chưa hoàn thành",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.isCompleted) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Reminder
                Text(
                    text = "🔔 Nhắc nhở",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = if (event.reminderEnabled) {
                        "Bật (${event.reminderMinutes} phút trước)"
                    } else {
                        "Tắt"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.reminderEnabled) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đóng")
                    }

                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sửa")
                    }
                }
            }
        }
    }
}

/**
 * Get category name in Vietnamese
 */
private fun getCategoryName(category: smart.study.planner.data.model.EventCategory): String {
    return when (category) {
        smart.study.planner.data.model.EventCategory.STUDY -> "Học tập"
        smart.study.planner.data.model.EventCategory.ASSIGNMENT -> "Bài tập"
        smart.study.planner.data.model.EventCategory.EXAM -> "Kiểm tra"
        smart.study.planner.data.model.EventCategory.SEMINAR -> "Seminar"
        smart.study.planner.data.model.EventCategory.WORKSHOP -> "Workshop"
        smart.study.planner.data.model.EventCategory.OTHER -> "Khác"
    }
}

/**
 * Get priority name in Vietnamese
 */
private fun getPriorityName(priority: EventPriority): String {
    return when (priority) {
        EventPriority.HIGH -> "Cao"
        EventPriority.MEDIUM -> "Trung bình"
        EventPriority.LOW -> "Thấp"
    }
}

/**
 * Get priority color
 */
private fun getPriorityColor(priority: EventPriority): Color {
    return when (priority) {
        EventPriority.HIGH -> Color(0xFFF44336) // Red
        EventPriority.MEDIUM -> Color(0xFFFF9800) // Orange
        EventPriority.LOW -> Color(0xFF4CAF50) // Green
    }
}
