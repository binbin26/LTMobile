package smart.study.planner.data.network

import android.util.Log
import com.google.gson.annotations.SerializedName
import smart.study.planner.data.model.Holiday
import smart.study.planner.data.model.Motivation

/**
 * Complete response DTO for db.json
 * Matches the full API response structure:
 * {
 *   "holidays": [...],
 *   "motivations": [...]
 * }
 * 
 * Root Cause Analysis:
 * - EOFException occurs when Gson tries to parse db.json as HolidayResponse
 * - db.json contains multiple arrays (holidays, motivations)
 * - Parsing only "holidays" while JSON has more fields causes parsing errors
 * 
 * Solution:
 * - Create wrapper that matches complete db.json structure
 * - Parse entire JSON correctly, then extract what we need
 * - Prevents EOFException by ensuring Gson parses complete structure
 * 
 * Architecture Note:
 * - This wrapper preserves the complete API contract
 * - Repositories extract only what they need (holidays or motivations)
 * - Keeps network layer decoupled from domain models
 */
data class DbJsonResponse(
    @SerializedName("holidays")
    val holidays: List<Holiday>? = null,
    
    @SerializedName("motivations")
    val motivations: List<Motivation>? = null
) {
    companion object {
        private const val TAG = "DbJsonResponse"
    }
    
    /**
     * Get holidays list from response
     * Returns empty list if holidays is null or empty
     */
    fun getHolidaysList(): List<Holiday> {
        return holidays?.ifEmpty {
            Log.w(TAG, "DbJsonResponse contains empty holidays list")
            emptyList()
        } ?: run {
            Log.w(TAG, "DbJsonResponse contains null holidays")
            emptyList()
        }
    }
    
    /**
     * Get motivations list from response
     * Returns empty list if motivations is null or empty
     */
    fun getMotivationsList(): List<Motivation> {
        return motivations?.ifEmpty {
            Log.w(TAG, "DbJsonResponse contains empty motivations list")
            emptyList()
        } ?: run {
            Log.w(TAG, "DbJsonResponse contains null motivations")
            emptyList()
        }
    }
}
