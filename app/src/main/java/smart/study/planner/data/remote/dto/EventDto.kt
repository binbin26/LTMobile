package smart.study.planner.data.remote.dto

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import smart.study.planner.data.model.Event
import smart.study.planner.data.model.EventCategory
import smart.study.planner.data.model.EventPriority

/**
 * Data Transfer Object for Firebase Realtime Database
 * Used for serialization/deserialization with Firebase
 */
@IgnoreExtraProperties
data class EventDto(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val startDateTime: Long = 0L,
    val endDateTime: Long? = null,
    val location: String = "",
    val category: String = EventCategory.STUDY.name,
    val priority: String = "MEDIUM",
    val isCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 15,
    val colorHex: String = "#4285F4",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val subjectId: String? = null,
    val subjectName: String? = null
) {
    /**
     * Convert to Map for Firebase, excluding null values
     */
    @Exclude
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id
        map["userId"] = userId
        map["title"] = title
        map["description"] = description
        map["startDateTime"] = startDateTime
        
        // Only add endDateTime if it's not null
        endDateTime?.let { map["endDateTime"] = it }
        
        map["location"] = location
        map["category"] = category
        map["priority"] = priority
        map["isCompleted"] = isCompleted
        map["isAllDay"] = isAllDay
        map["reminderEnabled"] = reminderEnabled
        map["reminderMinutes"] = reminderMinutes
        map["colorHex"] = colorHex
        map["isSynced"] = isSynced
        map["createdAt"] = createdAt
        map["updatedAt"] = updatedAt
        
        // Only add subjectId and subjectName if they're not null
        subjectId?.let { map["subjectId"] = it }
        subjectName?.let { map["subjectName"] = it }
        
        return map
    }
    
    /**
     * Convert DTO to domain model
     */
    fun toEvent(): Event {
        return Event(
            id = id,
            userId = userId,
            title = title,
            description = description,
            startDateTime = startDateTime,
            endDateTime = endDateTime,
            location = location,
            category = try {
                EventCategory.valueOf(category)
            } catch (e: IllegalArgumentException) {
                EventCategory.OTHER
            },
            priority = try {
                EventPriority.valueOf(priority)
            } catch (e: IllegalArgumentException) {
                EventPriority.MEDIUM
            },
            isCompleted = isCompleted,
            isAllDay = isAllDay,
            reminderEnabled = reminderEnabled,
            reminderMinutes = reminderMinutes,
            colorHex = colorHex,
            isSynced = isSynced,
            createdAt = createdAt,
            updatedAt = updatedAt,
            subjectId = subjectId,
            subjectName = subjectName
        )
    }
    
    companion object {
        /**
         * Convert domain model to DTO
         */
        fun fromEvent(event: Event): EventDto {
            return EventDto(
                id = event.id,
                userId = event.userId,
                title = event.title,
                description = event.description,
                startDateTime = event.startDateTime,
                endDateTime = event.endDateTime,
                location = event.location,
                category = event.category.name,
                priority = event.priority.name,
                isCompleted = event.isCompleted,
                isAllDay = event.isAllDay,
                reminderEnabled = event.reminderEnabled,
                reminderMinutes = event.reminderMinutes,
                colorHex = event.colorHex,
                isSynced = event.isSynced,
                createdAt = event.createdAt,
                updatedAt = event.updatedAt,
                subjectId = event.subjectId,
                subjectName = event.subjectName
            )
        }
    }
}

