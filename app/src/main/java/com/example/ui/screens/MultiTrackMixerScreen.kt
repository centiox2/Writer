package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioFileManager
import com.example.audio.mixer.MixerPlaybackState
import com.example.domain.models.AudioTrack
import com.example.domain.models.TrackType
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioOrange
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioRed
import com.example.ui.viewmodels.MixerTrackUiModel
import com.example.ui.viewmodels.MultiTrackMixerUiState
import com.example.ui.viewmodels.MultiTrackMixerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiTrackMixerScreen(
  viewModel: MultiTrackMixerViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  var showAddTrackMenu by remember { mutableStateOf(false) }

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      viewModel.handleImportedAudio(uri)
    }
  }

  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("audio/wav")
  ) { destinationUri ->
    if (destinationUri != null) {
      viewModel.exportToSafDestination(destinationUri)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Multi-Track Mixer",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            uiState.song?.let { song ->
              Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("mixer_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Lyric Editor"
            )
          }
        },
        actions = {
          // Export WAV Button
          FilledTonalButton(
            onClick = { viewModel.openExportSheet() },
            modifier = Modifier
              .padding(end = 6.dp)
              .testTag("export_mix_button"),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = StudioMint.copy(alpha = 0.2f),
              contentColor = StudioMint
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export WAV", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
          }

          Box {
            FilledTonalButton(
              onClick = { showAddTrackMenu = true },
              modifier = Modifier
                .padding(end = 8.dp)
                .testTag("add_track_button"),
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = StudioBlue.copy(alpha = 0.2f),
                contentColor = StudioBlue
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Track", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }

            DropdownMenu(
              expanded = showAddTrackMenu,
              onDismissRequest = { showAddTrackMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Beat Track") },
                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = StudioMint) },
                onClick = {
                  showAddTrackMenu = false
                  viewModel.startImport(TrackType.BEAT)
                  audioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES)
                }
              )
              DropdownMenuItem(
                text = { Text("Vocal Track") },
                leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = StudioRed) },
                onClick = {
                  showAddTrackMenu = false
                  viewModel.startImport(TrackType.VOCAL)
                  audioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES)
                }
              )
              DropdownMenuItem(
                text = { Text("Recording Take") },
                leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = StudioPurple) },
                onClick = {
                  showAddTrackMenu = false
                  viewModel.startImport(TrackType.RECORDING)
                  audioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES)
                }
              )
              DropdownMenuItem(
                text = { Text("Imported Audio (Stem / FX)") },
                leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, tint = StudioAmber) },
                onClick = {
                  showAddTrackMenu = false
                  viewModel.startImport(TrackType.IMPORTED_AUDIO)
                  audioPickerLauncher.launch(AudioFileManager.SUPPORTED_MIME_TYPES)
                }
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      // Shared Timeline Master Transport Bar & Master Gain Control
      MasterTransportBar(
        playbackState = uiState.playbackState,
        onPlay = { viewModel.play() },
        onPause = { viewModel.pause() },
        onStop = { viewModel.stop() },
        onSeek = { viewModel.seekTo(it) },
        onMasterVolumeChange = { viewModel.setMasterVolume(it) }
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (uiState.tracks.isEmpty()) {
        EmptyMixerState(onAddTrack = { showAddTrackMenu = true })
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("mixer_track_list"),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(uiState.tracks, key = { it.track.id }) { trackModel ->
            TrackChannelCard(
              trackModel = trackModel,
              playbackPositionMs = uiState.playbackState.currentPositionMs,
              totalDurationMs = uiState.playbackState.durationMs,
              isAnyTrackSolo = uiState.playbackState.soloActive,
              onVolumeChange = { viewModel.setTrackVolume(trackModel.track.id, it) },
              onToggleMute = { viewModel.toggleTrackMute(trackModel.track.id) },
              onToggleSolo = { viewModel.toggleTrackSolo(trackModel.track.id) },
              onDelete = { viewModel.confirmDeleteTrack(trackModel.track) }
            )
          }
        }
      }

      // Importing Progress Overlay
      if (uiState.isImportingAudio) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
          ) {
            Row(
              modifier = Modifier.padding(24.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp), color = StudioBlue)
              Spacer(modifier = Modifier.width(16.dp))
              Text("Importing audio track...", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }

  // Delete Track Confirmation Dialog
  uiState.showDeleteConfirmDialog?.let { trackToDelete ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissDeleteDialog() },
      title = { Text("Delete Track?") },
      text = { Text("Are you sure you want to remove \"${trackToDelete.name}\" from the mixer project?") },
      confirmButton = {
        TextButton(
          onClick = { viewModel.deleteTrack(trackToDelete.id) },
          colors = ButtonDefaults.textButtonColors(contentColor = StudioRed),
          modifier = Modifier.testTag("confirm_delete_track_button")
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
          Text("Cancel")
        }
      }
    )
  }

  // Error Dialog
  uiState.errorMessage?.let { errorMsg ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissErrorMessage() },
      title = { Text("Mixer Notice") },
      text = { Text(errorMsg) },
      confirmButton = {
        TextButton(onClick = { viewModel.dismissErrorMessage() }) {
          Text("OK")
        }
      }
    )
  }

  // WAV Mixdown Export Bottom Sheet
  if (uiState.showExportSheet) {
    WavExportBottomSheet(
      uiState = uiState,
      onStartExport = { viewModel.startWavExport() },
      onCancelExport = { viewModel.cancelExport() },
      onSaveToSaf = {
        val songTitle = (uiState.song?.title ?: "Mix").replace(Regex("[^a-zA-Z0-9._-]"), "_")
        createDocumentLauncher.launch("${songTitle}_Master.wav")
      },
      onShare = {
        val shareIntent = viewModel.getShareIntent()
        if (shareIntent != null) {
          context.startActivity(Intent.createChooser(shareIntent, "Share Master WAV Mix"))
        }
      },
      onDismiss = { viewModel.closeExportSheet() }
    )
  }
}

