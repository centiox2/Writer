package com.example.data.repositories

import com.example.data.dao.AlbumDao
import com.example.data.dao.SongDao
import com.example.data.entities.AlbumEntity
import com.example.domain.models.Album
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AlbumRepository(
  private val albumDao: AlbumDao,
  private val songDao: SongDao
) {
  fun getAllAlbums(): Flow<List<Album>> {
    return albumDao.getAlbumsWithSongCount().map { list ->
      list.map {
        Album(
          id = it.id,
          name = it.name,
          description = it.description,
          artworkUri = it.artworkUri,
          createdAt = it.createdAt,
          updatedAt = it.updatedAt,
          songCount = it.songCount
        )
      }
    }
  }

  fun getAlbumById(id: String): Flow<Album?> {
    return albumDao.getAlbumByIdFlow(id).map { it?.toDomain() }
  }

  suspend fun getAlbumByIdSync(id: String): Album? {
    return albumDao.getAlbumById(id)?.toDomain()
  }

  suspend fun createAlbum(name: String, description: String = "", artworkUri: String? = null): Album {
    val album = Album(
      id = UUID.randomUUID().toString(),
      name = name.ifBlank { "New Album" },
      description = description,
      artworkUri = artworkUri,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    albumDao.insertAlbum(album.toEntity())
    return album
  }

  suspend fun updateAlbum(album: Album) {
    albumDao.updateAlbum(album.toEntity())
  }

  suspend fun renameAlbum(id: String, newName: String, description: String = "", artworkUri: String? = null) {
    albumDao.updateAlbumDetails(
      id = id,
      name = newName.ifBlank { "Untitled Album" },
      description = description,
      artworkUri = artworkUri
    )
  }

  /**
   * Deleting an album must NOT delete its songs.
   * Explicitly detaches songs from this album first, then deletes the album record.
   */
  suspend fun deleteAlbum(id: String) {
    songDao.clearAlbumFromSongs(id)
    albumDao.deleteAlbumById(id)
  }

  suspend fun addSongToAlbum(songId: String, albumId: String) {
    songDao.setSongAlbum(songId, albumId)
  }

  suspend fun removeSongFromAlbum(songId: String) {
    songDao.setSongAlbum(songId, null)
  }

  suspend fun moveSongToAlbum(songId: String, newAlbumId: String?) {
    songDao.setSongAlbum(songId, newAlbumId)
  }
}

fun AlbumEntity.toDomain(): Album = Album(
  id = id,
  name = name,
  description = description,
  artworkUri = artworkUri,
  createdAt = createdAt,
  updatedAt = updatedAt
)

fun Album.toEntity(): AlbumEntity = AlbumEntity(
  id = id,
  name = name,
  description = description,
  artworkUri = artworkUri,
  createdAt = createdAt,
  updatedAt = updatedAt
)
