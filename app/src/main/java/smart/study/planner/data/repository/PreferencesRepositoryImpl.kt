package smart.study.planner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import smart.study.planner.data.model.Language
import smart.study.planner.data.model.ThemeMode
import smart.study.planner.domain.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using DataStore
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile("settings") }
    )

    // Keys
    private companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val THEME_KEY = stringPreferencesKey("theme")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    }

    /**
     * Get current language
     */
    override fun getLanguage(): Flow<Language> {
        return dataStore.data
            .catch { exception ->
                // Handle exception and emit default value
                emit(emptyPreferences())
            }
            .map { preferences ->
                val languageValue = preferences[LANGUAGE_KEY] ?: Language.VI.toString()
                try {
                    Language.valueOf(languageValue)
                } catch (e: IllegalArgumentException) {
                    Language.VI
                }
            }
    }

    /**
     * Update language preference
     */
    override suspend fun updateLanguage(language: Language): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = language.toString()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current theme mode
     */
    override fun getTheme(): Flow<ThemeMode> {
        return dataStore.data
            .catch { exception ->
                // Handle exception and emit default value
                emit(emptyPreferences())
            }
            .map { preferences ->
                val themeValue = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.toString()
                try {
                    ThemeMode.valueOf(themeValue)
                } catch (e: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                }
            }
    }

    /**
     * Update theme preference
     */
    override suspend fun updateTheme(theme: ThemeMode): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[THEME_KEY] = theme.toString()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get notifications enabled status
     */
    override fun getNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                // Handle exception and emit default value
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
            }
    }

    /**
     * Update notifications enabled status
     */
    override suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get sound enabled status
     */
    override fun getSoundEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                // Handle exception and emit default value
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[SOUND_ENABLED_KEY] ?: true
            }
    }

    /**
     * Update sound enabled status
     */
    override suspend fun updateSoundEnabled(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[SOUND_ENABLED_KEY] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get vibration enabled status
     */
    override fun getVibrationEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                // Handle exception and emit default value
                emit(emptyPreferences())
            }
            .map { preferences ->
                preferences[VIBRATION_ENABLED_KEY] ?: true
            }
    }

    /**
     * Update vibration enabled status
     */
    override suspend fun updateVibrationEnabled(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[VIBRATION_ENABLED_KEY] = enabled
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
