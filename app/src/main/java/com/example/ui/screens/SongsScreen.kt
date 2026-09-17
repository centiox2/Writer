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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repositories.SongSortOrder
import com.example.domain.models.Album
import com.example.domain.models.Song
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.MoveSongToAlbumDialog
import com.example.ui.components.RenameSongDialog
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.StudioRed
import com.example.ui.viewmodels.SongFilter
import com.example.ui.viewmodels.SongsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
  viewModel: SongsViewModel,
  onOpenSong: (String) -> Unit = {},
  onNewSong: () -> Unit = {},
  onExportSong: (String) -> Unit = {},
  onImportProject: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val rawSearchQuery by viewModel.searchQuery.collectAsState()
  val albums by viewModel.albums.collectAsState()

  var songToRename by remember { mutableStateOf<Song?>(null) }
  var songToDelete by remember { mutableStateOf<Song?>(null) }
  var songToMove by remember { mutableStateOf<Song?>(null) }
  var showSortMenu by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Header Title and Sort / Import Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Studio Projects",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "${uiState.totalCount} ${if (uiState.totalCount == 1) "song" else "songs"}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onImportProject,
          modifier = Modifier.testTag("import_song_project_button")
        ) {
          Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = "Import .songproject",
            tint = StudioBlue
          )
        }
        
        Box {
          IconButton(
            onClick = { showSortMenu = true },
            modifier = Modifier.testTag("sort_button")
          ) {
            Icon(
              imageVector = Icons.Default.Sort,
              contentDescription = "Sort songs",
              tint = StudioBlue
            )
          }

          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
          ) {
          DropdownMenuItem(
            text = {
              Text(
                "Recently Modified",
                fontWeight = if (uiState.sortOrder == SongSortOrder.RECENTLY_MODIFIED) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              viewModel.onSortOrderChanged(SongSortOrder.RECENTLY_MODIFIED)
              showSortMenu = false
            }
          )
          DropdownMenuItem(
            text = {
              Text(
                "Title (A to Z)",
                fontWeight = if (uiState.sortOrder == SongSortOrder.TITLE_AZ) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              viewModel.onSortOrderChanged(SongSortOrder.TITLE_AZ)
              showSortMenu = false
            }
          )
          DropdownMenuItem(
            text = {
              Text(
                "Title (Z to A)",
                fontWeight = if (uiState.sortOrder == SongSortOrder.TITLE_ZA) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              viewModel.onSortOrderChanged(SongSortOrder.TITLE_ZA)
              showSortMenu = false
            }
          )
          DropdownMenuItem(
            text = {
              Text(
                "Date Created",
                fontWeight = if (uiState.sortOrder == SongSortOrder.DATE_CREATED) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              viewModel.onSortOrderChanged(SongSortOrder.DATE_CREATED)
              showSortMenu = false
            }
          )
        }
      }
    }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Debounced Search Bar
    OutlinedTextField(
      value = rawSearchQuery,
      onValueChange = { viewModel.onSearchQueryChanged(it) },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_input"),
      placeholder = {
        Text(
          "Search title or lyrics...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      trailingIcon = {
        if (rawSearchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = StudioBlue,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = uiState.activeFilter == SongFilter.ALL,
        onClick = { viewModel.onFilterChanged(SongFilter.ALL) },
        label = { Text("All Songs") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = StudioBlue,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp)
      )
      FilterChip(
        selected = uiState.activeFilter == SongFilter.FAVORITES,
        onClick = { viewModel.onFilterChanged(SongFilter.FAVORITES) },
        label = { Text("Favorites") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (uiState.activeFilter == SongFilter.FAVORITES) MaterialTheme.colorScheme.onPrimary else StudioMint
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = StudioBlue,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp)
      )
      FilterChip(
        selected = uiState.activeFilter == SongFilter.ARCHIVED,
        onClick = { viewModel.onFilterChanged(SongFilter.ARCHIVED) },
        label = { Text("Archive") },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Archive,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = StudioBlue,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.songs.isEmpty()) {
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
              imageVector = Icons.Default.MusicNote,
              contentDescription = null,
              tint = StudioBlue,
              modifier = Modifier.size(32.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = if (rawSearchQuery.isNotEmpty()) "No matches for \"$rawSearchQuery\"" else "No songs found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (rawSearchQuery.isNotEmpty()) "Try a different search query" else "Tap + to create your first songwriting project",
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
          .testTag("songs_list"),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(uiState.songs, key = { it.id }) { song ->
          SongItemCard(
            song = song,
            onClick = { onOpenSong(song.id) },
            onToggleFavorite = { viewModel.toggleFavorite(song.id, !song.favorite) },
            onRename = { songToRename = song },
            onDuplicate = { viewModel.duplicateSong(song.id) },
            onMoveToAlbum = { songToMove = song },
            onToggleArchive = { viewModel.toggleArchived(song.id, !song.archived) },
            onDelete = { songToDelete = song },
            onExportSong = { onExportSong(song.id) }
          )
        }
        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }

  // Rename Dialog
  songToRename?.let { song ->
    RenameSongDialog(
      currentTitle = song.title,
      onDismiss = { songToRename = null },
      onConfirm = { newTitle ->
        viewModel.renameSong(song.id, newTitle)
        songToRename = null
      }
    )
  }

  // Delete Dialog
  songToDelete?.let { song ->
    DeleteConfirmDialog(
      title = "Delete Song Project?",
      message = "Are you sure you want to delete \"${song.title}\"? This action cannot be undone and will delete all associated audio tracks and recordings.",
      confirmButtonText = "Delete Song",
      onDismiss = { songToDelete = null },
      onConfirm = {
        viewModel.deleteSong(song.id)
        songToDelete = null
      }
    )
  }

  // Move To Album Dialog
  songToMove?.let { song ->
    MoveSongToAlbumDialog(
      songTitle = song.title,
      currentAlbumId = song.albumId,
      albums = albums,
      onDismiss = { songToMove = null },
      onConfirm = { albumId ->
        viewModel.moveSongToAlbum(song.id, albumId)
        songToMove = null
      }
    )
  }
}

@Composable
fun SongItemCard(
  song: Song,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  onRename: () -> Unit,
  onDuplicate: () -> Unit,
  onMoveToAlbum: () -> Unit,
  onToggleArchive: () -> Unit,
  onDelete: () -> Unit,
  onExportSong: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
  val formattedDate = remember(song.updatedAt) { dateFormat.format(Date(song.updatedAt)) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("song_card_${song.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Icon / Vinyl sleeve
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StudioBlue.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = StudioBlue,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Album Badge
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold
          )

          if (!song.albumName.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = StudioMint,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = song.albumName,
                style = MaterialTheme.typography.labelSmall,
                color = StudioMint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // Favorite Button
        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier
            .size(36.dp)
            .testTag("favorite_button_${song.id}")
        ) {
          Icon(
            imageVector = if (song.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (song.favorite) "Favorite" else "Not favorite",
            tint = if (song.favorite) StudioMint else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Context Menu 3-Dots
        Box {
          IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
              .size(36.dp)
              .testTag("song_menu_button_${song.id}")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Song options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Export .songproject") },
              leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = StudioBlue, modifier = Modifier.size(18.dp)) },
              onClick = {
                showMenu = false
                onExportSong()
              },
              modifier = Modifier.testTag("menu_export_${song.id}")
            )
            DropdownMenuItem(
              text = { Text("Rename") },
              leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showMenu = false
                onRename()
              },
              modifier = Modifier.testTag("menu_rename_${song.id}")
            )
            DropdownMenuItem(
              text = { Text("Duplicate") },
              leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showMenu = false
                onDuplicate()
              },
              modifier = Modifier.testTag("menu_duplicate_${song.id}")
            )
            DropdownMenuItem(
              text = { Text("Assign to Album") },
              leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showMenu = false
                onMoveToAlbum()
              },
              modifier = Modifier.testTag("menu_move_${song.id}")
            )
            DropdownMenuItem(
              text = { Text(if (song.archived) "Unarchive" else "Archive") },
              leadingIcon = {
                Icon(
                  imageVector = if (song.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
              },
              onClick = {
                showMenu = false
                onToggleArchive()
              },
              modifier = Modifier.testTag("menu_archive_${song.id}")
            )
            DropdownMenuItem(
              text = { Text("Delete", color = StudioRed) },
              leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed, modifier = Modifier.size(18.dp)) },
              onClick = {
                showMenu = false
                onDelete()
              },
              modifier = Modifier.testTag("menu_delete_${song.id}")
            )
          }
        }
      }

      // Lyric Preview
      Spacer(modifier = Modifier.height(8.dp))
      if (song.lyrics.isNotBlank()) {
        Text(
          text = song.lyrics.replace("\n", " • "),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )
      } else {
        Text(
          text = "No lyrics written yet",
          style = MaterialTheme.typography.bodySmall,
          fontStyle = FontStyle.Italic,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Badges: Audio Tracks Indicator, Recording Takes Indicator, Last Modified Date
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Audio & Recording Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (song.trackCount > 0) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(StudioBlue.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Audiotrack,
                contentDescription = null,
                tint = StudioBlue,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${song.trackCount} ${if (song.trackCount == 1) "track" else "tracks"}",
                style = MaterialTheme.typography.labelSmall,
                color = StudioBlue,
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (song.recordingCount > 0) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(StudioMint.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = StudioMint,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${song.recordingCount} ${if (song.recordingCount == 1) "take" else "takes"}",
                style = MaterialTheme.typography.labelSmall,
                color = StudioMint,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Last Modified Date
        Text(
          text = "Updated $formattedDate",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }
    }
  }
}
