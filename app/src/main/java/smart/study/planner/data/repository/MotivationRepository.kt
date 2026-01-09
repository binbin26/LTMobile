package smart.study.planner.data.repository

import android.util.Log
import smart.study.planner.data.model.Motivation
import smart.study.planner.data.network.HolidayApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Motivation data
 * Handles fetching motivational quotes from API and caching
 * 
 * Architecture Note:
 * - Exposes List<Motivation> to domain/UI layers (clean abstraction)
 * - Handles DbJsonResponse -> List<Motivation> conversion internally
 * - Implements caching to reduce API calls
 * - Provides clear error handling with Result<T>
 * - Uses same API endpoint as holidays but extracts different data
 */
@Singleton
class MotivationRepository @Inject constructor(
    private val apiService: HolidayApiService
) {
    companion object {
        private const val TAG = "MotivationRepository"
    }
    
    // Cache for motivations to avoid repeated API calls
    private var cachedMotivations: List<Motivation>? = null
    
    /**
     * Get all motivations from API
     * Uses caching to avoid repeated calls
     * Fetches from db.json file (same endpoint as holidays)
     * 
     * @return Result<List<Motivation>> - Success with motivations list or Failure with error
     */
    suspend fun getMotivations(): Result<List<Motivation>> {
        return try {
            // Return cached data if available
            cachedMotivations?.let {
                Log.d(TAG, "Returning cached motivations: ${it.size} motivations")
                return Result.success(it)
            }
            
            // Fetch from API
            Log.d(TAG, "Fetching motivations from API...")
            // Use getDbJson() to parse complete structure and prevent EOFException
            val dbResponse = apiService.getDbJson()
            
            // Extract motivations from complete response wrapper
            val motivations = dbResponse.getMotivationsList()
            
            // Validate response
            if (motivations.isEmpty()) {
                Log.w(TAG, "API returned empty motivations list")
            } else {
                Log.d(TAG, "Successfully fetched ${motivations.size} motivations from API")
            }
            
            // Cache the result
            cachedMotivations = motivations
            
            Result.success(motivations)
        } catch (e: com.google.gson.JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error: ${e.message}", e)
            Log.e(TAG, "Expected format: {\"holidays\": [...], \"motivations\": [...]}")
            Result.failure(
                Exception("Lỗi phân tích dữ liệu động lực. Vui lòng thử lại.", e)
            )
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()} - ${e.message()}", e)
            Result.failure(
                Exception("Không thể tải dữ liệu động lực (${e.code()}). Vui lòng kiểm tra kết nối mạng.", e)
            )
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network error: No internet connection", e)
            Result.failure(
                Exception("Không có kết nối mạng. Vui lòng kiểm tra kết nối internet.", e)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching motivations: ${e.javaClass.simpleName} - ${e.message}", e)
            Result.failure(
                Exception("Đã xảy ra lỗi khi tải động lực: ${e.message ?: "Lỗi không xác định"}", e)
            )
        }
    }
    
    /**
     * Get a random motivation from cached data
     * Returns null if no motivations are available
     * 
     * @return Motivation? - Random motivation or null
     */
    fun getRandomMotivation(): Motivation? {
        val motivations = cachedMotivations
        return if (motivations.isNullOrEmpty()) {
            Log.w(TAG, "No cached motivations available for random selection")
            null
        } else {
            val randomMotivation = motivations.random()
            Log.d(TAG, "Selected random motivation: ID ${randomMotivation.id}")
            randomMotivation
        }
    }
    
    /**
     * Clear cache (useful for refresh)
     */
    fun clearCache() {
        Log.d(TAG, "Clearing motivation cache")
        cachedMotivations = null
    }
}
