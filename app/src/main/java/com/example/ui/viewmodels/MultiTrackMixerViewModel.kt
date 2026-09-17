package com.example.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFileManager
import com.example.audio.WaveformAnalyzer
import com.example.audio.export.MixdownExportResult
import com.example.audio.export.MixdownProgress
import com.example.audio.export.WavMixdownExporter
import com.example.audio.mixer.MixerPlaybackState
import com.example.audio.mixer.MultiTrackAudioMixer
import com.example.audio.mixer.TrackChannel
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.AudioTrack
import com.example.domain.models.Song
import com.example.domain.models.TrackType
import kotlinx.coroutines.CancellationException
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
  val showDeleteConfirmDialog: AudioTrack? = null,
  val showExportSheet: Boolean = false,
  val isExporting: Boolean = false,
  val exportProgress: MixdownProgress = MixdownProgress(),
  val exportResult: MixdownExportResult? = null,
  val exportErrorMessage: String? = null
)

class MultiTrackMixerViewModel(
  val songId: String,
  private val songRepository: SongRepository,
  private val audioRepository: AudioRepository,
  private val audioFileManager: AudioFileManager,
  private val waveformAnalyzer: WaveformAnalyzer,
  private val wavMixdownExporter: WavMixdownExporter,
  private val mixer: MultiTrackAudioMixer
) : ViewModel() {

  private val _uiState = MutableStateFlow(MultiTrackMixerUiState())
  val uiState: StateFlow<MultiTrackMixerUiState> = _uiState.asStateFlow()

  private var waveformJobs = HashMap<String, Job>()
  private var exportJob: Job? = null

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

  // --- WAV Mixdown Export Controls ---

  fun openExportSheet() {
    _uiState.update {
      it.copy(
        showExportSheet = true,
        exportErrorMessage = null,
        isExporting = false,
        exportResult = null,
        exportProgress = MixdownProgress()
      )
    }
  }

  fun closeExportSheet() {
    cancelExport()
    _uiState.update {
      it.copy(
        showExportSheet = false,
        isExporting = false,
        exportErrorMessage = null
      )
    }
  }

  fun startWavExport() {
    exportJob?.cancel()
    val channels = mixer.tracks.value
    if (channels.isEmpty()) {
      _uiState.update { it.copy(exportErrorMessage = "No tracks available to export.") }
      return
    }

    val songTitle = _uiState.value.song?.title?.replace(Regex("[^a-zA-Z0-9._-]"), "_") ?: "Mix"
    val exportFileName = "${songTitle}_Master_${System.currentTimeMillis()}.wav"
    val masterGain = _uiState.value.playbackState.masterVolume

    exportJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isExporting = true,
          exportErrorMessage = null,
          exportResult = null,
          exportProgress = MixdownProgress(fraction = 0f, statusMessage = "Starting mixdown...")
        )
      }

      val result = wavMixdownExporter.exportMixToFile(
        tracks = channels,
        masterVolume = masterGain,
        fileName = exportFileName,
        onProgress = { progress ->
          _uiState.update { it.copy(exportProgress = progress) }
        }
      )

      _uiState.update {
        it.copy(
          isExporting = false,
          exportResult = result,
          exportErrorMessage = if (!result.success) result.errorMessage ?: "Export failed" else null
        )
      }
    }
  }

  fun cancelExport() {
    exportJob?.cancel()
    exportJob = null
    _uiState.update {
      it.copy(
        isExporting = false,
        exportErrorMessage = if (it.isExporting) "Export cancelled." else it.exportErrorMessage
      )
    }
  }

  fun exportToSafDestination(destinationUri: Uri) {
    val channels = mixer.tracks.value
    val masterGain = _uiState.value.playbackState.masterVolume

    exportJob?.cancel()
    exportJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isExporting = true,
          exportErrorMessage = null,
          exportResult = null,
          exportProgress = MixdownProgress(fraction = 0f, statusMessage = "Saving to selected folder...")
        )
      }

      val result = wavMixdownExporter.exportMixToSafUri(
        safDestinationUri = destinationUri,
        tracks = channels,
        masterVolume = masterGain,
        onProgress = { progress ->
          _uiState.update { it.copy(exportProgress = progress) }
        }
      )

      _uiState.update {
        it.copy(
          isExporting = false,
          exportResult = result,
          exportErrorMessage = if (!result.success) result.errorMessage ?: "Failed to save file" else null
        )
      }
    }
  }

  fun getShareIntent(): Intent? {
    val file = _uiState.value.exportResult?.file ?: return null
    val songTitle = _uiState.value.song?.title ?: "Song Mix"
    return wavMixdownExporter.createShareIntent(file, songTitle)
  }

  override fun onCleared() {
    super.onCleared()
    exportJob?.cancel()
    mixer.release()
    for ((_, job) in waveformJobs) {
      job.cancel()
    }
    waveformJobs.clear()
  }
}
