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
    val colorHex: String = "#4285F4",

    // 🆕 Thông tin mới theo yêu cầu
    val teacherName: String = "", // Tên giảng viên
    val schedule: String = "", // Thời gian học (VD: "Thứ 2, 7:30 - 9:30")
    val classroom: String = "", // Phòng học (VD: "A101")
    val credits: Int = 0, // Số tín chỉ
    val semester: String = "", // Học kỳ (VD: "HK1 2024")
    val description: String = "", // Mô tả môn học

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)