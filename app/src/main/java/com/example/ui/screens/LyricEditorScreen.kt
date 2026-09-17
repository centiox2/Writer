package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.audio.AudioFileManager
import com.example.ui.components.BeatPlayerDock
import com.example.ui.components.RhymeAssistantSheet
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioOrange
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioRed
import com.example.ui.viewmodels.FontFamilyChoice
import com.example.ui.viewmodels.LyricEditorUiState
import com.example.ui.viewmodels.LyricEditorViewModel
import com.example.ui.viewmodels.SaveStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricEditorScreen(
  viewModel: LyricEditorViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToMixer: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val clipboardManager = LocalClipboardManager.current
  val scrollState = rememberScrollState()
  var showMenu by remember { mutableStateOf(false) }

  // Storage Access Framework audio file picker launcher
  val beatAudioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { viewModel.importOrReplaceBeat(it) }
  }

  // Intercept back navigation: flush draft changes before exiting
  BackHandler {
    viewModel.saveNow()
    onNavigateBack()
  }

  // Lifecycle observer: save immediately when app is backgrounded or leaves screen
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        viewModel.saveNow()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.saveNow()
    }
  }

  // Keep screen awake feature
  KeepScreenOnEffect(keepScreenOn = uiState.keepScreenOn)

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .imePadding(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = uiState.song?.title ?: "Lyrics",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              maxLines = 1,
              color = MaterialTheme.colorScheme.onSurface
            )
            // Save status indicator
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              when (uiState.saveStatus) {
                SaveStatus.SAVED -> {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Saved",
                    tint = StudioMint,
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "Saved",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = StudioMint
                  )
                }
                SaveStatus.SAVING -> {
                  CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = StudioBlue
                  )
                  Text(
                    text = "Saving...",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = StudioBlue
                  )
                }
                SaveStatus.UNSAVED -> {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .background(StudioOrange, CircleShape)
                  )
                  Text(
                    text = "Unsaved",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = StudioOrange
                  )
                }
              }

              Text(
                text = "• ${uiState.wordCount} words • ${uiState.lineCount} lines",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = {
              viewModel.saveNow()
              onNavigateBack()
            },
            modifier = Modifier.testTag("editor_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Save and go back"
            )
          }
        },
        actions = {
          // Undo button
          IconButton(
            onClick = { viewModel.undo() },
            enabled = uiState.canUndo,
            modifier = Modifier.testTag("editor_undo_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Undo,
              contentDescription = "Undo",
              tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
          }

          // Redo button
          IconButton(
            onClick = { viewModel.redo() },
            enabled = uiState.canRedo,
            modifier = Modifier.testTag("editor_redo_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Redo,
              contentDescription = "Redo",
              tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
          }

          // In-lyrics search toggle
          IconButton(
            onClick = { viewModel.toggleSearch() },
            modifier = Modifier.testTag("editor_search_toggle")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search in lyrics",
              tint = if (uiState.isSearchActive) StudioBlue else MaterialTheme.colorScheme.onSurface
            )
          }

          // Multi-Track Mixer Button
          IconButton(
            onClick = {
              viewModel.saveNow()
              onNavigateToMixer(uiState.song?.id ?: "")
            },
            modifier = Modifier.testTag("editor_open_mixer_button")
          ) {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = "Multi-Track Mixer",
              tint = StudioMint
            )
          }

          // Rhyme Assistant Button
          IconButton(
            onClick = { viewModel.openRhymeAssistant() },
            modifier = Modifier.testTag("editor_rhyme_assistant_button")
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = "Offline Rhyme Assistant",
              tint = StudioPurple
            )
          }

          // Keep screen on toggle
          IconButton(
            onClick = { viewModel.toggleKeepScreenOn() },
            modifier = Modifier.testTag("editor_keep_screen_on_button")
          ) {
            Icon(
              imageVector = Icons.Default.Lightbulb,
              contentDescription = if (uiState.keepScreenOn) "Screen kept awake" else "Keep screen awake",
              tint = if (uiState.keepScreenOn) StudioOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
          }

          // More Options Menu
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.testTag("editor_more_options")
            ) {
              Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More editor options")
            }

            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Formatting & Font") },
                leadingIcon = { Icon(Icons.Default.FormatSize, contentDescription = null) },
                onClick = {
                  showMenu = false
                  viewModel.setShowFormatSheet(true)
                }
              )

              if (uiState.detectedSections.isNotEmpty()) {
                DropdownMenuItem(
                  text = { Text("Jump to Section (${uiState.detectedSections.size})") },
                  leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                  onClick = {
                    showMenu = false
                    viewModel.setShowSectionJumpSheet(true)
                  }
                )
              }

              DropdownMenuItem(
                text = { Text("Offline Rhyme Assistant") },
                leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = StudioPurple) },
                onClick = {
                  showMenu = false
                  viewModel.openRhymeAssistant()
                }
              )

              DropdownMenuItem(
                text = { Text("Multi-Track Mixer") },
                leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioBlue) },
                onClick = {
                  showMenu = false
                  viewModel.saveNow()
                  onNavigateToMixer(uiState.song?.id ?: "")
                }
              )

              DropdownMenuItem(
                text = { Text(if (uiState.beatTrack == null) "Import Instrumental Beat" else "Replace Instrumental Beat") },
                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = StudioMint) },
                onClick = {
                  showMenu = false
                  beatAudioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES)
                }
              )

              DropdownMenuItem(
                text = { Text("Lyric Stats & Details") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                onClick = {
                  showMenu = false
                  viewModel.setShowStatsDialog(true)
                }
              )

              DropdownMenuItem(
                text = { Text("Clear All Lyrics", color = StudioRed) },
                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, tint = StudioRed) },
                onClick = {
                  showMenu = false
                  viewModel.setShowClearConfirmDialog(true)
                }
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        )
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // In-Lyrics Search Bar (Animated visibility)
      AnimatedVisibility(
        visible = uiState.isSearchActive,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        SearchInLyricsBar(
          searchQuery = uiState.searchQuery,
          matchCount = uiState.searchMatchCount,
          currentIndex = uiState.currentMatchIndex,
          onQueryChange = { viewModel.onSearchQueryChanged(it) },
          onNextMatch = { viewModel.findNextMatch() },
          onPrevMatch = { viewModel.findPreviousMatch() },
          onCloseSearch = { viewModel.toggleSearch() }
        )
      }

      // Quick Section Insertion Bar: [Intro] [Verse] [Pre-Chorus] [Chorus] [Post-Chorus] [Bridge] [Hook] [Outro]
      LyricSectionChipsBar(
        onInsertSection = { section ->
          viewModel.insertSection(section)
        },
        onOpenOutline = {
          if (uiState.detectedSections.isNotEmpty()) {
            viewModel.setShowSectionJumpSheet(true)
          }
        },
        hasSections = uiState.detectedSections.isNotEmpty()
      )

      // Quick Editing Actions (Selection, Copy, Cut, Paste)
      EditingRibbonBar(
        hasSelection = uiState.textFieldValue.selection.length > 0,
        onCopy = {
          val sel = uiState.textFieldValue.selection
          val textToCopy = if (sel.length > 0) {
            uiState.textFieldValue.text.substring(sel.min, sel.max)
          } else {
            uiState.textFieldValue.text
          }
          if (textToCopy.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(textToCopy))
          }
        },
        onCut = {
          val cutText = viewModel.cutSelectedText()
          if (cutText.isNotEmpty()) {
            clipboardManager.setText(AnnotatedString(cutText))
          }
        },
        onPaste = {
          val clipboardText = clipboardManager.getText()?.text
          if (!clipboardText.isNullOrEmpty()) {
            viewModel.insertTextAtCursor(clipboardText)
          }
        },
        onSelectAll = { viewModel.selectAll() },
        onClearSelection = { viewModel.clearSelection() },
        onOpenFormat = { viewModel.setShowFormatSheet(true) },
        onRhyme = { viewModel.openRhymeAssistant() }
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // Native Editable Text Area
      val fontFamily = when (uiState.fontFamilyChoice) {
        FontFamilyChoice.SANS_SERIF -> FontFamily.SansSerif
        FontFamilyChoice.SERIF -> FontFamily.Serif
        FontFamilyChoice.MONOSPACE -> FontFamily.Monospace
      }

      val textStyle = TextStyle(
        fontSize = uiState.fontSizeSp.sp,
        lineHeight = (uiState.fontSizeSp * uiState.lineSpacingMultiplier).sp,
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onBackground
      )

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(scrollState)
          .padding(horizontal = 16.dp, vertical = 16.dp)
      ) {
        BasicTextField(
          value = uiState.textFieldValue,
          onValueChange = { viewModel.onTextChanged(it) },
          textStyle = textStyle,
          cursorBrush = SolidColor(StudioBlue),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("lyric_text_field"),
          decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
              if (uiState.textFieldValue.text.isEmpty()) {
                Text(
                  text = "Start writing your song lyrics here...\n\nTap any section tag above like [Verse] or [Chorus] to structure your song.\nChanges save automatically.",
                  style = textStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  )
                )
              }
              innerTextField()
            }
          }
        )
      }

      // Sticky Audio Beat Transport Dock (Beat plays continuously while user writes)
      BeatPlayerDock(
        beatTrack = uiState.beatTrack,
        playbackState = uiState.beatPlaybackState,
        waveformAmplitudes = uiState.waveformAmplitudes,
        isWaveformLoading = uiState.isWaveformLoading,
        onImportBeat = { beatAudioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES) },
        onReplaceBeat = { beatAudioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES) },
        onRemoveBeat = { viewModel.removeBeat() },
        onPlay = { viewModel.playBeat() },
        onPause = { viewModel.pauseBeat() },
        onSeek = { viewModel.seekBeat(it) },
        onSeekRelative = { viewModel.seekBeatRelative(it) },
        onVolumeChange = { viewModel.setBeatVolume(it) },
        onToggleMute = { viewModel.toggleBeatMute() },
        onToggleLoop = { viewModel.toggleBeatLoop() }
      )
    }
  }

  // Audio Import Error Dialog
  uiState.beatImportError?.let { errorMsg ->
    AlertDialog(
      onDismissRequest = { /* Dismissal handled via retry or cancel */ },
      title = { Text("Audio Import Error") },
      text = { Text(errorMsg) },
      confirmButton = {
        TextButton(onClick = { beatAudioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES) }) {
          Text("Try Again")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissBeatImportError() }) {
          Text("Dismiss")
        }
      }
    )
  }

  // Formatting & Font Customization Bottom Sheet
  if (uiState.showFormatSheet) {
    FormattingBottomSheet(
      uiState = uiState,
      onFontSizeChange = { viewModel.setFontSize(it) },
      onLineSpacingChange = { viewModel.setLineSpacing(it) },
      onFontFamilyChange = { viewModel.setFontFamily(it) },
      onDismiss = { viewModel.setShowFormatSheet(false) }
    )
  }

  // Jump to Section Sheet
  if (uiState.showSectionJumpSheet) {
    SectionJumpBottomSheet(
      sections = uiState.detectedSections,
      onSelectSection = { item ->
        viewModel.jumpToSection(item.startIndex)
      },
      onDismiss = { viewModel.setShowSectionJumpSheet(false) }
    )
  }

  // Stats Dialog
  if (uiState.showStatsDialog) {
    LyricStatsDialog(
      uiState = uiState,
      onDismiss = { viewModel.setShowStatsDialog(false) }
    )
  }

  // Rhyme Assistant Sheet (Phase 5)
  if (uiState.showRhymeSheet) {
    RhymeAssistantSheet(
      queryWord = uiState.rhymeQueryWord,
      result = uiState.rhymeQueryResult,
      isSearching = uiState.isRhymeSearching,
      onSearchWord = { viewModel.searchRhymes(it) },
      onInsertWordAtCursor = { rhymeWord ->
        viewModel.insertRhymeAtCursor(rhymeWord)
      },
      onCopyWord = { rhymeWord ->
        clipboardManager.setText(AnnotatedString(rhymeWord))
      },
      onDismiss = { viewModel.closeRhymeAssistant() }
    )
  }

  // Clear confirmation dialog
  if (uiState.showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.setShowClearConfirmDialog(false) },
      title = { Text("Clear All Lyrics?") },
      text = { Text("Are you sure you want to clear the entire lyrics document? You can undo this action immediately after if needed.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.clearLyrics()
            viewModel.setShowClearConfirmDialog(false)
          },
          modifier = Modifier.testTag("confirm_clear_lyrics_button")
        ) {
          Text("Clear", color = StudioRed, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.setShowClearConfirmDialog(false) }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun SearchInLyricsBar(
  searchQuery: String,
  matchCount: Int,
  currentIndex: Int,
  onQueryChange: (String) -> Unit,
  onNextMatch: () -> Unit,
  onPrevMatch: () -> Unit,
  onCloseSearch: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    tonalElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        placeholder = { Text("Find in lyrics...", fontSize = 14.sp) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onQueryChange("") },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
            }
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = StudioBlue
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("in_lyrics_search_input")
      )

      val matchText = if (searchQuery.isBlank()) {
        ""
      } else if (matchCount > 0) {
        "${currentIndex + 1}/$matchCount"
      } else {
        "0/0"
      }

      if (matchText.isNotEmpty()) {
        Text(
          text = matchText,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = if (matchCount > 0) StudioBlue else StudioRed
        )
      }

      IconButton(
        onClick = onPrevMatch,
        enabled = matchCount > 0,
        modifier = Modifier
          .size(36.dp)
          .testTag("search_prev_button")
      ) {
        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
      }

      IconButton(
        onClick = onNextMatch,
        enabled = matchCount > 0,
        modifier = Modifier
          .size(36.dp)
          .testTag("search_next_button")
      ) {
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
      }

      IconButton(
        onClick = onCloseSearch,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(Icons.Default.Close, contentDescription = "Close search")
      }
    }
  }
}

