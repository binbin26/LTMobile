package smart.study.planner.data.remote.dto

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import smart.study.planner.data.model.Subject

/**
 * Data Transfer Object for Firebase Realtime Database
 * Used for serialization/deserialization with Firebase
 *
 * Updated to include new subject management fields:
 * - teacherName: Instructor name
 * - schedule: Class schedule
 * - classroom: Room/location
 * - credits: Number of credits
 * - semester: Academic semester
 * - description: Additional notes
 * - updatedAt: Last update timestamp
 */
@IgnoreExtraProperties
data class SubjectDto(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#4285F4",

    // 🆕 New fields for enhanced subject management
    val teacherName: String = "",
    val schedule: String = "",
    val classroom: String = "",
    val credits: Int = 0,
    val semester: String = "",
    val description: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to Map for Firebase, excluding null values
     * All fields are included for complete sync
     */
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "colorHex" to colorHex,
            "teacherName" to teacherName,
            "schedule" to schedule,
            "classroom" to classroom,
            "credits" to credits,
            "semester" to semester,
            "description" to description,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
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
            teacherName = teacherName,
            schedule = schedule,
            classroom = classroom,
            credits = credits,
            semester = semester,
            description = description,
            createdAt = createdAt,
            updatedAt = updatedAt
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
                teacherName = subject.teacherName,
                schedule = subject.schedule,
                classroom = subject.classroom,
                credits = subject.credits,
                semester = subject.semester,
                description = subject.description,
                createdAt = subject.createdAt,
                updatedAt = subject.updatedAt
            )
        }
    }
}