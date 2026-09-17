package com.example.ui.viewmodels

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioFileManager
import com.example.audio.BeatPlaybackState
import com.example.audio.BeatPlayer
import com.example.audio.WaveformAnalyzer
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.AudioTrack
import com.example.domain.models.Song
import com.example.domain.models.TrackType
import com.example.rhyme.RhymeEngine
import com.example.rhyme.RhymeQueryResult
import com.example.rhyme.WordNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveStatus {
  SAVED,
  SAVING,
  UNSAVED
}

enum class FontFamilyChoice {
  SANS_SERIF,
  SERIF,
  MONOSPACE
}

data class SectionOutlineItem(
  val title: String,
  val startIndex: Int,
  val lineNumber: Int
)

data class LyricEditorUiState(
  val song: Song? = null,
  val textFieldValue: TextFieldValue = TextFieldValue(""),
  val saveStatus: SaveStatus = SaveStatus.SAVED,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,
  val isSearchActive: Boolean = false,
  val searchQuery: String = "",
  val searchMatchCount: Int = 0,
  val currentMatchIndex: Int = -1,
  val fontSizeSp: Float = 18f,
  val lineSpacingMultiplier: Float = 1.5f,
  val fontFamilyChoice: FontFamilyChoice = FontFamilyChoice.SANS_SERIF,
  val keepScreenOn: Boolean = false,
  val wordCount: Int = 0,
  val charCount: Int = 0,
  val lineCount: Int = 0,
  val detectedSections: List<SectionOutlineItem> = emptyList(),
  val showClearConfirmDialog: Boolean = false,
  val showFormatSheet: Boolean = false,
  val showSectionJumpSheet: Boolean = false,
  val showStatsDialog: Boolean = false,
  val isLoaded: Boolean = false,
  // Phase 4 Beat & Waveform State
  val beatTrack: AudioTrack? = null,
  val beatPlaybackState: BeatPlaybackState = BeatPlaybackState(),
  val waveformAmplitudes: FloatArray? = null,
  val isWaveformLoading: Boolean = false,
  val beatImportError: String? = null,
  // Phase 5 Rhyme Assistant State
  val showRhymeSheet: Boolean = false,
  val rhymeQueryWord: String = "",
  val rhymeQueryResult: RhymeQueryResult? = null,
  val isRhymeSearching: Boolean = false
)