@Composable
fun LyricSectionChipsBar(
  onInsertSection: (String) -> Unit,
  onOpenOutline: () -> Unit,
  hasSections: Boolean,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (hasSections) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = StudioPurple.copy(alpha = 0.15f),
        onClick = onOpenOutline,
        modifier = Modifier.testTag("open_section_outline_chip")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = "Sections Outline",
            tint = StudioPurple,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Outline",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = StudioPurple
          )
        }
      }
    }

    LyricEditorViewModel.PRESET_SECTIONS.forEach { section ->
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { onInsertSection(section) },
        modifier = Modifier.testTag("section_chip_$section")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = StudioBlue,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = "[$section]",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
fun EditingRibbonBar(
  hasSelection: Boolean,
  onCopy: () -> Unit,
  onCut: () -> Unit,
  onPaste: () -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onOpenFormat: () -> Unit,
  onRhyme: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Rhymes helper (prominent in ribbon)
    QuickActionButton(
      icon = Icons.Default.Psychology,
      label = if (hasSelection) "Rhyme Selection" else "Rhymes",
      onClick = onRhyme,
      tag = "ribbon_rhymes"
    )

    // Copy
    QuickActionButton(
      icon = Icons.Default.ContentCopy,
      label = if (hasSelection) "Copy Selection" else "Copy All",
      onClick = onCopy,
      tag = "ribbon_copy"
    )

    // Cut
    if (hasSelection) {
      QuickActionButton(
        icon = Icons.Default.ContentCut,
        label = "Cut",
        onClick = onCut,
        tag = "ribbon_cut"
      )
    }

    // Paste
    QuickActionButton(
      icon = Icons.Default.ContentPaste,
      label = "Paste",
      onClick = onPaste,
      tag = "ribbon_paste"
    )

    // Select All
    QuickActionButton(
      icon = Icons.Default.SelectAll,
      label = "Select All",
      onClick = onSelectAll,
      tag = "ribbon_select_all"
    )

    // Clear Selection
    if (hasSelection) {
      QuickActionButton(
        icon = Icons.Default.Close,
        label = "Deselect",
        onClick = onClearSelection,
        tag = "ribbon_deselect"
      )
    }

    // Font format
    QuickActionButton(
      icon = Icons.Default.FormatSize,
      label = "Font & Spacing",
      onClick = onOpenFormat,
      tag = "ribbon_format"
    )
  }
}

