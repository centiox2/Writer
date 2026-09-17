package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFileManager
import com.example.audio.RecordingQuality
import com.example.data.repositories.AlbumRepository
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.data.settings.AppSettings
import com.example.data.settings.SettingsRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageInfo(
  val songCount: Int = 0,
  val albumCount: Int = 0,
  val recordingCount: Int = 0,
  val audioStorageBytes: Long = 0L,
  val isLoading: Boolean = true
)

data class SettingsUiState(
  val appSettings: AppSettings = AppSettings(),
  val storageInfo: StorageInfo = StorageInfo()
)

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val songRepository: SongRepository,
  private val albumRepository: AlbumRepository,
  private val audioRepository: AudioRepository,
  private val audioFileManager: AudioFileManager
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      settingsRepository.settings.collect { settings ->
        _uiState.update { it.copy(appSettings = settings) }
      }
    }
    refreshStorageInfo()
  }

  fun refreshStorageInfo() {
    viewModelScope.launch {
      _uiState.update { it.copy(storageInfo = it.storageInfo.copy(isLoading = true)) }
      val songs = songRepository.getActiveSongsSync()
      val albums = albumRepository.getAllAlbums().first()
      val recordings = audioRepository.getAllRecordingsSync()
      val bytes = withContext(Dispatchers.IO) { audioFileManager.getAppStorageUsageBytes() }
      _uiState.update {
        it.copy(
          storageInfo = StorageInfo(
            songCount = songs.size,
            albumCount = albums.size,
            recordingCount = recordings.size,
            audioStorageBytes = bytes,
            isLoading = false
          )
        )
      }
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { settingsRepository.setThemeMode(mode) }
  }

  fun setFontSize(sizeSp: Float) {
    viewModelScope.launch { settingsRepository.setFontSizeSp(sizeSp) }
  }

  fun setLineSpacing(multiplier: Float) {
    viewModelScope.launch { settingsRepository.setLineSpacingMultiplier(multiplier) }
  }

  fun setKeepScreenAwakeDefault(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setKeepScreenAwakeDefault(enabled) }
  }

  fun setAutosaveEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setAutosaveEnabled(enabled) }
  }

  fun setRecordingQuality(quality: RecordingQuality) {
    viewModelScope.launch { settingsRepository.setRecordingQuality(quality) }
  }

  fun setCommunicationModeEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setCommunicationModeEnabled(enabled) }
  }

  fun setDefaultVolume(volume: Float) {
    viewModelScope.launch { settingsRepository.setDefaultVolume(volume) }
  }
}
