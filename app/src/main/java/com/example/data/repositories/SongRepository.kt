package com.example.data.repositories

import com.example.data.dao.SongDao
import com.example.data.dao.SongWithDetails
import com.example.data.entities.SongEntity
import com.example.domain.models.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class SongSortOrder {
  RECENTLY_MODIFIED,
  TITLE_AZ,
  TITLE_ZA,
  DATE_CREATED
}

class SongRepository(
  private val songDao: SongDao
) {
  fun getActiveSongs(): Flow<List<Song>> {
    return songDao.getActiveSongsWithDetails().map { list -> list.map { it.toDomain() } }
  }

  suspend fun getActiveSongsSync(): List<Song> {
    return songDao.getActiveSongsSync().map { it.toDomain() }
  }

  fun getAllSongs(): Flow<List<Song>> = getActiveSongs()

  fun getFavoriteSongs(): Flow<List<Song>> {
    return songDao.getFavoriteSongsWithDetails().map { list -> list.map { it.toDomain() } }
  }

  fun getArchivedSongs(): Flow<List<Song>> {
    return songDao.getArchivedSongsWithDetails().map { list -> list.map { it.toDomain() } }
  }

  fun getSongsByAlbum(albumId: String): Flow<List<Song>> {
    return songDao.getSongsByAlbumWithDetails(albumId).map { list -> list.map { it.toDomain() } }
  }

  fun searchSongs(query: String): Flow<List<Song>> {
    return songDao.searchSongsWithDetails(query).map { list -> list.map { it.toDomain() } }
  }

  fun getSongById(id: String): Flow<Song?> {
    return songDao.getSongWithDetailsById(id).map { it?.toDomain() }
  }

  suspend fun getSongByIdSync(id: String): Song? {
    return songDao.getSongById(id)?.toDomain()
  }

  suspend fun insertOrUpdate(song: Song) {
    songDao.insertSong(song.toEntity())
  }

  suspend fun createSong(title: String, albumId: String? = null, lyrics: String = ""): Song {
    val newSong = Song(
      id = UUID.randomUUID().toString(),
      title = title.ifBlank { "Untitled Song" },
      lyrics = lyrics,
      albumId = albumId,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    songDao.insertSong(newSong.toEntity())
    return newSong
  }

  suspend fun renameSong(id: String, newTitle: String) {
    songDao.renameSong(id, newTitle.ifBlank { "Untitled Song" })
  }

  suspend fun duplicateSong(id: String): Song? {
    val original = songDao.getSongById(id) ?: return null
    val copy = original.copy(
      id = UUID.randomUUID().toString(),
      title = "${original.title} (Copy)",
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      favorite = false
    )
    songDao.insertSong(copy)
    return copy.toDomain()
  }

  suspend fun updateFavorite(id: String, isFavorite: Boolean) {
    songDao.updateFavorite(id, isFavorite)
  }

  suspend fun updateArchived(id: String, isArchived: Boolean) {
    songDao.updateArchived(id, isArchived)
  }

  suspend fun updateLyrics(id: String, lyrics: String) {
    songDao.updateLyrics(id, lyrics)
  }

  suspend fun setSongAlbum(songId: String, albumId: String?) {
    songDao.setSongAlbum(songId, albumId)
  }

  suspend fun deleteSongById(id: String) {
    songDao.deleteSongById(id)
  }

  fun getSongCount(): Flow<Int> = songDao.getSongCount()
}

fun SongWithDetails.toDomain(): Song = Song(
  id = song.id,
  title = song.title,
  lyrics = song.lyrics,
  albumId = song.albumId,
  artworkUri = song.artworkUri,
  createdAt = song.createdAt,
  updatedAt = song.updatedAt,
  favorite = song.favorite,
  archived = song.archived,
  bpm = song.bpm,
  keySignature = song.keySignature,
  albumName = albumName,
  trackCount = trackCount,
  recordingCount = recordingCount
)

fun SongEntity.toDomain(): Song = Song(
  id = id,
  title = title,
  lyrics = lyrics,
  albumId = albumId,
  artworkUri = artworkUri,
  createdAt = createdAt,
  updatedAt = updatedAt,
  favorite = favorite,
  archived = archived,
  bpm = bpm,
  keySignature = keySignature
)

fun Song.toEntity(): SongEntity = SongEntity(
  id = id,
  title = title,
  lyrics = lyrics,
  albumId = albumId,
  artworkUri = artworkUri,
  createdAt = createdAt,
  updatedAt = updatedAt,
  favorite = favorite,
  archived = archived,
  bpm = bpm,
  keySignature = keySignature
)
