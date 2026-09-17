package com.example.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFileManager
import com.example.audio.WaveformAnalyzer
import com.example.audio.mixer.MixerPlaybackState
import com.example.audio.mixer.MultiTrackAudioMixer
import com.example.audio.mixer.TrackChannel
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.AudioTrack
import com.example.domain.models.Song
import com.example.domain.models.TrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class MixerTrackUiModel(
  val track: AudioTrack,
  val waveform: FloatArray? = null,
  val isWaveformLoading: Boolean = false,
  val fileExists: Boolean = true
)

data class MultiTrackMixerUiState(
  val song: Song? = null,
  val tracks: List<MixerTrackUiModel> = emptyList(),
  val playbackState: MixerPlaybackState = MixerPlaybackState(),
  val isImportingAudio: Boolean = false,
  val pendingImportType: TrackType = TrackType.IMPORTED_AUDIO,
  val errorMessage: String? = null,
  val showDeleteConfirmDialog: AudioTrack? = null
)

class MultiTrackMixerViewModel(
  val songId: String,
  private val songRepository: SongRepository,
  private val audioRepository: AudioRepository,
  private val audioFileManager: AudioFileManager,
  private val waveformAnalyzer: WaveformAnalyzer,
  private val mixer: MultiTrackAudioMixer
) : ViewModel() {

  private val _uiState = MutableStateFlow(MultiTrackMixerUiState())
  val uiState: StateFlow<MultiTrackMixerUiState> = _uiState.asStateFlow()

  private var waveformJobs = HashMap<String, Job>()

  init {
    loadSong()
    observeMixerState()
    observeSongTracks()
  }

  private fun loadSong() {
    viewModelScope.launch {
      val song = songRepository.getSongByIdSync(songId)
      _uiState.update { it.copy(song = song) }
    }
  }

  private fun observeMixerState() {
    viewModelScope.launch {
      mixer.playbackState.collect { state ->
        _uiState.update { it.copy(playbackState = state) }
      }
    }
  }

  private fun observeSongTracks() {
    viewModelScope.launch {
      audioRepository.getTracksForSong(songId).collect { tracksList ->
        // Convert to mixer TrackChannel list
        val channels = tracksList.map { track ->
          TrackChannel(
            id = track.id,
            name = track.name,
            filePath = track.uri,
            type = track.type.name,
            volume = track.volume,
            isMuted = track.muted,
            isSolo = track.solo,
            durationMs = track.duration
          )
        }
        mixer.setTracks(channels)

        // Build UI state with waveforms
        val existingMap = _uiState.value.tracks.associateBy { it.track.id }
        val uiModels = tracksList.map { track ->
          val existing = existingMap[track.id]
          MixerTrackUiModel(
            track = track,
            waveform = existing?.waveform,
            isWaveformLoading = existing?.isWaveformLoading ?: false,
            fileExists = File(track.uri).exists()
          )
        }
        _uiState.update { it.copy(tracks = uiModels) }

        // Trigger waveform extraction for any tracks that don't have it loaded yet
        for (item in uiModels) {
          if (item.waveform == null && item.fileExists && !waveformJobs.containsKey(item.track.id)) {
            loadWaveformForTrack(item.track.id, item.track.uri)
          }
        }
      }
    }
  }

  private fun loadWaveformForTrack(trackId: String, filePath: String) {
    waveformJobs[trackId]?.cancel()
    val job = viewModelScope.launch {
      _uiState.update { state ->
        val updated = state.tracks.map {
          if (it.track.id == trackId) it.copy(isWaveformLoading = true) else it
        }
        state.copy(tracks = updated)
      }

      val waveform = waveformAnalyzer.extractWaveform(filePath, targetBuckets = 100)

      _uiState.update { state ->
        val updated = state.tracks.map {
          if (it.track.id == trackId) it.copy(waveform = waveform, isWaveformLoading = false) else it
        }
        state.copy(tracks = updated)
      }
      waveformJobs.remove(trackId)
    }
    waveformJobs[trackId] = job
  }

  // --- Playback Controls ---

  fun play() = mixer.play()
  fun pause() = mixer.pause()
  fun stop() = mixer.stop()
  fun seekTo(positionMs: Long) = mixer.seekTo(positionMs)

  fun setMasterVolume(volume: Float) {
    mixer.setMasterVolume(volume)
  }

  // --- Track Controls ---

  fun setTrackVolume(trackId: String, volume: Float) {
    mixer.setTrackVolume(trackId, volume)
    viewModelScope.launch {
      val trackModel = _uiState.value.tracks.find { it.track.id == trackId }?.track ?: return@launch
      audioRepository.updateTrack(trackModel.copy(volume = volume.coerceIn(0f, 2.0f)))
    }
  }

  fun toggleTrackMute(trackId: String) {
    mixer.toggleTrackMute(trackId)
    viewModelScope.launch {
      val trackModel = _uiState.value.tracks.find { it.track.id == trackId }?.track ?: return@launch
      audioRepository.updateTrack(trackModel.copy(muted = !trackModel.muted))
    }
  }

  fun toggleTrackSolo(trackId: String) {
    mixer.toggleTrackSolo(trackId)
    viewModelScope.launch {
      val trackModel = _uiState.value.tracks.find { it.track.id == trackId }?.track ?: return@launch
      audioRepository.updateTrack(trackModel.copy(solo = !trackModel.solo))
    }
  }

  fun confirmDeleteTrack(track: AudioTrack) {
    _uiState.update { it.copy(showDeleteConfirmDialog = track) }
  }

  fun dismissDeleteDialog() {
    _uiState.update { it.copy(showDeleteConfirmDialog = null) }
  }

  fun deleteTrack(trackId: String) {
    _uiState.update { it.copy(showDeleteConfirmDialog = null) }
    mixer.removeTrack(trackId)
    waveformJobs[trackId]?.cancel()
    waveformJobs.remove(trackId)

    viewModelScope.launch {
      val trackModel = _uiState.value.tracks.find { it.track.id == trackId }?.track
      if (trackModel != null) {
        audioFileManager.deleteBeatFile(trackModel.uri)
      }
      audioRepository.deleteTrack(trackId)
    }
  }

  // --- Track Import Actions ---

  fun startImport(type: TrackType) {
    _uiState.update { it.copy(pendingImportType = type) }
  }

  fun handleImportedAudio(uri: Uri) {
    val importType = _uiState.value.pendingImportType
    viewModelScope.launch {
      _uiState.update { it.copy(isImportingAudio = true, errorMessage = null) }
      val result = audioFileManager.importAudioFile(uri, songId)
      result.onSuccess { imported ->
        val track = audioRepository.addTrack(
          songId = songId,
          name = imported.fileName,
          uri = imported.localPath,
          type = importType,
          duration = imported.durationMs
        )
        _uiState.update { it.copy(isImportingAudio = false) }
        loadWaveformForTrack(track.id, track.uri)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isImportingAudio = false,
            errorMessage = err.message ?: "Failed to import audio track"
          )
        }
      }
    }
  }

  fun dismissErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    mixer.release()
    for ((_, job) in waveformJobs) {
      job.cancel()
    }
    waveformJobs.clear()
  }
}
