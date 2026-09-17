package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.material.icons.filled.FolderZip
import com.example.domain.models.Album
import com.example.domain.models.Song
import com.example.ui.components.AddSongsToAlbumDialog
import com.example.ui.components.AlbumArtworkThumbnail
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EditAlbumDialog
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioRed
import com.example.ui.viewmodels.AlbumsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlbumsScreen(
  viewModel: AlbumsViewModel,
  onOpenSong: (String) -> Unit = {},
  onExportAlbum: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val albums by viewModel.albums.collectAsState()
  val selectedAlbumId by viewModel.selectedAlbumId.collectAsState()
  val selectedAlbumSongs by viewModel.selectedAlbumSongs.collectAsState()
  val activeSongs by viewModel.activeSongs.collectAsState()

  val selectedAlbum = remember(selectedAlbumId, albums) {
    albums.find { it.id == selectedAlbumId }
  }

  var albumToEdit by remember { mutableStateOf<Album?>(null) }
  var albumToDelete by remember { mutableStateOf<Album?>(null) }
  var showCreateDialog by remember { mutableStateOf(false) }
  var showAddSongsDialog by remember { mutableStateOf(false) }
  var songToRemoveFromAlbum by remember { mutableStateOf<Song?>(null) }

  if (selectedAlbum != null) {
    // Album Detail Screen
    AlbumDetailView(
      album = selectedAlbum,
      songs = selectedAlbumSongs,
      onBack = { viewModel.selectAlbum(null) },
      onOpenSong = onOpenSong,
      onAddSongsClick = { showAddSongsDialog = true },
      onRemoveSong = { song -> songToRemoveFromAlbum = song },
      onEditAlbum = { albumToEdit = selectedAlbum },
      onDeleteAlbum = { albumToDelete = selectedAlbum },
      onExportAlbum = { onExportAlbum(selectedAlbum.id) },
      modifier = modifier
    )
  } else {
    // Albums List Screen
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 16.dp)
        .testTag("albums_screen")
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Albums & EPs",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${albums.size} ${if (albums.size == 1) "collection" else "collections"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedButton(
          onClick = { showCreateDialog = true },
          modifier = Modifier.testTag("new_album_top_button")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("New Album")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (albums.isEmpty()) {
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
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = StudioBlue,
                modifier = Modifier.size(32.dp)
              )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "No albums created yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Group your songs into concept albums, EPs, or mixtapes",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .weight(1f)
            .testTag("albums_list"),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(albums, key = { it.id }) { album ->
            AlbumItemCard(
              album = album,
              onClick = { viewModel.selectAlbum(album.id) },
              onEdit = { albumToEdit = album },
              onDelete = { albumToDelete = album },
              onExport = { onExportAlbum(album.id) }
            )
          }
          item {
            Spacer(modifier = Modifier.height(80.dp))
          }
        }
      }
    }
  }

  // Create Album Dialog
  if (showCreateDialog) {
    EditAlbumDialog(
      isEditing = false,
      onDismiss = { showCreateDialog = false },
      onConfirm = { name, description, artworkUri ->
        viewModel.createAlbum(name, description, artworkUri)
        showCreateDialog = false
      }
    )
  }

  // Edit Album Dialog
  albumToEdit?.let { album ->
    EditAlbumDialog(
      initialName = album.name,
      initialDescription = album.description,
      initialArtworkUri = album.artworkUri,
      isEditing = true,
      onDismiss = { albumToEdit = null },
      onConfirm = { name, description, artworkUri ->
        viewModel.renameAlbum(album.id, name, description, artworkUri)
        albumToEdit = null
      }
    )
  }

  // Delete Album Confirmation Dialog (Explicitly informs user songs won't be deleted)
  albumToDelete?.let { album ->
    DeleteConfirmDialog(
      title = "Delete Album \"${album.name}\"?",
      message = "Deleting this album will NOT delete its songs. All ${album.songCount} songs will remain safely in your library as standalone tracks.",
      confirmButtonText = "Delete Album",
      onDismiss = { albumToDelete = null },
      onConfirm = {
        viewModel.deleteAlbum(album.id)
        albumToDelete = null
      }
    )
  }

  // Remove Song from Album Confirmation Dialog
  songToRemoveFromAlbum?.let { song ->
    DeleteConfirmDialog(
      title = "Remove from Album?",
      message = "Remove \"${song.title}\" from this album? The song will remain in your library.",
      confirmButtonText = "Remove",
      onDismiss = { songToRemoveFromAlbum = null },
      onConfirm = {
        viewModel.removeSongFromAlbum(song.id)
        songToRemoveFromAlbum = null
      }
    )
  }

  // Add Songs to Album Dialog
  if (showAddSongsDialog && selectedAlbum != null) {
    val unassignedSongs = remember(activeSongs, selectedAlbumSongs) {
      val currentIds = selectedAlbumSongs.map { it.id }.toSet()
      activeSongs.filter { !currentIds.contains(it.id) }
    }

    AddSongsToAlbumDialog(
      albumName = selectedAlbum.name,
      availableSongs = unassignedSongs,
      onDismiss = { showAddSongsDialog = false },
      onConfirm = { songIds ->
        viewModel.addMultipleSongsToAlbum(songIds, selectedAlbum.id)
        showAddSongsDialog = false
      }
    )
  }
}

