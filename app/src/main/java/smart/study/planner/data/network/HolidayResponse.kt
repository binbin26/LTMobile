package smart.study.planner.data.network

import android.util.Log
import com.google.gson.annotations.SerializedName
import smart.study.planner.data.model.Holiday

/**
 * Response DTO for holiday API
 * Matches the API response structure: {"holidays": [...]}
 * 
 * Architecture Note:
 * - This wrapper preserves the API contract without flattening
 * - Repository layer extracts List<Holiday> for domain/UI consumption
 * - Keeps data layer clean separation from domain layer
 */
data class HolidayResponse(
    @SerializedName("holidays")
    val holidays: List<Holiday>
) {
    companion object {
        private const val TAG = "HolidayResponse"
    }
    
    /**
     * Get holidays list from response
     * Returns empty list if holidays is null (defensive programming)
     */
    fun getHolidaysList(): List<Holiday> {
        return holidays.ifEmpty {
            Log.w(TAG, "HolidayResponse contains empty holidays list")
            emptyList()
        }
    }
}
