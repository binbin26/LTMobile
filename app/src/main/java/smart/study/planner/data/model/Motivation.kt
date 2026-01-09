package smart.study.planner.data.model

/**
 * Data class for Motivational Quotes
 * Represents a motivational quote from the API
 */
data class Motivation(
    val id: Int,
    val content: String,
    val author: String? = null
)
