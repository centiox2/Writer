package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFileManager
import com.example.audio.AudioRecorderEngine
import com.example.audio.RecordingPreviewPlayer
import com.example.audio.RecordingPreviewState
import com.example.audio.RecordingState
import com.example.audio.WaveformAnalyzer
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.data.settings.AppSettings
import com.example.data.settings.SettingsRepository
import com.example.domain.models.Recording
import com.example.domain.models.Song
import com.example.domain.models.TrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingItemUiModel(
  val recording: Recording,
  val songTitle: String? = null,
  val waveform: FloatArray? = null,
  val isWaveformLoading: Boolean = false,
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val isMuted: Boolean = false,
  val isSolo: Boolean = false,
  val fileExists: Boolean = true
)

data class RecordingsUiState(
  val recordings: List<RecordingItemUiModel> = emptyList(),
  val songs: List<Song> = emptyList(),
  val selectedSongFilterId: String? = null,
  val searchQuery: String = "",
  val activePlayingRecordingId: String? = null,
  val isMuted: Boolean = false,
  val soloedRecordingId: String? = null,
  // Quick recorder state
  val isRecordingIdea: Boolean = false,
  val recorderState: RecordingState = RecordingState.IDLE,
  val recordingDurationMs: Long = 0L,
  val liveAmplitude: Float = 0f,
  val liveAmplitudesHistory: List<Float> = emptyList(),
  val targetSongForNewRecording: String? = null,
  // Dialog states
  val showRenameDialog: Recording? = null,
  val showDeleteDialog: Recording? = null,
  val showAttachToMixerDialog: Recording? = null,
  val showNewTakeSheet: Boolean = false,
  val message: String? = null,
  val errorMessage: String? = null
)

