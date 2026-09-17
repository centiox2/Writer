package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.models.Album
import com.example.domain.models.Song
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioRed

@Composable
fun RenameSongDialog(
  currentTitle: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var title by remember { mutableStateOf(currentTitle) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = StudioBlue)
    },
    title = {
      Text(
        text = "Rename Song Project",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column {
        Text(
          text = "Enter a new title for this songwriting project:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Song Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("rename_song_input")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = title.trim().ifBlank { currentTitle }
          onConfirm(clean)
        },
        colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
        modifier = Modifier.testTag("confirm_rename_button")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun DeleteConfirmDialog(
  title: String,
  message: String,
  confirmButtonText: String = "Delete",
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = StudioRed)
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = StudioRed),
        modifier = Modifier.testTag("confirm_delete_button")
      ) {
        Text(confirmButtonText)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun MoveSongToAlbumDialog(
  songTitle: String,
  currentAlbumId: String?,
  albums: List<Album>,
  onDismiss: () -> Unit,
  onConfirm: (selectedAlbumId: String?) -> Unit
) {
  var selectedId by remember { mutableStateOf(currentAlbumId) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(imageVector = Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, tint = StudioBlue)
    },
    title = {
      Text(
        text = "Assign to Album",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Select an album for \"$songTitle\":",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
        ) {
          // Option 1: No Album (Single / Standalone)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { selectedId = null }
              .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = selectedId == null,
              onClick = { selectedId = null },
              colors = RadioButtonDefaults.colors(selectedColor = StudioBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "No Album (Standalone Single)",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          // List of existing albums
          albums.forEach { album ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { selectedId = album.id }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedId == album.id,
                onClick = { selectedId = album.id },
                colors = RadioButtonDefaults.colors(selectedColor = StudioBlue)
              )
              Spacer(modifier = Modifier.width(8.dp))
              AlbumArtworkThumbnail(artworkUri = album.artworkUri, size = 32.dp, shapeRadius = 6.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = album.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedId) },
        colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
        modifier = Modifier.testTag("confirm_assign_album")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun EditAlbumDialog(
  initialName: String = "",
  initialDescription: String = "",
  initialArtworkUri: String? = null,
  isEditing: Boolean = false,
  onDismiss: () -> Unit,
  onConfirm: (name: String, description: String, artworkUri: String?) -> Unit
) {
  var name by remember { mutableStateOf(initialName) }
  var description by remember { mutableStateOf(initialDescription) }
  var artworkUri by remember { mutableStateOf(initialArtworkUri ?: ALBUM_COVER_PRESETS.first().id) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(imageVector = Icons.Default.Album, contentDescription = null, tint = StudioBlue)
    },
    title = {
      Text(
        text = if (isEditing) "Edit Album" else "Create Album",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Album Title") },
          placeholder = { Text("e.g. Neon Horizon") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("album_title_dialog_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)") },
          placeholder = { Text("Theme, mood, notes...") },
          singleLine = false,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Cover Artwork Style",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        AlbumCoverPresetPicker(
          selectedPresetId = artworkUri,
          onSelectPreset = { artworkUri = it }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanName = name.trim().ifBlank { if (isEditing) initialName else "New Album" }
          onConfirm(cleanName, description.trim(), artworkUri)
        },
        colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
        modifier = Modifier.testTag("confirm_save_album_button")
      ) {
        Text(if (isEditing) "Save Changes" else "Create Album")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
fun AddSongsToAlbumDialog(
  albumName: String,
  availableSongs: List<Song>,
  onDismiss: () -> Unit,
  onConfirm: (selectedSongIds: List<String>) -> Unit
) {
  val selectedSongIds = remember { mutableStateOf(setOf<String>()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Add Songs to \"$albumName\"",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      if (availableSongs.isEmpty()) {
        Text(
          text = "All your songs are already added to this album, or no other songs exist.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          items(availableSongs, key = { it.id }) { song ->
            val isChecked = selectedSongIds.value.contains(song.id)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  selectedSongIds.value = if (isChecked) {
                    selectedSongIds.value - song.id
                  } else {
                    selectedSongIds.value + song.id
                  }
                }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                  selectedSongIds.value = if (checked) {
                    selectedSongIds.value + song.id
                  } else {
                    selectedSongIds.value - song.id
                  }
                },
                colors = CheckboxDefaults.colors(checkedColor = StudioBlue)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = song.title,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (song.albumName != null) {
                  Text(
                    text = "Currently in: ${song.albumName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(selectedSongIds.value.toList()) },
        enabled = selectedSongIds.value.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
        modifier = Modifier.testTag("confirm_add_songs_to_album")
      ) {
        Text("Add (${selectedSongIds.value.size})")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp)
  )
}