@Composable
fun QuickActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  tag: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    onClick = onClick,
    modifier = modifier.testTag(tag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier.size(14.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormattingBottomSheet(
  uiState: LyricEditorUiState,
  onFontSizeChange: (Float) -> Unit,
  onLineSpacingChange: (Float) -> Unit,
  onFontFamilyChange: (FontFamilyChoice) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Text(
        text = "Lyric Typography & Styling",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      // Font Size Section
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Font Size",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
          )
          Text(
            text = "${uiState.fontSizeSp.toInt()} sp",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = StudioBlue
          )
        }

        Slider(
          value = uiState.fontSizeSp,
          onValueChange = onFontSizeChange,
          valueRange = 14f..30f,
          steps = 7,
          colors = SliderDefaults.colors(
            thumbColor = StudioBlue,
            activeTrackColor = StudioBlue
          ),
          modifier = Modifier.testTag("font_size_slider")
        )

        // Preset chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          listOf(14f to "Compact", 18f to "Standard", 22f to "Large", 28f to "Giant").forEach { (size, label) ->
            FilterChip(
              selected = uiState.fontSizeSp == size,
              onClick = { onFontSizeChange(size) },
              label = { Text(label, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioBlue.copy(alpha = 0.15f),
                selectedLabelColor = StudioBlue
              )
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Line Spacing Section
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Line Spacing",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
          )
          val spacingLabel = when (uiState.lineSpacingMultiplier) {
            1.2f -> "Compact (1.2x)"
            1.5f -> "Normal (1.5x)"
            1.8f -> "Relaxed (1.8x)"
            2.2f -> "Double (2.2x)"
            else -> String.format("%.1fx", uiState.lineSpacingMultiplier)
          }
          Text(
            text = spacingLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = StudioBlue
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            1.2f to "1.2x",
            1.5f to "1.5x",
            1.8f to "1.8x",
            2.2f to "2.2x"
          ).forEach { (mult, lbl) ->
            FilterChip(
              selected = kotlin.math.abs(uiState.lineSpacingMultiplier - mult) < 0.05f,
              onClick = { onLineSpacingChange(mult) },
              label = { Text(lbl, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioBlue.copy(alpha = 0.15f),
                selectedLabelColor = StudioBlue
              ),
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      // Font Family
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Typography Style",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = uiState.fontFamilyChoice == FontFamilyChoice.SANS_SERIF,
            onClick = { onFontFamilyChange(FontFamilyChoice.SANS_SERIF) },
            label = { Text("Modern Sans", fontFamily = FontFamily.SansSerif) },
            modifier = Modifier.weight(1f)
          )
          FilterChip(
            selected = uiState.fontFamilyChoice == FontFamilyChoice.SERIF,
            onClick = { onFontFamilyChange(FontFamilyChoice.SERIF) },
            label = { Text("Poetic Serif", fontFamily = FontFamily.Serif) },
            modifier = Modifier.weight(1f)
          )
          FilterChip(
            selected = uiState.fontFamilyChoice == FontFamilyChoice.MONOSPACE,
            onClick = { onFontFamilyChange(FontFamilyChoice.MONOSPACE) },
            label = { Text("Grid Mono", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionJumpBottomSheet(
  sections: List<com.example.ui.viewmodels.SectionOutlineItem>,
  onSelectSection: (com.example.ui.viewmodels.SectionOutlineItem) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      Text(
        text = "Song Structure Outline",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Tap any section to jump the editor directly to it",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        items(sections) { item ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            onClick = {
              onSelectSection(item)
              onDismiss()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("jump_to_${item.title}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "[${item.title}]",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = StudioBlue
              )
              Text(
                text = "Line ${item.lineNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun LyricStatsDialog(
  uiState: LyricEditorUiState,
  onDismiss: () -> Unit
) {
  val words = uiState.wordCount
  val chars = uiState.charCount
  val lines = uiState.lineCount
  val charsNoSpaces = uiState.textFieldValue.text.count { !it.isWhitespace() }
  // Average speaking / singing speed ~ 130 words per minute
  val estimatedMinutes = if (words > 0) String.format("%.1f", words / 130f) else "0.0"

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = StudioBlue)
        Text("Lyric Statistics")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatRow(label = "Word Count", value = "$words")
        StatRow(label = "Total Characters", value = "$chars")
        StatRow(label = "Characters (no spaces)", value = "$charsNoSpaces")
        StatRow(label = "Total Lines", value = "$lines")
        StatRow(label = "Detected Sections", value = "${uiState.detectedSections.size}")
        StatRow(label = "Est. Reading / Vocal Time", value = "$estimatedMinutes min")
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun StatRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
fun KeepScreenOnEffect(keepScreenOn: Boolean) {
  val context = LocalContext.current
  DisposableEffect(keepScreenOn) {
    val activity = context.findActivity()
    if (keepScreenOn) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
      activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