class RecordingsViewModel(
  private val audioRepository: AudioRepository,
  private val songRepository: SongRepository,
  private val audioFileManager: AudioFileManager,
  private val waveformAnalyzer: WaveformAnalyzer,
  private val previewPlayer: RecordingPreviewPlayer,
  private val recorderEngine: AudioRecorderEngine,
  private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

  private val _uiState = MutableStateFlow(RecordingsUiState())
  val uiState: StateFlow<RecordingsUiState> = _uiState.asStateFlow()

  private val waveformMap = HashMap<String, FloatArray>()
  private val waveformJobs = HashMap<String, Job>()
  private var currentRecordingFile: File? = null

  private var cachedRecordings: List<Recording> = emptyList()
  private var cachedSongs: List<Song> = emptyList()
  private var cachedSettings: AppSettings = AppSettings()

  init {
    observeRecordings()
    observeSongs()
    observePreviewPlayerState()
    observeRecorderStatus()
    observeSettings()
  }

  private fun observeSettings() {
    val repo = settingsRepository ?: return
    viewModelScope.launch {
      repo.settings.collect { cachedSettings = it }
    }
  }

  suspend fun refreshDataSync() {
    cachedRecordings = audioRepository.getAllRecordingsSync()
    cachedSongs = songRepository.getActiveSongsSync()
    refreshUiList()
  }

  private fun observeRecordings() {
    viewModelScope.launch {
      audioRepository.getAllRecordings().collect { recordings ->
        cachedRecordings = recordings
        refreshUiList()
      }
    }
  }

  private fun observeSongs() {
    viewModelScope.launch {
      songRepository.getActiveSongs().collect { songs ->
        cachedSongs = songs
        refreshUiList()
      }
    }
  }

  private fun refreshUiList() {
    val state = _uiState.value
    val songMap = cachedSongs.associateBy { it.id }

    var filtered = if (state.selectedSongFilterId != null) {
      cachedRecordings.filter { it.songId == state.selectedSongFilterId }
    } else {
      cachedRecordings
    }

    if (state.searchQuery.isNotBlank()) {
      val q = state.searchQuery.trim().lowercase()
      filtered = filtered.filter {
        it.name.lowercase().contains(q) ||
            (songMap[it.songId]?.title?.lowercase()?.contains(q) == true)
      }
    }

    val items = filtered.map { rec ->
      val isPlaying = state.activePlayingRecordingId == rec.id && state.recorderState == RecordingState.IDLE
      val pos = if (isPlaying) state.recordings.find { it.recording.id == rec.id }?.currentPositionMs ?: 0L else 0L
      val waveform = waveformMap[rec.id]
      val isWaveformLoading = waveform == null && !waveformMap.containsKey(rec.id)
      val exists = audioFileManager.fileExists(rec.uri)

      if (waveform == null && !waveformJobs.containsKey(rec.id)) {
        loadWaveformForRecording(rec)
      }

      RecordingItemUiModel(
        recording = rec,
        songTitle = songMap[rec.songId]?.title ?: "Standalone Idea",
        waveform = waveform,
        isWaveformLoading = isWaveformLoading,
        isPlaying = isPlaying,
        currentPositionMs = pos,
        isMuted = state.isMuted && state.activePlayingRecordingId == rec.id,
        isSolo = state.soloedRecordingId == rec.id,
        fileExists = exists
      )
    }

    _uiState.update {
      it.copy(
        recordings = items,
        songs = cachedSongs
      )
    }
  }

  private fun observePreviewPlayerState() {
    viewModelScope.launch {
      previewPlayer.state.collect { pState ->
        _uiState.update { current ->
          val activeId = if (pState.isPlaying) pState.activeRecordingId else null
          val updatedList = current.recordings.map { item ->
            if (item.recording.id == pState.activeRecordingId) {
              item.copy(
                isPlaying = pState.isPlaying,
                currentPositionMs = pState.currentPositionMs,
                isMuted = pState.isMuted,
                isSolo = current.soloedRecordingId == item.recording.id
              )
            } else {
              item.copy(
                isPlaying = false,
                currentPositionMs = 0L,
                isMuted = false,
                isSolo = current.soloedRecordingId == item.recording.id
              )
            }
          }
          current.copy(
            activePlayingRecordingId = activeId,
            isMuted = pState.isMuted,
            recordings = updatedList,
            errorMessage = pState.errorMessage
          )
        }
      }
    }
  }

  private fun observeRecorderStatus() {
    viewModelScope.launch {
      recorderEngine.status.collect { rStatus ->
        _uiState.update { current ->
          val newHistory = if (rStatus.state == RecordingState.RECORDING) {
            (current.liveAmplitudesHistory + rStatus.currentAmplitude).takeLast(60)
          } else if (rStatus.state == RecordingState.IDLE) {
            emptyList()
          } else {
            current.liveAmplitudesHistory
          }

          current.copy(
            isRecordingIdea = rStatus.state == RecordingState.RECORDING || rStatus.state == RecordingState.PAUSED,
            recorderState = rStatus.state,
            recordingDurationMs = rStatus.durationMs,
            liveAmplitude = rStatus.currentAmplitude,
            liveAmplitudesHistory = newHistory,
            errorMessage = rStatus.errorMessage
          )
        }
      }
    }
  }

  private fun loadWaveformForRecording(recording: Recording) {
    waveformJobs[recording.id]?.cancel()
    val job = viewModelScope.launch(Dispatchers.Default) {
      val wf = waveformAnalyzer.extractWaveform(recording.uri, targetBuckets = 72)
      waveformMap[recording.id] = wf

      _uiState.update { state ->
        val updated = state.recordings.map {
          if (it.recording.id == recording.id) {
            it.copy(waveform = wf, isWaveformLoading = false)
          } else it
        }
        state.copy(recordings = updated)
      }
    }
    waveformJobs[recording.id] = job
  }

  // --- Filtering & Search ---
  fun setSongFilter(songId: String?) {
    _uiState.update { it.copy(selectedSongFilterId = songId) }
    refreshUiList()
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    refreshUiList()
  }

  // --- Playback / Preview Actions ---
  fun togglePlayTake(recording: Recording) {
    previewPlayer.playTake(recording.id, recording.uri)
  }

  fun seekTake(recording: Recording, fraction: Float) {
    val duration = if (recording.duration > 0) recording.duration else 1000L
    val targetPos = (fraction * duration).toLong().coerceIn(0L, duration)
    previewPlayer.seekTo(targetPos)
  }

  fun toggleMute(recordingId: String) {
    val newMuted = !_uiState.value.isMuted
    previewPlayer.setMuted(newMuted)
    _uiState.update { it.copy(isMuted = newMuted) }
  }

  fun toggleSolo(recordingId: String) {
    val currentSolo = _uiState.value.soloedRecordingId
    val nextSolo = if (currentSolo == recordingId) null else recordingId
    _uiState.update { it.copy(soloedRecordingId = nextSolo) }
    previewPlayer.toggleSolo(recordingId)
  }

  // --- Take Management: Rename, Delete, Duplicate, Attach to Mixer ---
  fun promptRename(recording: Recording) {
    _uiState.update { it.copy(showRenameDialog = recording) }
  }

  fun dismissRenameDialog() {
    _uiState.update { it.copy(showRenameDialog = null) }
  }

  fun renameRecording(recordingId: String, newName: String) {
    if (newName.isBlank()) return
    viewModelScope.launch {
      audioRepository.renameRecording(recordingId, newName.trim())
      _uiState.update {
        it.copy(
          showRenameDialog = null,
          message = "Renamed take to \"${newName.trim()}\""
        )
      }
    }
  }

  fun promptDelete(recording: Recording) {
    _uiState.update { it.copy(showDeleteDialog = recording) }
  }

  fun dismissDeleteDialog() {
    _uiState.update { it.copy(showDeleteDialog = null) }
  }

  fun deleteRecording(recording: Recording) {
    viewModelScope.launch {
      if (_uiState.value.activePlayingRecordingId == recording.id) {
        previewPlayer.pause()
      }
      audioRepository.deleteRecording(recording.id)
      audioFileManager.deleteAudioFile(recording.uri)
      waveformMap.remove(recording.id)
      waveformJobs.remove(recording.id)

      _uiState.update {
        it.copy(
          showDeleteDialog = null,
          message = "Deleted take \"${recording.name}\""
        )
      }
    }
  }

  fun duplicateRecording(recording: Recording) {
    viewModelScope.launch {
      val duplicatePath = audioFileManager.duplicateAudioFile(recording.uri) ?: recording.uri
      val duplicated = audioRepository.duplicateRecording(
        recordingId = recording.id,
        newName = "${recording.name} (Copy)",
        newUri = duplicatePath
      )
      if (duplicated != null) {
        loadWaveformForRecording(duplicated)
        _uiState.update {
          it.copy(message = "Duplicated take as \"${duplicated.name}\"")
        }
      }
    }
  }

  fun promptAttachToMixer(recording: Recording) {
    _uiState.update { it.copy(showAttachToMixerDialog = recording) }
  }

  fun dismissAttachToMixerDialog() {
    _uiState.update { it.copy(showAttachToMixerDialog = null) }
  }

  fun attachRecordingToMixer(
    recording: Recording,
    targetSongId: String? = null,
    trackType: TrackType = TrackType.RECORDING
  ) {
    viewModelScope.launch {
      val effectiveSongId = targetSongId ?: recording.songId
      val track = audioRepository.attachRecordingToMixer(
        recordingId = recording.id,
        targetSongId = effectiveSongId,
        trackType = trackType,
        volume = cachedSettings.defaultVolume
      )
      if (track != null) {
        val songName = _uiState.value.songs.find { it.id == effectiveSongId }?.title ?: "song"
        _uiState.update {
          it.copy(
            showAttachToMixerDialog = null,
            message = "Attached \"${recording.name}\" to mixer of \"$songName\""
          )
        }
      } else {
        _uiState.update {
          it.copy(
            showAttachToMixerDialog = null,
            errorMessage = "Failed to attach track to mixer"
          )
        }
      }
    }
  }

  // --- Voice Idea & Take Recorder ---
  fun openNewTakeSheet(songId: String? = null) {
    _uiState.update {
      it.copy(
        showNewTakeSheet = true,
        targetSongForNewRecording = songId ?: it.selectedSongFilterId
      )
    }
  }

  fun dismissNewTakeSheet() {
    if (_uiState.value.isRecordingIdea) {
      cancelActiveRecording()
    }
    _uiState.update {
      it.copy(
        showNewTakeSheet = false,
        targetSongForNewRecording = null
      )
    }
  }

  fun startRecordingNewTake(targetSongId: String? = null) {
    previewPlayer.pause()
    val songId = targetSongId ?: _uiState.value.targetSongForNewRecording ?: _uiState.value.songs.firstOrNull()?.id ?: "standalone"
    val file = audioFileManager.createNewRecordingFile(songId = songId)
    currentRecordingFile = file

    val started = recorderEngine.startRecording(
      targetFile = file,
      quality = cachedSettings.recordingQuality,
      useCommunicationMode = cachedSettings.communicationModeEnabled
    )
    if (started) {
      _uiState.update {
        it.copy(
          targetSongForNewRecording = songId,
          showNewTakeSheet = true
        )
      }
    }
  }

  fun pauseRecordingTake() {
    recorderEngine.pauseRecording()
  }

  fun resumeRecordingTake() {
    recorderEngine.resumeRecording()
  }

  fun stopAndSaveRecordingTake(customName: String? = null, targetSongId: String? = null) {
    val file = currentRecordingFile
    val durationMs = recorderEngine.stopRecording()

    viewModelScope.launch {
      val songId = targetSongId ?: _uiState.value.targetSongForNewRecording ?: _uiState.value.songs.firstOrNull()?.id ?: "standalone"
      val songTitle = _uiState.value.songs.find { it.id == songId }?.title

      val defaultDate = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(Date())
      val takeName = if (!customName.isNullOrBlank()) {
        customName.trim()
      } else {
        "Take • $defaultDate"
      }

      val localPath = file?.absolutePath ?: ""
      val actualDuration = if (durationMs > 0) durationMs else audioFileManager.extractDuration(localPath)

      val savedRecording = audioRepository.addRecording(
        songId = songId,
        name = takeName,
        uri = localPath,
        duration = actualDuration
      )

      loadWaveformForRecording(savedRecording)

      _uiState.update {
        it.copy(
          showNewTakeSheet = false,
          targetSongForNewRecording = null,
          message = "Saved new take \"$takeName\""
        )
      }
    }
  }

  fun cancelActiveRecording() {
    recorderEngine.cancelRecording()
    currentRecordingFile = null
    _uiState.update {
      it.copy(
        isRecordingIdea = false,
        showNewTakeSheet = false
      )
    }
  }

  fun clearMessage() {
    _uiState.update { it.copy(message = null) }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  override fun onCleared() {
    super.onCleared()
    previewPlayer.release()
    recorderEngine.release()
  }
}
