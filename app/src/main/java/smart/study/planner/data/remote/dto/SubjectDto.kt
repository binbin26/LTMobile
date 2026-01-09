package smart.study.planner.data.remote.dto

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import smart.study.planner.data.model.Subject

/**
 * Data Transfer Object for Firebase Realtime Database
 * Used for serialization/deserialization with Firebase
 */
@IgnoreExtraProperties
data class SubjectDto(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#4285F4",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to Map for Firebase, excluding null values
     */
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "colorHex" to colorHex,
            "createdAt" to createdAt
        )
    }
    
    /**
     * Convert DTO to domain model
     */
    fun toSubject(): Subject {
        return Subject(
            id = id,
            userId = userId,
            name = name,
            colorHex = colorHex,
            createdAt = createdAt
        )
    }
    
    companion object {
        /**
         * Convert domain model to DTO
         */
        fun fromSubject(subject: Subject): SubjectDto {
            return SubjectDto(
                id = subject.id,
                userId = subject.userId,
                name = subject.name,
                colorHex = subject.colorHex,
                createdAt = subject.createdAt
            )
        }
    }
}
