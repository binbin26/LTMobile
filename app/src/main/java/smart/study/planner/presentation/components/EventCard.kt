package smart.study.planner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventCategory
import smart.study.planner.data.model.EventPriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// OPTIMIZATION: Create only one instance of SimpleDateFormat and reuse it.
private val eventCardDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private val eventCardDateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val eventCardTimeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

/**
 * Event card component for displaying event information
 * Modern, elegant design with better visual hierarchy
 * CRITICAL: Does NOT cache event state - uses Flow values directly
 */
@Composable
fun EventCard(
    event: Event,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onViewDetail: (() -> Unit)? = null,
    onToggleComplete: (() -> Unit)? = null
) {
    val categoryColor = getCategoryColor(event.category)
    val priorityColor = getPriorityColor(event.priority)
    val isCompleted = event.isCompleted
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCompleted) 2.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Colored accent bar on the left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = if (isCompleted) {
                            categoryColor.copy(alpha = 0.4f)
                        } else {
                            categoryColor
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header row: Checkbox, Title, Action Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { 
                                onToggleComplete?.invoke()
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = event.title,
                                style = if (isCompleted) {
                                    MaterialTheme.typography.titleMedium.copy(
                                        textDecoration = TextDecoration.LineThrough,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Category and Priority badges
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = categoryColor.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Text(
                                        text = getCategoryName(event.category),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = categoryColor,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                
                                // Priority badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = priorityColor.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(priorityColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = getPriorityName(event.priority),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = priorityColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Action menu
                    if (onEdit != null || onDelete != null || onViewDetail != null) {
                        TaskActionMenu(
                            isCompleted = event.isCompleted,
                            onEdit = { onEdit?.invoke() },
                            onDelete = { onDelete?.invoke() },
                            onViewDetail = { onViewDetail?.invoke() },
                            onToggleComplete = { onToggleComplete?.invoke() }
                        )
                    }
                }
                
                // Description
                if (event.description.isNotEmpty()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                
                // Footer: Date and Location
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date/Time
                    val dateOnly = remember(event.startDateTime) {
                        eventCardDateOnlyFormat.format(Date(event.startDateTime))
                    }
                    val timeOnly = remember(event.startDateTime) {
                        eventCardTimeOnlyFormat.format(Date(event.startDateTime))
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Time",
                            modifier = Modifier.size(16.dp),
                            tint = if (isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = dateOnly,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isCompleted) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = timeOnly,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isCompleted) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            )
                        }
                    }
                    
                    // Location
                    if (event.location.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                modifier = Modifier.size(16.dp),
                                tint = if (isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isCompleted) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get color based on event category
 * OPTIMIZATION: This is now a standard, non-composable function.
 */
private fun getCategoryColor(category: EventCategory): Color {
    return when (category) {
        EventCategory.STUDY -> Color(0xFF4CAF50) // Green
        EventCategory.ASSIGNMENT -> Color(0xFF2196F3) // Blue
        EventCategory.EXAM -> Color(0xFFF44336) // Red
        EventCategory.SEMINAR -> Color(0xFFFF9800) // Orange
        EventCategory.WORKSHOP -> Color(0xFF9C27B0) // Purple
        EventCategory.OTHER -> Color(0xFF9E9E9E) // Gray
    }
}

/**
 * Get category name in Vietnamese
 */
private fun getCategoryName(category: EventCategory): String {
    return when (category) {
        EventCategory.STUDY -> "Học tập"
        EventCategory.ASSIGNMENT -> "Bài tập"
        EventCategory.EXAM -> "Kiểm tra"
        EventCategory.SEMINAR -> "Seminar"
        EventCategory.WORKSHOP -> "Workshop"
        EventCategory.OTHER -> "Khác"
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
 * Format timestamp to readable date string using the cached formatter.
 */
private fun formatDate(timestamp: Long): String {
    return eventCardDateFormat.format(Date(timestamp))
}
