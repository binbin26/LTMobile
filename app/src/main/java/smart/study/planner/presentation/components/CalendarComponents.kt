package smart.study.planner.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.Holiday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Colors for calendar
private val HolidayColor = Color(0xFFFFEBEE) // Đỏ nhạt
private val TodayColor = Color(0xFF1976D2) // Xanh đậm
private val SelectedColor = Color(0xFFE3F2FD) // Xanh nhạt
private val WeekendColor = Color(0xFFF5F5F5) // Xám nhạt
private val HolidayTextColor = Color(0xFFD32F2F) // Đỏ đậm

/**
 * Month selector component with navigation arrows
 */
@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("vi", "VN"))
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Tháng trước"
            )
        }
        
        Text(
            text = currentMonth.format(monthFormatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Tháng sau"
            )
        }
    }
}

/**
 * Calendar grid component displaying days of the month
 */
@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    holidays: List<Holiday>,
    eventsMap: Map<LocalDate, List<Event>>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val firstDayOfMonth = currentMonth.atDay(1)
    val lastDayOfMonth = currentMonth.atEndOfMonth()
    
    // Get the first day of the week for the first day of month
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysInMonth = currentMonth.lengthOfMonth()
    
    // Calculate offset (0 = Monday, 6 = Sunday)
    val offset = (firstDayOfWeek.value + 6) % 7 // Convert to Monday = 0
    
    Column(modifier = modifier) {
        // Week header
        WeekHeader()
        
        // Calendar grid
        Column(
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Generate rows (weeks)
            var dayCounter = 1
            var weekCounter = 0
            
            while (weekCounter < 6 && dayCounter <= daysInMonth) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Generate 7 days per week
                    for (dayIndex in 0..6) {
                        if (weekCounter == 0 && dayIndex < offset) {
                            // Empty cell before first day of month
                            CalendarDayCell(
                                date = null,
                                isCurrentMonth = false,
                                isToday = false,
                                isSelected = false,
                                holiday = null,
                                taskCount = 0,
                                onClick = {},
                                modifier = Modifier.weight(1f)
                            )
                        } else if (dayCounter <= daysInMonth) {
                            val date = firstDayOfMonth.plusDays((dayCounter - 1).toLong())
                            val holiday = holidays.firstOrNull { 
                                LocalDate.parse(it.date) == date 
                            }
                            val events = eventsMap[date] ?: emptyList()
                            
                            CalendarDayCell(
                                date = date,
                                isCurrentMonth = true,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                holiday = holiday,
                                taskCount = events.size,
                                onClick = { onDateClick(date) },
                                modifier = Modifier.weight(1f)
                            )
                            dayCounter++
                        } else {
                            // Empty cell after last day of month
                            CalendarDayCell(
                                date = null,
                                isCurrentMonth = false,
                                isToday = false,
                                isSelected = false,
                                holiday = null,
                                taskCount = 0,
                                onClick = {},
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                weekCounter++
            }
        }
    }
}

/**
 * Week header (H, B, T, N, S, B, C)
 */
@Composable
fun WeekHeader(modifier: Modifier = Modifier) {
    val weekDays = listOf("H", "B", "T", "N", "S", "B", "C")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Individual day cell in the calendar
 */
@Composable
fun CalendarDayCell(
    date: LocalDate?,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    holiday: Holiday?,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        holiday != null -> HolidayColor
        isSelected -> SelectedColor
        isToday -> TodayColor.copy(alpha = 0.2f)
        !isCurrentMonth -> Color.Transparent
        date?.dayOfWeek == DayOfWeek.SATURDAY || date?.dayOfWeek == DayOfWeek.SUNDAY -> WeekendColor
        else -> Color.Transparent
    }
    
    val textColor = when {
        holiday != null -> HolidayTextColor
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary
        !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .then(
                if (date != null && isCurrentMonth) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (date != null) {
                // Day number
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
                
                // Holiday icon or task dots
                if (holiday != null) {
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (taskCount > 0) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        repeat(minOf(taskCount, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            if (it < minOf(taskCount, 3) - 1) {
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Selected date detail component
 * Shows holiday info and events for the selected date
 */
@Composable
fun SelectedDateDetail(
    date: LocalDate,
    holiday: Holiday?,
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("vi", "VN"))
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "📅",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Holiday info
            if (holiday != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HolidayColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "🎉 ${holiday.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HolidayTextColor
                        )
                        if (holiday.description.isNotEmpty()) {
                            Text(
                                text = holiday.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Events list
            if (events.isNotEmpty()) {
                Text(
                    text = "Sự kiện (${events.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            if (event.description.isNotEmpty()) {
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (holiday == null) {
                Text(
                    text = "Không có sự kiện nào",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