/**
 * Individual Channel Strip Card representing a track:
 * - name, type badge
 * - synchronized real waveform scrubbing playhead
 * - volume fader (0% to 200%)
 * - mute button (active visual state)
 * - solo button (active visual state)
 * - delete button
 */
@Composable
private fun TrackChannelCard(
  trackModel: MixerTrackUiModel,
  playbackPositionMs: Long,
  totalDurationMs: Long,
  isAnyTrackSolo: Boolean,
  onVolumeChange: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleSolo: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val track = trackModel.track
  val (typeColor, typeLabel) = when (track.type) {
    TrackType.BEAT -> StudioMint to "BEAT"
    TrackType.VOCAL -> StudioRed to "VOCAL"
    TrackType.RECORDING -> StudioPurple to "RECORDING"
    TrackType.IMPORTED_AUDIO -> StudioAmber to "IMPORTED"
    else -> StudioBlue to "TRACK"
  }

  val isAudible = if (isAnyTrackSolo) {
    track.solo && !track.muted
  } else {
    !track.muted
  }

  val cardAlpha = if (isAudible) 1.0f else 0.5f

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("track_channel_${track.id}"),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = if (track.solo) 6.dp else 2.dp,
    border = if (track.solo) {
      BorderStroke(1.5.dp, StudioAmber)
    } else if (track.muted) {
      BorderStroke(1.dp, StudioRed.copy(alpha = 0.4f))
    } else {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Row 1: Header (Track Type Badge, Name, Duration, Delete Button)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Track Type Chip
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(typeColor.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = typeLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              ),
              color = typeColor
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = track.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )

          if (track.duration > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = formatDurationMs(track.duration),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }

        // Delete track action
        IconButton(
          onClick = onDelete,
          modifier = Modifier
            .size(32.dp)
            .testTag("delete_track_${track.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete track",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Row 2: Synchronized Waveform Display
      val trackProgress = if (track.duration > 0) {
        (playbackPositionMs.toFloat() / track.duration.toFloat()).coerceIn(0f, 1f)
      } else if (totalDurationMs > 0) {
        (playbackPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }

      WaveformVisualizer(
        amplitudes = trackModel.waveform,
        progress = trackProgress,
        isLoading = trackModel.isWaveformLoading,
        onSeek = { /* Seek is controlled globally through shared timeline */ },
        height = 40.dp,
        activeColor = if (isAudible) typeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Row 3: Channel Controls (Mute, Solo, Volume Fader, Volume Indicator)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // MUTE Button
        ChannelStripButton(
          text = "M",
          isActive = track.muted,
          activeColor = StudioRed,
          activeContentColor = Color.White,
          onClick = onToggleMute,
          testTag = "mute_track_${track.id}"
        )

        Spacer(modifier = Modifier.width(6.dp))

        // SOLO Button
        ChannelStripButton(
          text = "S",
          isActive = track.solo,
          activeColor = StudioAmber,
          activeContentColor = Color.Black,
          onClick = onToggleSolo,
          testTag = "solo_track_${track.id}"
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Track Volume Slider
        Icon(
          imageVector = when {
            track.muted || track.volume == 0f -> Icons.Default.VolumeOff
            track.volume < 0.5f -> Icons.Default.VolumeDown
            else -> Icons.Default.VolumeUp
          },
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Slider(
          value = track.volume,
          onValueChange = onVolumeChange,
          valueRange = 0f..2.0f,
          colors = SliderDefaults.colors(
            thumbColor = if (isAudible) typeColor else MaterialTheme.colorScheme.outline,
            activeTrackColor = if (isAudible) typeColor else MaterialTheme.colorScheme.outlineVariant
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("volume_slider_${track.id}")
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
          text = "${(track.volume * 100).toInt()}%",
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.width(42.dp)
        )
      }
    }
  }
}

/**
 * Mute / Solo Hardware Style Button
 */
@Composable
private fun ChannelStripButton(
  text: String,
  isActive: Boolean,
  activeColor: Color,
  activeContentColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(34.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isActive) activeColor else MaterialTheme.colorScheme.surfaceVariant)
      .clickable(onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      color = if (isActive) activeContentColor else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

/**
 * Shared Timeline Master Transport Bar & Master Gain Control
 */
@Composable
private fun MasterTransportBar(
  playbackState: MixerPlaybackState,
  onPlay: () -> Unit,
  onPause: () -> Unit,
  onStop: () -> Unit,
  onSeek: (Long) -> Unit,
  onMasterVolumeChange: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("master_transport_bar"),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    shadowElevation = 10.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Timeline Scrubber Slider
      val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
      } else {
        0f
      }

      Slider(
        value = progress,
        onValueChange = { fraction ->
          val targetMs = (fraction * playbackState.durationMs).toLong()
          onSeek(targetMs)
        },
        colors = SliderDefaults.colors(
          thumbColor = StudioBlue,
          activeTrackColor = StudioBlue
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(24.dp)
          .testTag("mixer_timeline_slider")
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Row 2: Transport Buttons, Timecode, Master Volume, Clipping Indicator
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Transport: Stop, Play/Pause
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Stop button
          IconButton(
            onClick = onStop,
            modifier = Modifier
              .size(38.dp)
              .testTag("mixer_stop_button")
          ) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = "Stop",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Play/Pause button
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (playbackState.isPlaying) StudioMint else StudioBlue)
              .clickable(onClick = if (playbackState.isPlaying) onPause else onPlay)
              .testTag("mixer_play_pause_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Timecode
          Text(
            text = "${formatDurationMs(playbackState.currentPositionMs)} / ${formatDurationMs(playbackState.durationMs)}",
            style = MaterialTheme.typography.labelMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Master Volume & Anti-Clipping Guard
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.width(160.dp)
        ) {
          // Clipping Guard Lamp Indicator
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(
                if (playbackState.isClippingPrevented) StudioRed
                else if (playbackState.peakOutputLevel > 0.85f) StudioAmber
                else StudioMint
              )
          )

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = "Master",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.width(4.dp))

          Slider(
            value = playbackState.masterVolume,
            onValueChange = onMasterVolumeChange,
            valueRange = 0f..2.0f,
            colors = SliderDefaults.colors(
              thumbColor = StudioMint,
              activeTrackColor = StudioMint
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("master_volume_slider")
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyMixerState(
  onAddTrack: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(StudioBlue.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = null,
          tint = StudioBlue,
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "No Audio Tracks Added",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Add Beat stems, Vocal takes, or Audio recordings to mix and synchronize them together.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(18.dp))

      FilledTonalButton(
        onClick = onAddTrack,
        modifier = Modifier.testTag("empty_add_track_button"),
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = StudioBlue,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add First Track", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

/**
 * Bottom sheet dialog for configuring and rendering a 16-bit 44.1kHz Stereo WAV mixdown,
 * tracking real-time rendering progress, handling cancellation, and triggering Android Share Sheet
 * or SAF file persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavExportBottomSheet(
  uiState: MultiTrackMixerUiState,
  onStartExport: () -> Unit,
  onCancelExport: () -> Unit,
  onSaveToSaf: () -> Unit,
  onShare: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = modifier.testTag("export_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 12.dp)
        .padding(bottom = 24.dp)
    ) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StudioMint.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = null,
            tint = StudioMint,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Export Master WAV",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "44.1 kHz • 16-bit Linear PCM • Stereo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_export_sheet_button")) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      when {
        // 1. Export in progress
        uiState.isExporting -> {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = uiState.exportProgress.statusMessage.ifEmpty { "Rendering audio..." },
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                  text = "${(uiState.exportProgress.fraction * 100).toInt()}%",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = StudioMint
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              LinearProgressIndicator(
                progress = { uiState.exportProgress.fraction },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .testTag("export_progress_bar"),
                color = StudioMint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
              )

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                val currentSec = uiState.exportProgress.currentPositionMs / 1000
                val totalSec = uiState.exportProgress.totalDurationMs / 1000
                Text(
                  text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60),
                  style = MaterialTheme.typography.labelMedium,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.exportProgress.isClippingDetected) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = StudioAmber.copy(alpha = 0.2f)
                  ) {
                    Text(
                      text = "Limiter: Active",
                      style = MaterialTheme.typography.labelSmall,
                      color = StudioAmber,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          OutlinedButton(
            onClick = onCancelExport,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cancel_export_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRed)
          ) {
            Text("Cancel Mixdown")
          }
        }

        // 2. Export finished successfully
        uiState.exportResult != null && uiState.exportResult.success -> {
          val result = uiState.exportResult
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioMint.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, StudioMint.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = StudioMint,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Mixdown Rendered & Validated!",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = StudioMint
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              val sizeKb = result.totalBytesWritten / 1024
              val sizeStr = if (sizeKb > 1024) String.format("%.2f MB", sizeKb / 1024f) else "$sizeKb KB"
              val durSec = result.durationMs / 1000
              val durStr = String.format("%02d:%02d", durSec / 60, durSec % 60)

              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "File: ${result.file?.name ?: "Master_Mix.wav"}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "Duration: $durStr • Size: $sizeStr",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Format: 44,100 Hz • 16-bit Stereo PCM (RIFF)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = if (result.clippingPrevented) "Dynamics: Anti-clipping soft saturation applied" else "Dynamics: Clean headroom (0 clips detected)",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (result.clippingPrevented) StudioAmber else StudioMint
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Action Buttons: Share and SAF Save
          Button(
            onClick = onShare,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("share_export_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = StudioBlue,
              contentColor = Color.White
            )
          ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share Master WAV via Share Sheet", fontWeight = FontWeight.SemiBold)
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = onSaveToSaf,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("save_saf_export_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to Files (SAF)")
          }

          Spacer(modifier = Modifier.height(8.dp))

          TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Done")
          }
        }

        // 3. Idle / Configuration State
        else -> {
          val activeTrackCount = uiState.tracks.count { it.fileExists && !it.track.muted }
          val totalDurationMs = uiState.playbackState.durationMs
          val totalSec = totalDurationMs / 1000
          val durationStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60)

          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Active Channels:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$activeTrackCount Tracks", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Project Duration:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(durationStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Master Gain:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(uiState.playbackState.masterVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Protection:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Soft Limiter & Tanh Saturation", style = MaterialTheme.typography.bodySmall, color = StudioMint, fontWeight = FontWeight.SemiBold)
              }
            }
          }

          if (uiState.exportErrorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = StudioRed.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, StudioRed.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = StudioRed, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = uiState.exportErrorMessage,
                  style = MaterialTheme.typography.bodySmall,
                  color = StudioRed
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          Button(
            onClick = onStartExport,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("start_export_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = StudioMint,
              contentColor = Color.Black
            ),
            enabled = activeTrackCount > 0
          ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Render WAV Mixdown", fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedButton(
            onClick = onSaveToSaf,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("start_saf_export_button"),
            shape = RoundedCornerShape(12.dp),
            enabled = activeTrackCount > 0
          ) {
            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Location & Export (SAF)")
          }
        }
      }
    }
  }
}

