package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RecordingState
import com.example.domain.models.Recording
import com.example.domain.models.Song
import com.example.domain.models.TrackType
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioRed
import com.example.ui.viewmodels.RecordingItemUiModel
import com.example.ui.viewmodels.RecordingsUiState
import com.example.ui.viewmodels.RecordingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
  viewModel: RecordingsViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.message, uiState.errorMessage) {
    uiState.message?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessage()
    }
    uiState.errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearMessage()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Recordings & Ideas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "${uiState.recordings.size} takes across projects (unlimited)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        FilledTonalButton(
          onClick = { viewModel.openNewTakeSheet() },
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = StudioRed.copy(alpha = 0.15f),
            contentColor = StudioRed
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("record_idea_top_button")
        ) {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("New Take", fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Search Field
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.setSearchQuery(it) },
        placeholder = { Text("Search takes and voice ideas...") },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = { viewModel.setSearchQuery("") }) {
              Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("recordings_search_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Song Filter Chips
      if (uiState.songs.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = uiState.selectedSongFilterId == null,
            onClick = { viewModel.setSongFilter(null) },
            label = { Text("All Takes (${uiState.recordings.size})") },
            shape = RoundedCornerShape(20.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioBlue,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("filter_all_songs")
          )

          uiState.songs.forEach { song ->
            FilterChip(
              selected = uiState.selectedSongFilterId == song.id,
              onClick = { viewModel.setSongFilter(song.id) },
              label = { Text(song.title) },
              shape = RoundedCornerShape(20.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioBlue,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("filter_song_${song.id}")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
      }

      // Takes List / Empty State
      if (uiState.recordings.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(StudioRed.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = StudioRed,
                modifier = Modifier.size(36.dp)
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = if (uiState.searchQuery.isNotBlank()) "No matching takes found" else "No audio takes recorded yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Capture vocal melodies, freestyle ideas, and take iterations with real waveforms and instant mixer routing",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = { viewModel.openNewTakeSheet() },
              colors = ButtonDefaults.buttonColors(containerColor = StudioRed),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("record_first_take_button")
            ) {
              Icon(imageVector = Icons.Default.Mic, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Record First Take")
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("recordings_list"),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(uiState.recordings, key = { it.recording.id }) { item ->
            RecordingTakeCard(
              item = item,
              onTogglePlay = { viewModel.togglePlayTake(item.recording) },
              onSeek = { fraction -> viewModel.seekTake(item.recording, fraction) },
              onToggleMute = { viewModel.toggleMute(item.recording.id) },
              onToggleSolo = { viewModel.toggleSolo(item.recording.id) },
              onRename = { viewModel.promptRename(item.recording) },
              onDuplicate = { viewModel.duplicateRecording(item.recording) },
              onAttachToMixer = { viewModel.promptAttachToMixer(item.recording) },
              onDelete = { viewModel.promptDelete(item.recording) }
            )
          }

          item {
            Spacer(modifier = Modifier.height(88.dp))
          }
        }
      }
    }

    // Snackbar Host
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
    )
  }

  // --- Dialogs ---

  // Rename Dialog
  uiState.showRenameDialog?.let { recording ->
    var newName by remember(recording) { mutableStateOf(recording.name) }
    AlertDialog(
      onDismissRequest = { viewModel.dismissRenameDialog() },
      title = { Text("Rename Take") },
      text = {
        Column {
          Text(
            text = "Enter a descriptive name for this take:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("rename_take_input")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { viewModel.renameRecording(recording.id, newName) },
          enabled = newName.isNotBlank(),
          modifier = Modifier.testTag("rename_take_confirm")
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissRenameDialog() }) {
          Text("Cancel")
        }
      }
    )
  }

  // Delete Dialog
  uiState.showDeleteDialog?.let { recording ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissDeleteDialog() },
      icon = {
        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StudioRed)
      },
      title = { Text("Delete Take?") },
      text = {
        Text("Are you sure you want to permanently delete \"${recording.name}\"? This will also remove the recorded audio file from storage.")
      },
      confirmButton = {
        Button(
          onClick = { viewModel.deleteRecording(recording) },
          colors = ButtonDefaults.buttonColors(containerColor = StudioRed),
          modifier = Modifier.testTag("delete_take_confirm")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
          Text("Cancel")
        }
      }
    )
  }

  // Attach to Mixer Dialog
  uiState.showAttachToMixerDialog?.let { recording ->
    var selectedSongId by remember(recording) {
      mutableStateOf(recording.songId.ifEmpty { uiState.songs.firstOrNull()?.id ?: "" })
    }
    var selectedType by remember { mutableStateOf(TrackType.RECORDING) }

    AlertDialog(
      onDismissRequest = { viewModel.dismissAttachToMixerDialog() },
      icon = {
        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = StudioBlue)
      },
      title = { Text("Attach to Mixer") },
      text = {
        Column {
          Text(
            text = "Convert take \"${recording.name}\" into a mixer channel strip for multi-track mixing:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Target Project Song:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))

          uiState.songs.forEach { song ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedSongId = song.id }
                .padding(vertical = 4.dp)
            ) {
              RadioButton(
                selected = selectedSongId == song.id,
                onClick = { selectedSongId = song.id }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(song.title, style = MaterialTheme.typography.bodyMedium)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Track Role:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = selectedType == TrackType.VOCAL,
              onClick = { selectedType = TrackType.VOCAL },
              label = { Text("Lead Vocal") }
            )
            FilterChip(
              selected = selectedType == TrackType.RECORDING,
              onClick = { selectedType = TrackType.RECORDING },
              label = { Text("Take / Ad-lib") }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.attachRecordingToMixer(
              recording = recording,
              targetSongId = selectedSongId,
              trackType = selectedType
            )
          },
          enabled = selectedSongId.isNotBlank(),
          modifier = Modifier.testTag("attach_to_mixer_confirm")
        ) {
          Text("Attach Track")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissAttachToMixerDialog() }) {
          Text("Cancel")
        }
      }
    )
  }

  // New Take Recording Bottom Sheet
  if (uiState.showNewTakeSheet) {
    NewTakeBottomSheet(
      uiState = uiState,
      onStart = { targetSongId -> viewModel.startRecordingNewTake(targetSongId) },
      onPause = { viewModel.pauseRecordingTake() },
      onResume = { viewModel.resumeRecordingTake() },
      onStopAndSave = { customName, targetSongId ->
        viewModel.stopAndSaveRecordingTake(customName, targetSongId)
      },
      onCancel = { viewModel.cancelActiveRecording() },
      onDismiss = { viewModel.dismissNewTakeSheet() }
    )
  }
}

