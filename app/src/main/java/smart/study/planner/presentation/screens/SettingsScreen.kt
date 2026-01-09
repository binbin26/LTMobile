package smart.study.planner.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import smart.study.planner.R
import smart.study.planner.data.model.Language
import smart.study.planner.data.model.ThemeMode
import smart.study.planner.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsType: String,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val language by viewModel.language.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Smart Study Planner",
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (settingsType) {
                "notifications" -> NotificationSettings(
                    notificationsEnabled = notificationsEnabled,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    onNotificationsChange = viewModel::toggleNotifications,
                    onSoundChange = viewModel::toggleSound,
                    onVibrationChange = viewModel::toggleVibration
                )

                "language" -> LanguageSettings(
                    currentLanguage = language,
                    onLanguageChange = viewModel::updateLanguage
                )

                "theme" -> ThemeSettings(
                    currentTheme = theme,
                    onThemeChange = viewModel::updateTheme
                )

                "privacy" -> PrivacySettings()

                "debug" -> DebugScreen()

                else -> Text(stringResource(R.string.settings_title))
            }
        }
    }
}

@Composable
private fun NotificationSettings(
    notificationsEnabled: Boolean,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Main notifications toggle
        SettingToggle(
            title = stringResource(R.string.settings_notifications_title),
            description = stringResource(R.string.notification_channel_reminders_desc),
            isEnabled = notificationsEnabled,
            onToggle = onNotificationsChange,
            modifier = Modifier.padding(16.dp)
        )

        Divider()

        // Sound toggle
        SettingToggle(
            title = stringResource(R.string.settings_sound),
            isEnabled = soundEnabled,
            onToggle = onSoundChange,
            enabled = notificationsEnabled,
            modifier = Modifier.padding(16.dp)
        )

        Divider()

        // Vibration toggle
        SettingToggle(
            title = stringResource(R.string.settings_vibration),
            isEnabled = vibrationEnabled,
            onToggle = onVibrationChange,
            enabled = notificationsEnabled,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LanguageSettings(
    currentLanguage: Language,
    onLanguageChange: (Language) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        SettingRadioOption(
            text = stringResource(R.string.settings_language_vi),
            isSelected = currentLanguage == Language.VI,
            onClick = { onLanguageChange(Language.VI) }
        )

        Divider()

        SettingRadioOption(
            text = stringResource(R.string.settings_language_en),
            isSelected = currentLanguage == Language.EN,
            onClick = { onLanguageChange(Language.EN) }
        )
    }
}

@Composable
private fun ThemeSettings(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        SettingRadioOption(
            text = stringResource(R.string.settings_theme_light),
            isSelected = currentTheme == ThemeMode.LIGHT,
            onClick = { onThemeChange(ThemeMode.LIGHT) }
        )

        Divider()

        SettingRadioOption(
            text = stringResource(R.string.settings_theme_dark),
            isSelected = currentTheme == ThemeMode.DARK,
            onClick = { onThemeChange(ThemeMode.DARK) }
        )

        Divider()

        SettingRadioOption(
            text = stringResource(R.string.settings_theme_system),
            isSelected = currentTheme == ThemeMode.SYSTEM,
            onClick = { onThemeChange(ThemeMode.SYSTEM) }
        )
    }
}

@Composable
private fun PrivacySettings() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { /* TODO: Navigate to privacy policy */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_privacy))
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String? = null,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!isEnabled) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingRadioOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}
