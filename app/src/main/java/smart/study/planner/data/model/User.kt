package smart.study.planner.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * User model
 */
@IgnoreExtraProperties
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val phoneNumber: String? = null,
    val dateOfBirth: Long? = null,
    val gender: String? = null,
    val studentId: String? = null,
    val school: String? = null,
    val major: String? = null,
    val yearOfStudy: Int? = null,
    val bio: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor cho Firebase
    constructor() : this(
        id = "",
        email = "",
        displayName = ""
    )
}