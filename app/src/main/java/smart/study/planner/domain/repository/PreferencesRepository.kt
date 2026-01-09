package smart.study.planner.domain.repository

import kotlinx.coroutines.flow.Flow
import smart.study.planner.data.model.Language
import smart.study.planner.data.model.ThemeMode

/**
 * Repository interface for App Preferences
 */
interface PreferencesRepository {
    /**
     * Get current language
     */
    fun getLanguage(): Flow<Language>
    
    /**
     * Update language preference
     */
    suspend fun updateLanguage(language: Language): Result<Unit>
    
    /**
     * Get current theme mode
     */
    fun getTheme(): Flow<ThemeMode>
    
    /**
     * Update theme preference
     */
    suspend fun updateTheme(theme: ThemeMode): Result<Unit>
    
    /**
     * Get notifications enabled status
     */
    fun getNotificationsEnabled(): Flow<Boolean>
    
    /**
     * Update notifications enabled status
     */
    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit>
    
    /**
     * Get sound enabled status
     */
    fun getSoundEnabled(): Flow<Boolean>
    
    /**
     * Update sound enabled status
     */
    suspend fun updateSoundEnabled(enabled: Boolean): Result<Unit>
    
    /**
     * Get vibration enabled status
     */
    fun getVibrationEnabled(): Flow<Boolean>
    
    /**
     * Update vibration enabled status
     */
    suspend fun updateVibrationEnabled(enabled: Boolean): Result<Unit>
}

