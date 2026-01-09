package smart.study.planner.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Domain model for Subject (Môn học)
 * Represents a subject/course that can be associated with events
 */
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val colorHex: String = "#4285F4", // Default color for subject
    val createdAt: Long = System.currentTimeMillis()
)
