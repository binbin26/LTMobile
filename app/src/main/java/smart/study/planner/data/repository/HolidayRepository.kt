package smart.study.planner.data.repository

import android.util.Log
import smart.study.planner.data.model.Holiday
import smart.study.planner.data.network.HolidayApiService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Holiday data
 * Handles fetching holidays from API and caching
 * 
 * Architecture Note:
 * - Exposes List<Holiday> to domain/UI layers (clean abstraction)
 * - Handles HolidayResponse -> List<Holiday> conversion internally
 * - Implements caching to reduce API calls
 * - Provides clear error handling with Result<T>
 */
@Singleton
class HolidayRepository @Inject constructor(
    private val apiService: HolidayApiService
) {
    companion object {
        private const val TAG = "HolidayRepository"
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }
    
    // Cache for holidays to avoid repeated API calls
    private var cachedHolidays: List<Holiday>? = null
    
    /**
     * Get all holidays from API
     * Uses caching to avoid repeated calls
     * Fetches from db.json file
     * 
     * @return Result<List<Holiday>> - Success with holidays list or Failure with error
     */
    suspend fun getHolidays(): Result<List<Holiday>> {
        return try {
            // Return cached data if available
            cachedHolidays?.let {
                Log.d(TAG, "Returning cached holidays: ${it.size} holidays")
                return Result.success(it)
            }
            
            // Fetch from API
            Log.d(TAG, "Fetching holidays from API...")
            // Use getDbJson() to parse complete structure and prevent EOFException
            val dbResponse = apiService.getDbJson()
            
            // Extract holidays from complete response wrapper
            val holidays = dbResponse.getHolidaysList()
            
            // Validate response
            if (holidays.isEmpty()) {
                Log.w(TAG, "API returned empty holidays list")
            } else {
                Log.d(TAG, "Successfully fetched ${holidays.size} holidays from API")
            }
            
            // Cache the result
            cachedHolidays = holidays
            
            Result.success(holidays)
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error: ${e.message}", e)
            Log.e(TAG, "Expected format: {\"holidays\": [{\"id\": 1, ...}, ...]}")
            Result.failure(
                Exception("Lỗi phân tích dữ liệu ngày lễ. Vui lòng thử lại.", e)
            )
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()} - ${e.message()}", e)
            Result.failure(
                Exception("Không thể tải dữ liệu ngày lễ (${e.code()}). Vui lòng kiểm tra kết nối mạng.", e)
            )
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network error: No internet connection", e)
            Result.failure(
                Exception("Không có kết nối mạng. Vui lòng kiểm tra kết nối internet.", e)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching holidays: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(
                Exception("Đã xảy ra lỗi khi tải ngày lễ: ${e.message ?: "Lỗi không xác định"}", e)
            )
        }
    }
    
    /**
     * Get holiday for a specific date
     */
    fun getHolidayByDate(date: LocalDate): Holiday? {
        val dateString = date.format(DATE_FORMATTER)
        return cachedHolidays?.firstOrNull { it.date == dateString }
    }
    
    /**
     * Clear cache (useful for refresh)
     */
    fun clearCache() {
        Log.d(TAG, "Clearing holiday cache")
        cachedHolidays = null
    }
    
    /**
     * Get holidays for a specific month
     */
    fun getHolidaysForMonth(year: Int, month: Int): List<Holiday> {
        return cachedHolidays?.filter { holiday ->
            try {
                val holidayDate = LocalDate.parse(holiday.date, DATE_FORMATTER)
                holidayDate.year == year && holidayDate.monthValue == month
            } catch (e: Exception) {
                false
            }
        } ?: emptyList()
    }
}
