package smart.study.planner.data.network

import android.util.Log
import com.google.gson.annotations.SerializedName
import smart.study.planner.data.model.Motivation

/**
 * Response DTO for motivations array
 * Can be used if API provides separate endpoint
 * Currently used for extracting motivations from DbJsonResponse
 */
data class MotivationResponse(
    @SerializedName("motivations")
    val motivations: List<Motivation>
) {
    companion object {
        private const val TAG = "MotivationResponse"
    }
    
    /**
     * Get motivations list from response
     * Returns empty list if motivations is null (defensive programming)
     */
    fun getMotivationsList(): List<Motivation> {
        return motivations.ifEmpty {
            Log.w(TAG, "MotivationResponse contains empty motivations list")
            emptyList()
        }
    }
}
