package smart.study.planner.data.network

import retrofit2.http.GET

/**
 * Retrofit API service for db.json data
 * Fetches from db.json file on GitHub
 * 
 * API Response Format (db.json):
 * {
 *   "holidays": [
 *     {
 *       "id": 1,
 *       "name": "Tết Dương lịch",
 *       "date": "2026-01-01",
 *       "type": "official",
 *       "isOffDay": true,
 *       "description": "Ngày đầu năm mới theo lịch Dương"
 *     }
 *   ],
 *   "motivations": [
 *     {
 *       "id": 1,
 *       "content": "Success is the sum of small efforts...",
 *       "author": "Robert Collier"
 *     }
 *   ]
 * }
 * 
 * Root Cause Fix:
 * - EOFException was caused by parsing db.json as HolidayResponse
 * - db.json contains multiple arrays (holidays + motivations)
 * - Solution: Parse complete structure as DbJsonResponse
 * - Repositories extract only what they need
 * 
 * Architecture Note:
 * - Returns DbJsonResponse wrapper matching complete API structure
 * - Repository layer extracts List<Holiday> or List<Motivation> as needed
 * - Keeps network layer decoupled from domain models
 * - Prevents EOFException by parsing complete JSON structure
 */
interface HolidayApiService {
    /**
     * Get complete db.json data
     * Returns DbJsonResponse wrapper matching complete API structure
     * This prevents EOFException by parsing the entire JSON correctly
     * 
     * Root Cause Fix:
     * - EOFException was caused by parsing db.json as HolidayResponse
     * - db.json contains multiple arrays (holidays + motivations)
     * - Solution: Parse complete structure as DbJsonResponse
     * - Repositories extract only what they need
     */
    @GET("db.json")
    suspend fun getDbJson(): DbJsonResponse
}
