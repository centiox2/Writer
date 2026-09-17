package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repositories.AlbumRepository
import com.example.data.repositories.SongRepository
import com.example.data.repositories.SongSortOrder
import com.example.domain.models.Album
import com.example.domain.models.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SongFilter {
  ALL,
  FAVORITES,
  ARCHIVED
}

data class SongsUiState(
  val songs: List<Song> = emptyList(),
  val searchQuery: String = "",
  val activeFilter: SongFilter = SongFilter.ALL,
  val sortOrder: SongSortOrder = SongSortOrder.RECENTLY_MODIFIED,
  val albums: List<Album> = emptyList(),
  val totalCount: Int = 0
)

private data class FilterCriteria(
  val query: String,
  val filter: SongFilter,
  val sort: SongSortOrder,
  val albumList: List<Album>
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SongsViewModel(
  private val songRepository: SongRepository,
  private val albumRepository: AlbumRepository
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery

  private val _activeFilter = MutableStateFlow(SongFilter.ALL)
  val activeFilter: StateFlow<SongFilter> = _activeFilter

  private val _sortOrder = MutableStateFlow(SongSortOrder.RECENTLY_MODIFIED)
  val sortOrder: StateFlow<SongSortOrder> = _sortOrder

  val albums: StateFlow<List<Album>> = albumRepository.getAllAlbums()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val debouncedSearchQuery = _searchQuery
    .debounce(300L)
    .distinctUntilChanged()

  val uiState: StateFlow<SongsUiState> = combine(
    debouncedSearchQuery,
    _activeFilter,
    _sortOrder,
    albums
  ) { query: String, filter: SongFilter, sort: SongSortOrder, albumList: List<Album> ->
    FilterCriteria(query, filter, sort, albumList)
  }.flatMapLatest { criteria ->
    val baseFlow = when {
      criteria.query.isNotBlank() -> songRepository.searchSongs(criteria.query)
      criteria.filter == SongFilter.FAVORITES -> songRepository.getFavoriteSongs()
      criteria.filter == SongFilter.ARCHIVED -> songRepository.getArchivedSongs()
      else -> songRepository.getActiveSongs()
    }

    baseFlow.map { list ->
      val sortedList = when (criteria.sort) {
        SongSortOrder.RECENTLY_MODIFIED -> list.sortedByDescending { it.updatedAt }
        SongSortOrder.TITLE_AZ -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        SongSortOrder.TITLE_ZA -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
        SongSortOrder.DATE_CREATED -> list.sortedByDescending { it.createdAt }
      }

      SongsUiState(
        songs = sortedList,
        searchQuery = _searchQuery.value,
        activeFilter = criteria.filter,
        sortOrder = criteria.sort,
        albums = criteria.albumList,
        totalCount = sortedList.size
      )
    }
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    SongsUiState()
  )

  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
  }

  fun onFilterChanged(filter: SongFilter) {
    _activeFilter.value = filter
  }

  fun onSortOrderChanged(sortOrder: SongSortOrder) {
    _sortOrder.value = sortOrder
  }

  fun createSong(title: String, albumId: String? = null, lyrics: String = "", onCreated: ((String) -> Unit)? = null) {
    viewModelScope.launch {
      val song = songRepository.createSong(title, albumId, lyrics)
      onCreated?.invoke(song.id)
    }
  }

  fun renameSong(id: String, newTitle: String) {
    viewModelScope.launch {
      songRepository.renameSong(id, newTitle)
    }
  }

  fun deleteSong(id: String) {
    viewModelScope.launch {
      songRepository.deleteSongById(id)
    }
  }

  fun duplicateSong(id: String) {
    viewModelScope.launch {
      songRepository.duplicateSong(id)
    }
  }

  fun toggleFavorite(id: String, isFavorite: Boolean) {
    viewModelScope.launch {
      songRepository.updateFavorite(id, isFavorite)
    }
  }

  fun toggleArchived(id: String, isArchived: Boolean) {
    viewModelScope.launch {
      songRepository.updateArchived(id, isArchived)
    }
  }

  fun moveSongToAlbum(songId: String, albumId: String?) {
    viewModelScope.launch {
      songRepository.setSongAlbum(songId, albumId)
    }
  }
}