class LyricEditorViewModel(
  val songId: String,
  private val songRepository: SongRepository,
  initialSong: Song? = null,
  private val audioRepository: AudioRepository? = null,
  private val audioFileManager: AudioFileManager? = null,
  private val waveformAnalyzer: WaveformAnalyzer? = null,
  private val beatPlayer: BeatPlayer? = null,
  private val rhymeEngine: RhymeEngine = RhymeEngine(),
  defaultFontSizeSp: Float = 18f,
  defaultLineSpacingMultiplier: Float = 1.5f,
  defaultKeepScreenOn: Boolean = false,
  private val autosaveEnabled: Boolean = true
) : ViewModel() {

  private val _uiState = MutableStateFlow(
    LyricEditorUiState(
      fontSizeSp = defaultFontSizeSp,
      lineSpacingMultiplier = defaultLineSpacingMultiplier,
      keepScreenOn = defaultKeepScreenOn
    )
  )
  val uiState: StateFlow<LyricEditorUiState> = _uiState.asStateFlow()

  // Undo / Redo history stacks
  private val undoStack = mutableListOf<TextFieldValue>()
  private val redoStack = mutableListOf<TextFieldValue>()
  private val maxHistorySize = 50
  private var lastSnapshotTime = 0L

  private var autosaveJob: Job? = null
  private var lastSavedText: String = ""
  private var waveformJob: Job? = null
  private var rhymeJob: Job? = null

  init {
    if (initialSong != null) {
      val initialText = initialSong.lyrics
      val initialValue = TextFieldValue(initialText, TextRange(initialText.length))
      lastSavedText = initialText

      _uiState.update { state ->
        state.copy(
          song = initialSong,
          textFieldValue = initialValue,
          saveStatus = SaveStatus.SAVED,
          wordCount = calculateWordCount(initialText),
          charCount = initialText.length,
          lineCount = calculateLineCount(initialText),
          detectedSections = extractSections(initialText),
          isLoaded = true
        )
      }
    } else {
      loadSong()
    }

    // Connect Beat Player state observer
    beatPlayer?.let { player ->
      viewModelScope.launch {
        player.playbackState.collect { playbackState ->
          _uiState.update { it.copy(beatPlaybackState = playbackState) }
        }
      }
    }

    // Observe beat track for this song
    audioRepository?.let { repo ->
      viewModelScope.launch {
        repo.getBeatTrackForSong(songId).collect { track ->
          _uiState.update { it.copy(beatTrack = track) }
          if (track != null) {
            beatPlayer?.loadBeat(track.uri, initialVolume = track.volume)
            loadWaveform(track.uri)
          } else {
            _uiState.update { it.copy(waveformAmplitudes = null, isWaveformLoading = false) }
          }
        }
      }
    }
  }

  fun loadSong() {
    viewModelScope.launch {
      val song = songRepository.getSongByIdSync(songId)
      if (song != null) {
        val initialText = song.lyrics
        val initialValue = TextFieldValue(initialText, TextRange(initialText.length))
        lastSavedText = initialText

        _uiState.update { state ->
          state.copy(
            song = song,
            textFieldValue = initialValue,
            saveStatus = SaveStatus.SAVED,
            wordCount = calculateWordCount(initialText),
            charCount = initialText.length,
            lineCount = calculateLineCount(initialText),
            detectedSections = extractSections(initialText),
            isLoaded = true
          )
        }
      }
    }
  }

  fun onTextChanged(newValue: TextFieldValue) {
    val previousValue = _uiState.value.textFieldValue
    val textChanged = newValue.text != previousValue.text

    if (textChanged) {
      val now = System.currentTimeMillis()
      // Coalesce rapid typing: create a new undo checkpoint if more than 800ms passed,
      // or if whitespace was added (word finished), or if length difference is > 1 (paste/cut)
      val timeDiff = now - lastSnapshotTime
      val isWordBoundary = newValue.text.length > previousValue.text.length &&
          (newValue.text.lastOrNull()?.isWhitespace() == true)
      val isLargeChange = kotlin.math.abs(newValue.text.length - previousValue.text.length) > 1

      if (undoStack.isEmpty() || timeDiff > 800L || isWordBoundary || isLargeChange) {
        pushUndo(previousValue)
        lastSnapshotTime = now
      }
      redoStack.clear()

      val words = calculateWordCount(newValue.text)
      val chars = newValue.text.length
      val lines = calculateLineCount(newValue.text)
      val sections = extractSections(newValue.text)

      _uiState.update { state ->
        state.copy(
          textFieldValue = newValue,
          saveStatus = SaveStatus.UNSAVED,
          canUndo = undoStack.isNotEmpty(),
          canRedo = false,
          wordCount = words,
          charCount = chars,
          lineCount = lines,
          detectedSections = sections
        )
      }

      if (_uiState.value.isSearchActive && _uiState.value.searchQuery.isNotBlank()) {
        updateSearchMatches(_uiState.value.searchQuery, newValue.text)
      }

      scheduleAutosave(newValue.text)
    } else {
      // Only cursor / selection changed
      _uiState.update { it.copy(textFieldValue = newValue) }
    }
  }

  private fun pushUndo(snapshot: TextFieldValue) {
    undoStack.add(snapshot)
    if (undoStack.size > maxHistorySize) {
      undoStack.removeAt(0)
    }
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val current = _uiState.value.textFieldValue
    val previous = undoStack.removeAt(undoStack.lastIndex)

    redoStack.add(current)
    if (redoStack.size > maxHistorySize) {
      redoStack.removeAt(0)
    }

    applyValueAndTriggerAutosave(previous)
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val current = _uiState.value.textFieldValue
    val next = redoStack.removeAt(redoStack.lastIndex)

    undoStack.add(current)
    if (undoStack.size > maxHistorySize) {
      undoStack.removeAt(0)
    }

    applyValueAndTriggerAutosave(next)
  }

  private fun applyValueAndTriggerAutosave(value: TextFieldValue) {
    val words = calculateWordCount(value.text)
    val chars = value.text.length
    val lines = calculateLineCount(value.text)
    val sections = extractSections(value.text)

    _uiState.update { state ->
      state.copy(
        textFieldValue = value,
        saveStatus = SaveStatus.UNSAVED,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        wordCount = words,
        charCount = chars,
        lineCount = lines,
        detectedSections = sections
      )
    }

    scheduleAutosave(value.text)
  }

  fun insertSection(sectionTitle: String) {
    val current = _uiState.value.textFieldValue
    pushUndo(current)
    redoStack.clear()

    val text = current.text
    val cursor = current.selection.start.coerceIn(0, text.length)
    val tag = "[$sectionTitle]"

    val prefix: String
    val suffix = "\n"

    if (text.isEmpty()) {
      prefix = ""
    } else if (cursor == 0) {
      prefix = ""
    } else {
      val before = text.substring(0, cursor)
      prefix = when {
        before.endsWith("\n\n") -> ""
        before.endsWith("\n") -> "\n"
        else -> "\n\n"
      }
    }

    val inserted = "$prefix$tag$suffix"
    val newText = text.substring(0, cursor) + inserted + text.substring(cursor)
    val newCursorPos = cursor + inserted.length
    val newValue = TextFieldValue(newText, TextRange(newCursorPos))

    applyValueAndTriggerAutosave(newValue)
  }

  fun insertTextAtCursor(insertText: String) {
    val current = _uiState.value.textFieldValue
    pushUndo(current)
    redoStack.clear()

    val text = current.text
    val start = current.selection.min.coerceIn(0, text.length)
    val end = current.selection.max.coerceIn(0, text.length)

    val newText = text.substring(0, start) + insertText + text.substring(end)
    val newCursorPos = start + insertText.length
    val newValue = TextFieldValue(newText, TextRange(newCursorPos))

    applyValueAndTriggerAutosave(newValue)
  }

  fun cutSelectedText(): String {
    val current = _uiState.value.textFieldValue
    val text = current.text
    val start = current.selection.min.coerceIn(0, text.length)
    val end = current.selection.max.coerceIn(0, text.length)

    if (start == end) return ""

    val cutText = text.substring(start, end)
    pushUndo(current)
    redoStack.clear()

    val newText = text.substring(0, start) + text.substring(end)
    val newValue = TextFieldValue(newText, TextRange(start))

    applyValueAndTriggerAutosave(newValue)
    return cutText
  }

  fun selectAll() {
    val text = _uiState.value.textFieldValue.text
    _uiState.update {
      it.copy(
        textFieldValue = it.textFieldValue.copy(
          selection = TextRange(0, text.length)
        )
      )
    }
  }

  fun clearSelection() {
    val current = _uiState.value.textFieldValue
    val cursor = current.selection.end
    _uiState.update {
      it.copy(
        textFieldValue = it.textFieldValue.copy(
          selection = TextRange(cursor)
        )
      )
    }
  }

  fun clearLyrics() {
    val current = _uiState.value.textFieldValue
    if (current.text.isEmpty()) return

    pushUndo(current)
    redoStack.clear()

    val emptyValue = TextFieldValue("", TextRange.Zero)
    applyValueAndTriggerAutosave(emptyValue)
  }

  private fun scheduleAutosave(text: String) {
    if (!autosaveEnabled) return
    autosaveJob?.cancel()
    autosaveJob = viewModelScope.launch {
      delay(600L) // 600ms debounce
      saveLyricsToDatabase(text)
    }
  }

  suspend fun saveLyricsToDatabase(text: String) {
    if (text == lastSavedText && _uiState.value.saveStatus == SaveStatus.SAVED) return

    _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
    try {
      songRepository.updateLyrics(songId, text)
      lastSavedText = text
      _uiState.update { it.copy(saveStatus = SaveStatus.SAVED) }
    } catch (e: Exception) {
      _uiState.update { it.copy(saveStatus = SaveStatus.UNSAVED) }
    }
  }

  fun saveNow() {
    autosaveJob?.cancel()
    val text = _uiState.value.textFieldValue.text
    viewModelScope.launch {
      saveLyricsToDatabase(text)
    }
  }

  suspend fun saveNowSync() {
    autosaveJob?.cancel()
    val text = _uiState.value.textFieldValue.text
    saveLyricsToDatabase(text)
  }

  // --- In-Editor Search ---

  fun toggleSearch() {
    val active = !_uiState.value.isSearchActive
    _uiState.update {
      it.copy(
        isSearchActive = active,
        searchQuery = if (!active) "" else it.searchQuery,
        searchMatchCount = if (!active) 0 else it.searchMatchCount,
        currentMatchIndex = if (!active) -1 else it.currentMatchIndex
      )
    }
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
    updateSearchMatches(query, _uiState.value.textFieldValue.text)
  }

  private fun updateSearchMatches(query: String, text: String) {
    if (query.isBlank() || text.isBlank()) {
      _uiState.update {
        it.copy(searchMatchCount = 0, currentMatchIndex = -1)
      }
      return
    }

    val matches = mutableListOf<Int>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index >= 0) {
      matches.add(index)
      index = text.indexOf(query, startIndex = index + query.length, ignoreCase = true)
    }

    val count = matches.size
    val nextIndex = if (count > 0) 0 else -1

    _uiState.update {
      it.copy(
        searchMatchCount = count,
        currentMatchIndex = nextIndex
      )
    }

    if (count > 0) {
      highlightMatch(matches[0], query.length)
    }
  }

  fun findNextMatch() {
    val state = _uiState.value
    if (state.searchMatchCount <= 0 || state.searchQuery.isBlank()) return

    val matches = findMatchIndices(state.searchQuery, state.textFieldValue.text)
    if (matches.isEmpty()) return

    val nextIdx = (state.currentMatchIndex + 1) % matches.size
    _uiState.update { it.copy(currentMatchIndex = nextIdx) }
    highlightMatch(matches[nextIdx], state.searchQuery.length)
  }

  fun findPreviousMatch() {
    val state = _uiState.value
    if (state.searchMatchCount <= 0 || state.searchQuery.isBlank()) return

    val matches = findMatchIndices(state.searchQuery, state.textFieldValue.text)
    if (matches.isEmpty()) return

    val prevIdx = if (state.currentMatchIndex <= 0) matches.size - 1 else state.currentMatchIndex - 1
    _uiState.update { it.copy(currentMatchIndex = prevIdx) }
    highlightMatch(matches[prevIdx], state.searchQuery.length)
  }

  private fun highlightMatch(startIndex: Int, length: Int) {
    _uiState.update {
      it.copy(
        textFieldValue = it.textFieldValue.copy(
          selection = TextRange(startIndex, startIndex + length)
        )
      )
    }
  }

  private fun findMatchIndices(query: String, text: String): List<Int> {
    val matches = mutableListOf<Int>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index >= 0) {
      matches.add(index)
      index = text.indexOf(query, startIndex = index + query.length, ignoreCase = true)
    }
    return matches
  }

  // --- Formatting & Preferences ---

  fun setFontSize(sizeSp: Float) {
    _uiState.update { it.copy(fontSizeSp = sizeSp.coerceIn(12f, 32f)) }
  }

  fun setLineSpacing(multiplier: Float) {
    _uiState.update { it.copy(lineSpacingMultiplier = multiplier.coerceIn(1.1f, 2.5f)) }
  }

  fun setFontFamily(family: FontFamilyChoice) {
    _uiState.update { it.copy(fontFamilyChoice = family) }
  }

  fun toggleKeepScreenOn() {
    _uiState.update { it.copy(keepScreenOn = !it.keepScreenOn) }
  }

  fun jumpToSection(startIndex: Int) {
    val text = _uiState.value.textFieldValue.text
    if (startIndex in 0..text.length) {
      val endOfHeader = text.indexOf('\n', startIndex).let { if (it == -1) text.length else it }
      _uiState.update {
        it.copy(
          textFieldValue = it.textFieldValue.copy(
            selection = TextRange(startIndex, endOfHeader)
          ),
          showSectionJumpSheet = false
        )
      }
    }
  }

  // Dialog & Sheet Visibility
  fun setShowClearConfirmDialog(show: Boolean) {
    _uiState.update { it.copy(showClearConfirmDialog = show) }
  }

  fun setShowFormatSheet(show: Boolean) {
    _uiState.update { it.copy(showFormatSheet = show) }
  }

  fun setShowSectionJumpSheet(show: Boolean) {
    _uiState.update { it.copy(showSectionJumpSheet = show) }
  }

  fun setShowStatsDialog(show: Boolean) {
    _uiState.update { it.copy(showStatsDialog = show) }
  }

  // --- Phase 5 Rhyme Assistant Methods ---

  fun openRhymeAssistant(initialWord: String? = null) {
    val wordToLookup = if (!initialWord.isNullOrBlank()) {
      initialWord
    } else {
      val tfv = _uiState.value.textFieldValue
      WordNormalizer.extractWordAtCursor(tfv.text, tfv.selection.start, tfv.selection.end)
    }

    _uiState.update {
      it.copy(
        showRhymeSheet = true,
        rhymeQueryWord = wordToLookup
      )
    }

    if (wordToLookup.isNotBlank()) {
      searchRhymes(wordToLookup)
    }
  }

  fun closeRhymeAssistant() {
    rhymeJob?.cancel()
    _uiState.update {
      it.copy(
        showRhymeSheet = false,
        isRhymeSearching = false
      )
    }
  }

  fun searchRhymes(word: String) {
    rhymeJob?.cancel()
    val clean = WordNormalizer.normalize(word)
    _uiState.update {
      it.copy(
        rhymeQueryWord = word,
        isRhymeSearching = clean.isNotEmpty()
      )
    }

    if (clean.isEmpty()) {
      _uiState.update { it.copy(rhymeQueryResult = null, isRhymeSearching = false) }
      return
    }

    rhymeJob = viewModelScope.launch {
      val result = rhymeEngine.findRhymes(clean)
      _uiState.update {
        it.copy(
          rhymeQueryResult = result,
          isRhymeSearching = false
        )
      }
    }
  }

  fun insertRhymeAtCursor(rhymeWord: String) {
    insertTextAtCursor(rhymeWord)
  }

  private fun calculateWordCount(text: String): Int {
    if (text.isBlank()) return 0
    return text.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
  }

  private fun calculateLineCount(text: String): Int {
    if (text.isEmpty()) return 0
    return text.lines().size
  }

  private fun extractSections(text: String): List<SectionOutlineItem> {
    val regex = Regex("""\[([^\]]+)\]""")
    val lines = text.lines()
    val results = mutableListOf<SectionOutlineItem>()

    var currentLineIndex = 0
    for (match in regex.findAll(text)) {
      val title = match.groupValues[1]
      val startIndex = match.range.first

      // Calculate line number
      val precedingText = text.substring(0, startIndex)
      val lineNum = precedingText.count { it == '\n' } + 1

      results.add(SectionOutlineItem(title = title, startIndex = startIndex, lineNumber = lineNum))
    }
    return results
  }

  // --- Phase 4 Beat & Waveform Methods ---

  fun loadWaveform(filePath: String) {
    if (waveformAnalyzer == null) return
    waveformJob?.cancel()
    _uiState.update { it.copy(isWaveformLoading = true) }
    waveformJob = viewModelScope.launch(Dispatchers.Default) {
      try {
        val amplitudes = waveformAnalyzer.extractWaveform(filePath, targetBuckets = 120)
        _uiState.update { it.copy(waveformAmplitudes = amplitudes, isWaveformLoading = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isWaveformLoading = false) }
      }
    }
  }

  fun importOrReplaceBeat(uri: Uri) {
    val fileManager = audioFileManager ?: return
    val audioRepo = audioRepository ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(beatImportError = null) }
      val result = fileManager.importBeatFile(uri, songId)

      result.onSuccess { imported ->
        val currentTrack = _uiState.value.beatTrack
        if (currentTrack != null) {
          // If replacing, clean up previous file if different
          if (currentTrack.uri != imported.localPath) {
            fileManager.deleteBeatFile(currentTrack.uri)
          }
          val updated = currentTrack.copy(
            name = imported.fileName,
            uri = imported.localPath,
            duration = imported.durationMs
          )
          audioRepo.updateTrack(updated)
        } else {
          audioRepo.addTrack(
            songId = songId,
            name = imported.fileName,
            uri = imported.localPath,
            type = TrackType.BEAT,
            duration = imported.durationMs
          )
        }

        beatPlayer?.loadBeat(imported.localPath, initialVolume = 1.0f)
        loadWaveform(imported.localPath)
      }.onFailure { err ->
        _uiState.update { it.copy(beatImportError = err.message ?: "Failed to import audio file") }
      }
    }
  }

  fun dismissBeatImportError() {
    _uiState.update { it.copy(beatImportError = null) }
  }

  fun removeBeat() {
    val currentTrack = _uiState.value.beatTrack ?: return
    beatPlayer?.pause()
    audioFileManager?.deleteBeatFile(currentTrack.uri)
    viewModelScope.launch {
      audioRepository?.deleteTrack(currentTrack.id)
      _uiState.update {
        it.copy(
          beatTrack = null,
          waveformAmplitudes = null,
          isWaveformLoading = false
        )
      }
    }
  }

  fun playBeat() {
    beatPlayer?.play()
  }

  fun pauseBeat() {
    beatPlayer?.pause()
  }

  fun togglePlayPauseBeat() {
    beatPlayer?.togglePlayPause()
  }

  fun seekBeat(targetMs: Long) {
    beatPlayer?.seekTo(targetMs)
  }

  fun seekBeatRelative(deltaMs: Long) {
    beatPlayer?.seekRelative(deltaMs)
  }

  fun setBeatVolume(volume: Float) {
    beatPlayer?.setVolume(volume)
    val currentTrack = _uiState.value.beatTrack
    if (currentTrack != null && audioRepository != null) {
      viewModelScope.launch {
        audioRepository.updateTrack(currentTrack.copy(volume = volume.coerceIn(0f, 1f)))
      }
    }
  }

  fun toggleBeatMute() {
    beatPlayer?.toggleMute()
  }

  fun toggleBeatLoop() {
    beatPlayer?.toggleLoop()
  }

  override fun onCleared() {
    super.onCleared()
    waveformJob?.cancel()
    rhymeJob?.cancel()
    beatPlayer?.release()
  }

  companion object {
    val PRESET_SECTIONS = listOf(
      "Intro",
      "Verse",
      "Pre-Chorus",
      "Chorus",
      "Post-Chorus",
      "Bridge",
      "Hook",
      "Outro"
    )
  }
}
