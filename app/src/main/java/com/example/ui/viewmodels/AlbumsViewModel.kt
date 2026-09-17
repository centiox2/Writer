package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.AlbumRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.Album
import com.example.domain.models.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumDetailUiState(
  val album: Album? = null,
  val songs: List<Song> = emptyList(),
  val availableSongsToAdd: List<Song> = emptyList()
)

class AlbumsViewModel(
  private val albumRepository: AlbumRepository,
  private val songRepository: SongRepository
) : ViewModel() {

  val albums: StateFlow<List<Album>> = albumRepository.getAllAlbums()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _selectedAlbumId = MutableStateFlow<String?>(null)
  val selectedAlbumId: StateFlow<String?> = _selectedAlbumId

  @OptIn(ExperimentalCoroutinesApi::class)
  val selectedAlbumSongs: StateFlow<List<Song>> = _selectedAlbumId
    .flatMapLatest { albumId ->
      if (albumId == null) flowOf(emptyList()) else songRepository.getSongsByAlbum(albumId)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeSongs: StateFlow<List<Song>> = songRepository.getActiveSongs()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun selectAlbum(albumId: String?) {
    _selectedAlbumId.value = albumId
  }

  fun createAlbum(name: String, description: String = "", artworkUri: String? = null) {
    viewModelScope.launch {
      albumRepository.createAlbum(name, description, artworkUri)
    }
  }

  fun renameAlbum(id: String, newName: String, description: String = "", artworkUri: String? = null) {
    viewModelScope.launch {
      albumRepository.renameAlbum(id, newName, description, artworkUri)
    }
  }

  /**
   * Deleting an album does NOT delete its songs.
   */
  fun deleteAlbum(id: String) {
    viewModelScope.launch {
      albumRepository.deleteAlbum(id)
      if (_selectedAlbumId.value == id) {
        _selectedAlbumId.value = null
      }
    }
  }

  fun addSongToAlbum(songId: String, albumId: String) {
    viewModelScope.launch {
      albumRepository.addSongToAlbum(songId, albumId)
    }
  }

  fun addMultipleSongsToAlbum(songIds: List<String>, albumId: String) {
    viewModelScope.launch {
      songIds.forEach { songId ->
        albumRepository.addSongToAlbum(songId, albumId)
      }
    }
  }

  fun removeSongFromAlbum(songId: String) {
    viewModelScope.launch {
      albumRepository.removeSongFromAlbum(songId)
    }
  }
}
