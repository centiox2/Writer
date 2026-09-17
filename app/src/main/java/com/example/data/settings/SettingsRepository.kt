package com.example.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.RecordingQuality
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "studio_settings")

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.DARK,
  val fontSizeSp: Float = 18f,
  val lineSpacingMultiplier: Float = 1.5f,
  val keepScreenAwakeDefault: Boolean = false,
  val autosaveEnabled: Boolean = true,
  val recordingQuality: RecordingQuality = RecordingQuality.HIGH,
  val communicationModeEnabled: Boolean = false,
  val defaultVolume: Float = 1.0f
)

/**
 * Persists user preferences with DataStore so they survive app restarts.
 */
class SettingsRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val FONT_SIZE = floatPreferencesKey("font_size_sp")
    val LINE_SPACING = floatPreferencesKey("line_spacing_multiplier")
    val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake_default")
    val AUTOSAVE_ENABLED = booleanPreferencesKey("autosave_enabled")
    val RECORDING_QUALITY = stringPreferencesKey("recording_quality")
    val COMMUNICATION_MODE = booleanPreferencesKey("communication_mode_enabled")
    val DEFAULT_VOLUME = floatPreferencesKey("default_volume")
  }

  val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
    AppSettings(
      themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
        ?: ThemeMode.DARK,
      fontSizeSp = prefs[Keys.FONT_SIZE] ?: 18f,
      lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: 1.5f,
      keepScreenAwakeDefault = prefs[Keys.KEEP_SCREEN_AWAKE] ?: false,
      autosaveEnabled = prefs[Keys.AUTOSAVE_ENABLED] ?: true,
      recordingQuality = prefs[Keys.RECORDING_QUALITY]?.let { runCatching { RecordingQuality.valueOf(it) }.getOrNull() }
        ?: RecordingQuality.HIGH,
      communicationModeEnabled = prefs[Keys.COMMUNICATION_MODE] ?: false,
      defaultVolume = prefs[Keys.DEFAULT_VOLUME] ?: 1.0f
    )
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setFontSizeSp(sizeSp: Float) {
    context.settingsDataStore.edit { it[Keys.FONT_SIZE] = sizeSp.coerceIn(12f, 32f) }
  }

  suspend fun setLineSpacingMultiplier(multiplier: Float) {
    context.settingsDataStore.edit { it[Keys.LINE_SPACING] = multiplier.coerceIn(1.1f, 2.5f) }
  }

  suspend fun setKeepScreenAwakeDefault(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_AWAKE] = enabled }
  }

  suspend fun setAutosaveEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.AUTOSAVE_ENABLED] = enabled }
  }

  suspend fun setRecordingQuality(quality: RecordingQuality) {
    context.settingsDataStore.edit { it[Keys.RECORDING_QUALITY] = quality.name }
  }

  suspend fun setCommunicationModeEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.COMMUNICATION_MODE] = enabled }
  }

  suspend fun setDefaultVolume(volume: Float) {
    context.settingsDataStore.edit { it[Keys.DEFAULT_VOLUME] = volume.coerceIn(0f, 1f) }
  }
}
