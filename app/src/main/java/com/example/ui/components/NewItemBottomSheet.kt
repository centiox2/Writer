package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CreateMode {
  object Selector : CreateMode()
  object CreateSong : CreateMode()
  object CreateAlbum : CreateMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewItemBottomSheet(
  onDismiss: () -> Unit,
  onCreateSong: (title: String) -> Unit,
  onCreateAlbum: (name: String, description: String, artworkUri: String?) -> Unit,
  onCreateVoiceIdea: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var mode by remember { mutableStateOf<CreateMode>(CreateMode.Selector) }
  var songTitle by remember { mutableStateOf("") }
  var albumName by remember { mutableStateOf("") }
  var albumDescription by remember { mutableStateOf("") }
  var selectedArtworkPreset by remember { mutableStateOf(ALBUM_COVER_PRESETS.first().id) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    modifier = Modifier.testTag("new_item_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .imePadding()
        .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
      when (mode) {
        CreateMode.Selector -> {
          Text(
            text = "Create New",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Choose what you want to create",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(20.dp))

          // New Song Option
          Card(
            onClick = { mode = CreateMode.CreateSong },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_song_option"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(StudioBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = StudioBlue)
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text("New Song Project", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Lyrics editor, beats, and recording studio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // New Album Option
          Card(
            onClick = { mode = CreateMode.CreateAlbum },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("create_album_option"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(StudioMint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = StudioMint)
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text("New Album / EP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Group and organize multiple songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Quick Voice Idea Option
          Card(
            onClick = {
              onCreateVoiceIdea()
              onDismiss()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("quick_idea_option"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(StudioRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = StudioRed)
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text("Quick Voice Idea", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Instant project for catching melodies on the fly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          Spacer(modifier = Modifier.height(28.dp))
        }

        CreateMode.CreateSong -> {
          Text(
            text = "New Song Project",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = songTitle,
            onValueChange = { songTitle = it },
            label = { Text("Song Title") },
            placeholder = { Text("e.g. Midnight Waves") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("song_title_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = StudioBlue
            )
          )

          Spacer(modifier = Modifier.height(24.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { mode = CreateMode.Selector }) {
              Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val title = songTitle.ifBlank { "Untitled Song" }
                onCreateSong(title)
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
              modifier = Modifier.testTag("confirm_create_song")
            ) {
              Text("Create Song")
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
        }

        CreateMode.CreateAlbum -> {
          Text(
            text = "New Album / EP",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = albumName,
            onValueChange = { albumName = it },
            label = { Text("Album Name") },
            placeholder = { Text("e.g. Neon Horizon") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("album_name_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = StudioBlue
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = albumDescription,
            onValueChange = { albumDescription = it },
            label = { Text("Description / Notes (Optional)") },
            placeholder = { Text("e.g. 10-track conceptual project") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = StudioBlue
            )
          )

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Cover Artwork Style",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))

          AlbumCoverPresetPicker(
            selectedPresetId = selectedArtworkPreset,
            onSelectPreset = { selectedArtworkPreset = it }
          )

          Spacer(modifier = Modifier.height(24.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { mode = CreateMode.Selector }) {
              Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val name = albumName.ifBlank { "New Album" }
                onCreateAlbum(name, albumDescription, selectedArtworkPreset)
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
              modifier = Modifier.testTag("confirm_create_album")
            ) {
              Text("Create Album")
            }
          }
          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }
  }
}