/**
 * Rich Material 3 Card displaying a single recording take with real interactive waveform,
 * play/pause scrubbing, mute, solo, and studio options.
 */
@Composable
fun RecordingTakeCard(
  item: RecordingItemUiModel,
  onTogglePlay: () -> Unit,
  onSeek: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleSolo: () -> Unit,
  onRename: () -> Unit,
  onDuplicate: () -> Unit,
  onAttachToMixer: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val recording = item.recording
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
  val formattedDate = remember(recording.createdAt) { dateFormat.format(Date(recording.createdAt)) }

  val totalDurationMs = if (recording.duration > 0) recording.duration else 1000L
  val currentPosMs = if (item.isPlaying) item.currentPositionMs else 0L
  val progress = (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

  val durationFormatted = remember(recording.duration) {
    val totalSeconds = recording.duration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }

  val currentPosFormatted = remember(currentPosMs) {
    val totalSeconds = currentPosMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }

  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(
        width = if (item.isSolo) 1.5.dp else if (item.isPlaying) 1.dp else 0.5.dp,
        color = if (item.isSolo) StudioAmber else if (item.isPlaying) StudioMint.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
      )
      .testTag("recording_card_${recording.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Top Row: Title, Song Badge, Options
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (item.isPlaying) StudioMint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (item.isPlaying) Icons.Default.GraphicEq else Icons.Default.Mic,
              contentDescription = null,
              tint = if (item.isPlaying) StudioMint else StudioRed,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = recording.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Surface(
                color = StudioBlue.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = item.songTitle ?: "Standalone",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioBlue,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.testTag("take_options_${recording.id}")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Rename Take") },
              leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
              onClick = {
                showMenu = false
                onRename()
              }
            )
            DropdownMenuItem(
              text = { Text("Duplicate Take") },
              leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
              onClick = {
                showMenu = false
                onDuplicate()
              }
            )
            DropdownMenuItem(
              text = { Text("Attach to Mixer") },
              leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = StudioBlue) },
              onClick = {
                showMenu = false
                onAttachToMixer()
              }
            )
            HorizontalDivider()
            DropdownMenuItem(
              text = { Text("Delete Take", color = StudioRed) },
              leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed) },
              onClick = {
                showMenu = false
                onDelete()
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Real Interactive Waveform Visualizer
      WaveformVisualizer(
        amplitudes = item.waveform,
        progress = progress,
        isLoading = item.isWaveformLoading,
        onSeek = onSeek,
        height = 48.dp,
        activeColor = if (item.isMuted) MaterialTheme.colorScheme.outline else StudioMint,
        inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
        playheadColor = if (item.isSolo) StudioAmber else StudioBlue,
        modifier = Modifier.testTag("waveform_${recording.id}")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom Controls Row: Play/Pause, Time Position, Mute, Solo, Attach
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Playback controls & Timers
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilledTonalButton(
            onClick = onTogglePlay,
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = if (item.isPlaying) StudioMint else StudioMint.copy(alpha = 0.15f),
              contentColor = if (item.isPlaying) Color.Black else StudioMint
            ),
            modifier = Modifier
              .size(42.dp)
              .testTag("play_pause_button_${recording.id}"),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = if (item.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (item.isPlaying) "Pause" else "Play",
              modifier = Modifier.size(22.dp)
            )
          }

          Text(
            text = if (item.isPlaying) "$currentPosFormatted / $durationFormatted" else durationFormatted,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = if (item.isPlaying) StudioMint else MaterialTheme.colorScheme.onSurface
          )
        }

        // Mute, Solo & Attach Action Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Mute Button
          Surface(
            checked = item.isMuted,
            onCheckedChange = { onToggleMute() },
            shape = RoundedCornerShape(8.dp),
            color = if (item.isMuted) StudioRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .size(34.dp)
              .testTag("mute_take_${recording.id}")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "M",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isMuted) StudioRed else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Solo Button
          Surface(
            checked = item.isSolo,
            onCheckedChange = { onToggleSolo() },
            shape = RoundedCornerShape(8.dp),
            color = if (item.isSolo) StudioAmber.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .size(34.dp)
              .testTag("solo_take_${recording.id}")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "S",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isSolo) StudioAmber else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Quick Attach to Mixer Button
          IconButton(
            onClick = onAttachToMixer,
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(StudioBlue.copy(alpha = 0.12f))
              .testTag("attach_mixer_button_${recording.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Attach to Mixer",
              tint = StudioBlue,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Bottom Sheet for recording unlimited new takes and voice memos with live amplitude wave visualizer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTakeBottomSheet(
  uiState: RecordingsUiState,
  onStart: (String?) -> Unit,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onStopAndSave: (String?, String?) -> Unit,
  onCancel: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var takeName by remember { mutableStateOf("") }
  var selectedSongId by remember(uiState.targetSongForNewRecording) {
    mutableStateOf(uiState.targetSongForNewRecording ?: uiState.songs.firstOrNull()?.id ?: "")
  }

  val durationFormatted = remember(uiState.recordingDurationMs) {
    val totalSeconds = uiState.recordingDurationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (uiState.recordingDurationMs % 1000) / 100
    String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
  }

  val isRecording = uiState.recorderState == RecordingState.RECORDING
  val isPaused = uiState.recorderState == RecordingState.PAUSED
  val isIdle = uiState.recorderState == RecordingState.IDLE

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("new_take_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = if (isRecording) "Recording Voice Take..." else if (isPaused) "Recording Paused" else "Voice Take & Idea Recorder",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = if (isRecording) StudioRed else MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Live Timer Display
      Text(
        text = durationFormatted,
        style = MaterialTheme.typography.displayMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = if (isRecording) StudioRed else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("recorder_timer_text")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Live Dynamic Amplitude Waveform Bar
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (uiState.liveAmplitudesHistory.isEmpty()) {
            Text(
              text = if (isIdle) "Ready to capture ideas" else "Listening...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            uiState.liveAmplitudesHistory.takeLast(40).forEach { amp ->
              val barHeight = (44.dp * amp.coerceIn(0.1f, 1f)).coerceAtLeast(6.dp)
              Box(
                modifier = Modifier
                  .width(4.dp)
                  .height(barHeight)
                  .clip(RoundedCornerShape(2.dp))
                  .background(if (isRecording) StudioRed else StudioMint)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Take Name (Optional)
      OutlinedTextField(
        value = takeName,
        onValueChange = { takeName = it },
        label = { Text("Take Name (e.g. Chorus Hook, Verse 1 Idea)") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("new_take_name_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Target Song Picker
      if (uiState.songs.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Link to:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          uiState.songs.forEach { song ->
            FilterChip(
              selected = selectedSongId == song.id,
              onClick = { selectedSongId = song.id },
              label = { Text(song.title) },
              shape = RoundedCornerShape(16.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioBlue,
                selectedLabelColor = Color.White
              )
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      // Record / Pause / Stop / Cancel Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isIdle) {
          Button(
            onClick = { onStart(selectedSongId) },
            colors = ButtonDefaults.buttonColors(containerColor = StudioRed),
            shape = CircleShape,
            modifier = Modifier
              .size(68.dp)
              .testTag("start_recording_button"),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Start Recording",
              modifier = Modifier.size(32.dp)
            )
          }
        } else {
          // Cancel Button
          OutlinedButton(
            onClick = onCancel,
            shape = CircleShape,
            modifier = Modifier.size(54.dp),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cancel",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Pause / Resume Button
          FilledTonalButton(
            onClick = { if (isRecording) onPause() else onResume() },
            shape = CircleShape,
            modifier = Modifier.size(54.dp),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isRecording) "Pause" else "Resume"
            )
          }

          // Stop & Save Button
          Button(
            onClick = { onStopAndSave(takeName, selectedSongId) },
            colors = ButtonDefaults.buttonColors(containerColor = StudioMint),
            shape = CircleShape,
            modifier = Modifier
              .size(68.dp)
              .testTag("stop_save_recording_button"),
            contentPadding = PaddingValues(0.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = "Save Take",
              tint = Color.Black,
              modifier = Modifier.size(32.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
