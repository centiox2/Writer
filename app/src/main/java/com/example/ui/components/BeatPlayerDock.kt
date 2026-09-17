package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioOutputRoute
import com.example.audio.BeatPlaybackState
import com.example.domain.models.AudioTrack
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioOrange
import com.example.ui.theme.StudioRed
import java.util.Locale

/**
 * Sticky Beat Player Dock attached to the bottom of the Lyric Editor.
 * Provides controls for importing, replacing, playing, pausing, seeking,
 * volume adjustments, loop toggles, audio output indicators, and missing-file handling.
 */
@Composable
fun BeatPlayerDock(
  beatTrack: AudioTrack?,
  playbackState: BeatPlaybackState,
  waveformAmplitudes: FloatArray?,
  isWaveformLoading: Boolean,
  onImportBeat: () -> Unit,
  onReplaceBeat: () -> Unit,
  onRemoveBeat: () -> Unit,
  onPlay: () -> Unit,
  onPause: () -> Unit,
  onSeek: (Long) -> Unit,
  onSeekRelative: (Long) -> Unit,
  onVolumeChange: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleLoop: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showOptionsDropdown by remember { mutableStateOf(false) }
  var showRemoveConfirmDialog by remember { mutableStateOf(false) }
  var isVolumeExpanded by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("beat_player_dock"),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shadowElevation = 8.dp
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

      if (beatTrack == null) {
        // Empty State: Add Instrumental / Beat
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(StudioBlue.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = StudioBlue,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Backing Beat / Instrumental",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "MP3, WAV, M4A, OGG, FLAC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          FilledTonalButton(
            onClick = onImportBeat,
            modifier = Modifier.testTag("import_beat_button"),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = StudioBlue.copy(alpha = 0.2f),
              contentColor = StudioBlue
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Import Beat", fontWeight = FontWeight.SemiBold)
          }
        }
      } else if (playbackState.isMissingFile) {
        // Missing File State
        MissingFileBanner(
          trackName = beatTrack.name,
          onReplace = onReplaceBeat,
          onRemove = { showRemoveConfirmDialog = true }
        )
      } else {
        // Active Beat Attached
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          // Row 1: Header (Filename, Output indicator, Volume icon, Options)
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = StudioMint,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = beatTrack.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )

              Spacer(modifier = Modifier.width(8.dp))

              // Audio Output Badge (Headphones, Bluetooth, Speaker)
              AudioOutputBadge(route = playbackState.audioOutputRoute)
            }

            // Volume Toggle Icon
            IconButton(
              onClick = { isVolumeExpanded = !isVolumeExpanded },
              modifier = Modifier
                .size(36.dp)
                .testTag("beat_volume_button")
            ) {
              val volIcon = when {
                playbackState.isMuted || playbackState.volume == 0f -> Icons.Default.VolumeOff
                playbackState.volume < 0.5f -> Icons.Default.VolumeDown
                else -> Icons.Default.VolumeUp
              }
              Icon(
                imageVector = volIcon,
                contentDescription = "Volume controls",
                tint = if (isVolumeExpanded) StudioBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }

            // More Options Menu (Replace, Remove)
            Box {
              IconButton(
                onClick = { showOptionsDropdown = true },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("beat_options_menu_button")
              ) {
                Icon(
                  imageVector = Icons.Default.MoreVert,
                  contentDescription = "Beat options",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp)
                )
              }

              DropdownMenu(
                expanded = showOptionsDropdown,
                onDismissRequest = { showOptionsDropdown = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Replace Beat") },
                  leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                  onClick = {
                    showOptionsDropdown = false
                    onReplaceBeat()
                  },
                  modifier = Modifier.testTag("replace_beat_menu_item")
                )
                DropdownMenuItem(
                  text = { Text("Remove Beat", color = StudioRed) },
                  leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed) },
                  onClick = {
                    showOptionsDropdown = false
                    showRemoveConfirmDialog = true
                  },
                  modifier = Modifier.testTag("remove_beat_menu_item")
                )
              }
            }
          }

          // Expandable Volume Slider
          AnimatedVisibility(
            visible = isVolumeExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = onToggleMute,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = if (playbackState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                  contentDescription = if (playbackState.isMuted) "Unmute" else "Mute",
                  tint = if (playbackState.isMuted) StudioOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }

              Slider(
                value = if (playbackState.isMuted) 0f else playbackState.volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier
                  .weight(1f)
                  .testTag("beat_volume_slider"),
                colors = SliderDefaults.colors(
                  thumbColor = StudioBlue,
                  activeTrackColor = StudioBlue,
                  inactiveTrackColor = StudioBlue.copy(alpha = 0.24f)
                )
              )

              Text(
                text = "${(if (playbackState.isMuted) 0 else (playbackState.volume * 100).toInt())}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Row 2: REAL Interactive Waveform Visualizer
          val progress = if (playbackState.durationMs > 0) {
            (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
          } else {
            0f
          }

          WaveformVisualizer(
            amplitudes = waveformAmplitudes,
            progress = progress,
            isLoading = isWaveformLoading,
            onSeek = { seekFraction ->
              val targetMs = (seekFraction * playbackState.durationMs).toLong()
              onSeek(targetMs)
            },
            height = 48.dp,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Row 3: Timecode & Transport Controls
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Timecode: 00:00 / 03:45
            Text(
              text = "${formatDurationMs(playbackState.currentPositionMs)} / ${formatDurationMs(playbackState.durationMs)}",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("beat_timecode_text")
            )

            // Playback controls (Jump back 5s, Play/Pause, Jump fwd 5s, Loop)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Jump back 5s
              IconButton(
                onClick = { onSeekRelative(-5000L) },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("beat_jump_back_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Replay5,
                  contentDescription = "Jump backward 5 seconds",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp)
                )
              }

              // Main Play/Pause Button (48x48 touch target)
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(if (playbackState.isPlaying) StudioMint else StudioBlue)
                  .clickable(onClick = {
                    if (playbackState.isPlaying) onPause() else onPlay()
                  })
                  .testTag("beat_play_pause_button"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                  contentDescription = if (playbackState.isPlaying) "Pause beat" else "Play beat",
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(26.dp)
                )
              }

              // Jump forward 5s
              IconButton(
                onClick = { onSeekRelative(5000L) },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("beat_jump_forward_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Forward5,
                  contentDescription = "Jump forward 5 seconds",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(20.dp)
                )
              }

              // Loop Toggle Button
              IconButton(
                onClick = onToggleLoop,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("beat_loop_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Loop,
                  contentDescription = if (playbackState.isLooping) "Loop enabled" else "Loop disabled",
                  tint = if (playbackState.isLooping) StudioMint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // Remove Beat Confirmation Dialog
  if (showRemoveConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showRemoveConfirmDialog = false },
      title = { Text("Remove Beat?") },
      text = {
        Text("This will remove the attached instrumental track from this song. Your written lyrics will remain unchanged.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showRemoveConfirmDialog = false
            onRemoveBeat()
          },
          colors = ButtonDefaults.textButtonColors(contentColor = StudioRed),
          modifier = Modifier.testTag("confirm_remove_beat_button")
        ) {
          Text("Remove", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showRemoveConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun AudioOutputBadge(route: AudioOutputRoute) {
  val (icon, label, tint) = when (route) {
    AudioOutputRoute.WIRED_HEADSET -> Triple(Icons.Default.Headphones, "Headphones", StudioMint)
    AudioOutputRoute.BLUETOOTH -> Triple(Icons.Default.Bluetooth, "Bluetooth", StudioBlue)
    AudioOutputRoute.SPEAKER -> Triple(Icons.Default.VolumeUp, "Speaker", MaterialTheme.colorScheme.onSurfaceVariant)
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(tint.copy(alpha = 0.12f))
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(12.dp)
    )
    Spacer(modifier = Modifier.width(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
      color = tint
    )
  }
}

@Composable
private fun MissingFileBanner(
  trackName: String,
  onReplace: () -> Unit,
  onRemove: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(StudioOrange.copy(alpha = 0.12f))
      .border(1.dp, StudioOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "Warning",
        tint = StudioOrange,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Audio File Missing: $trackName",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = StudioOrange
      )
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "The beat file was moved or deleted from the device storage. You can replace it or remove it.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(
        onClick = onRemove,
        colors = ButtonDefaults.textButtonColors(contentColor = StudioRed)
      ) {
        Text("Remove")
      }
      Spacer(modifier = Modifier.width(8.dp))
      OutlinedButton(
        onClick = onReplace,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioOrange),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Locate / Replace")
      }
    }
  }
}

fun formatDurationMs(millis: Long): String {
  val totalSeconds = (millis / 1000).coerceAtLeast(0)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
