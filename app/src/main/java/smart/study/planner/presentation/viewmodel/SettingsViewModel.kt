package smart.study.planner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import smart.study.planner.data.model.Language
import smart.study.planner.data.model.ThemeMode
import smart.study.planner.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * Handles user preferences: language, theme, notifications
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // Language preference
    private val _language = MutableStateFlow(Language.VI)
    val language: StateFlow<Language> = _language.asStateFlow()

    // Theme preference
    private val _theme = MutableStateFlow(ThemeMode.SYSTEM)
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    // Notifications enabled
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Sound enabled
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    // Vibration enabled
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPreferences()
    }

    /**
     * Load all preferences from repository
     */
    private fun loadPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                preferencesRepository.getLanguage().collect { language ->
                    _language.value = language
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }

            try {
                preferencesRepository.getTheme().collect { theme ->
                    _theme.value = theme
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }

            try {
                preferencesRepository.getNotificationsEnabled().collect { enabled ->
                    _notificationsEnabled.value = enabled
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }

            try {
                preferencesRepository.getSoundEnabled().collect { enabled ->
                    _soundEnabled.value = enabled
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }

            try {
                preferencesRepository.getVibrationEnabled().collect { enabled ->
                    _vibrationEnabled.value = enabled
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }

            _isLoading.value = false
        }
    }

    /**
     * Update language preference
     */
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            preferencesRepository.updateLanguage(language)
                .onSuccess {
                    _language.value = language
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Không thể lưu ngôn ngữ"
                }
        }
    }

    /**
     * Update theme preference
     */
    fun updateTheme(theme: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(theme)
                .onSuccess {
                    _theme.value = theme
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Không thể lưu chế độ nền"
                }
        }
    }

    /**
     * Toggle notifications
     */
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
                .onSuccess {
                    _notificationsEnabled.value = enabled
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Không thể lưu cài đặt thông báo"
                }
        }
    }

    /**
     * Toggle sound
     */
    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSoundEnabled(enabled)
                .onSuccess {
                    _soundEnabled.value = enabled
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Không thể lưu cài đặt âm thanh"
                }
        }
    }

    /**
     * Toggle vibration
     */
    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateVibrationEnabled(enabled)
                .onSuccess {
                    _vibrationEnabled.value = enabled
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Không thể lưu cài đặt rung"
                }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
