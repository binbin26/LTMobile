package smart.study.planner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Domain model for Event
 * Represents a study event/task in the application
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val description: String = "",
    val startDateTime: Long, // timestamp in milliseconds
    val endDateTime: Long? = null, // optional end timestamp
    val location: String = "",
    val category: EventCategory = EventCategory.STUDY,
    val priority: EventPriority = EventPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 15, // minutes before event
    val colorHex: String = "#4285F4", // màu hiển thị trên calendar
    val isSynced: Boolean = false, // for offline sync tracking
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Subject fields - tham chiếu đến môn học
    val subjectId: String? = null, // ID của Subject (nếu có)
    val subjectName: String? = null // Tên môn học (để hiển thị, không cần join)
) {
    // Backward compatibility
    @Deprecated("Use startDateTime instead", ReplaceWith("startDateTime"))
    val date: Long get() = startDateTime
    
    @Deprecated("Use endDateTime instead", ReplaceWith("endDateTime"))
    val endDate: Long? get() = endDateTime
}

/**
 * Event categories for classification
 */
enum class EventCategory {
    STUDY,      // Học tập
    ASSIGNMENT, // Bài tập
    EXAM,       // Kiểm tra
    SEMINAR,    // Seminar
    WORKSHOP,   // Workshop
    OTHER       // Khác
}

/**
 * Event priority levels
 */
enum class EventPriority {
    HIGH,    // Cao
    MEDIUM,  // Trung bình
    LOW      // Thấp
}

