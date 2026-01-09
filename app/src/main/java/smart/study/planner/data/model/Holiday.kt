package smart.study.planner.data.model

/**
 * Data class for Vietnamese Holidays
 * Represents a holiday from the API
 */
data class Holiday(
    val id: Int,
    val name: String,
    val date: String, // Format: "YYYY-MM-DD"
    val type: String, // "official", "traditional", etc.
    val isOffDay: Boolean,
    val description: String,
    val lunarDate: String? = null // Optional lunar calendar date
)