@Composable
fun AlbumItemCard(
  album: Album,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onExport: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
  val formattedDate = remember(album.updatedAt) { dateFormat.format(Date(album.updatedAt)) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("album_card_${album.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AlbumArtworkThumbnail(artworkUri = album.artworkUri, size = 52.dp, shapeRadius = 10.dp)

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = album.name,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (album.description.isNotBlank()) {
          Text(
            text = album.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = "${album.songCount} ${if (album.songCount == 1) "song" else "songs"} • Updated $formattedDate",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
      }

      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.testTag("album_menu_${album.id}")
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Album options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("Export Album (.songproject)") },
            leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = StudioBlue, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onExport()
            },
            modifier = Modifier.testTag("menu_export_album_${album.id}")
          )
          DropdownMenuItem(
            text = { Text("Edit Album") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onEdit()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete Album", color = StudioRed) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
fun AlbumDetailView(
  album: Album,
  songs: List<Song>,
  onBack: () -> Unit,
  onOpenSong: (String) -> Unit,
  onAddSongsClick: () -> Unit,
  onRemoveSong: (Song) -> Unit,
  onEditAlbum: () -> Unit,
  onDeleteAlbum: () -> Unit,
  onExportAlbum: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp)
      .testTag("album_detail_view")
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Top Bar with Back, Title, and Menu
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("album_detail_back_button")
        ) {
          Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Albums")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = album.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Album actions")
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
          DropdownMenuItem(
            text = { Text("Export Album (.songproject)") },
            leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = StudioBlue, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onExportAlbum()
            }
          )
          DropdownMenuItem(
            text = { Text("Edit Album") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onEditAlbum()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete Album", color = StudioRed) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onDeleteAlbum()
            }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Album Hero Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AlbumArtworkThumbnail(artworkUri = album.artworkUri, size = 68.dp, shapeRadius = 12.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = album.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          if (album.description.isNotBlank()) {
            Text(
              text = album.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${songs.size} ${if (songs.size == 1) "track" else "tracks"}",
            style = MaterialTheme.typography.labelMedium,
            color = StudioBlue
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Track list header & Add Songs button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Tracklist",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )

      Button(
        onClick = onAddSongsClick,
        colors = ButtonDefaults.buttonColors(containerColor = StudioBlue),
        modifier = Modifier.testTag("add_songs_to_album_button")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Songs")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (songs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "No songs in this album yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap 'Add Songs' above to assign songs to this album",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f)
          .testTag("album_songs_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(songs, key = { it.id }) { song ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onOpenSong(song.id) },
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant),
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

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = song.title,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                if (song.lyrics.isNotBlank()) {
                  Text(
                    text = song.lyrics.replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              IconButton(
                onClick = { onRemoveSong(song) },
                modifier = Modifier.testTag("remove_song_${song.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.RemoveCircleOutline,
                  contentDescription = "Remove from album",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
        item {
          Spacer(modifier = Modifier.height(60.dp))
        }
      }
    }
  }
}
