package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.SongEntity
import kotlinx.coroutines.flow.Flow

data class SongWithDetails(
  @Embedded val song: SongEntity,
  val albumName: String?,
  val trackCount: Int,
  val recordingCount: Int
)

@Dao
interface SongDao {
  @Query("""
    SELECT 
      s.*, 
      a.name AS albumName,
      (SELECT COUNT(*) FROM audio_tracks at WHERE at.songId = s.id) AS trackCount,
      (SELECT COUNT(*) FROM recordings r WHERE r.songId = s.id) AS recordingCount
    FROM songs s
    LEFT JOIN albums a ON a.id = s.albumId
    WHERE s.archived = 0
    ORDER BY s.updatedAt DESC
  """)
  fun getActiveSongsWithDetails(): Flow<List<SongWithDetails>>

  @Query("""
    SELECT 
      s.*, 
      a.name AS albumName,
      (SELECT COUNT(*) FROM audio_tracks at WHERE at.songId = s.id) AS trackCount,
      (SELECT COUNT(*) FROM recordings r WHERE r.songId = s.id) AS recordingCount
    FROM songs s
    LEFT JOIN albums a ON a.id = s.albumId
    WHERE s.favorite = 1 AND s.archived = 0
    ORDER BY s.updatedAt DESC
  """)
  fun getFavoriteSongsWithDetails(): Flow<List<SongWithDetails>>

  @Query("""
    SELECT 
      s.*, 
      a.name AS albumName,
      (SELECT COUNT(*) FROM audio_tracks at WHERE at.songId = s.id) AS trackCount,
      (SELECT COUNT(*) FROM recordings r WHERE r.songId = s.id) AS recordingCount
    FROM songs s
    LEFT JOIN albums a ON a.id = s.albumId
    WHERE s.archived = 1
    ORDER BY s.updatedAt DESC
  """)
  fun getArchivedSongsWithDetails(): Flow<List<SongWithDetails>>

  @Query("""
    SELECT 
      s.*, 
      a.name AS albumName,
      (SELECT COUNT(*) FROM audio_tracks at WHERE at.songId = s.id) AS trackCount,
      (SELECT COUNT(*) FROM recordings r WHERE r.songId = s.id) AS recordingCount
    FROM songs s
    LEFT JOIN albums a ON a.id = s.albumId
    WHERE s.albumId = :albumId AND s.archived = 0
    ORDER BY s.updatedAt DESC
  """)
  fun getSongsByAlbumWithDetails(albumId: String): Flow<List<SongWithDetails>>

  @Query("""
    SELECT 
      s.*, 
      a.name AS albumName,
      (SELECT COUNT(*) FROM audio_tracks at WHERE at.songId = s.id) AS trackCount,
      (SELECT COUNT(*) FROM recordings r WHERE r.songId = s.id) AS recordingCount
    FROM songs s
    LEFT JOIN albums a ON a.id = s.albumId
    WHERE (s.title LIKE '%' || :query || '%' OR s.lyrics LIKE '%' || :query || '%') AND s.archived = 0
    ORDER BY s.updatedAt DESC
  """)
  fun searchSongsWithDetails(query: String): Flow<List<SongWithDetails>>

  @Query("""
    SELECT 
      s.*, 
      a.name AS albumName,
      (SELECT COUNT(*) FROM audio_tracks at WHERE at.songId = s.id) AS trackCount,
      (SELECT COUNT(*) FROM recordings r WHERE r.songId = s.id) AS recordingCount
    FROM songs s
    LEFT JOIN albums a ON a.id = s.albumId
    WHERE s.id = :id
  """)
  fun getSongWithDetailsById(id: String): Flow<SongWithDetails?>

  @Query("SELECT * FROM songs WHERE archived = 0 ORDER BY updatedAt DESC")
  fun getActiveSongs(): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs WHERE archived = 0 ORDER BY updatedAt DESC")
  suspend fun getActiveSongsSync(): List<SongEntity>

  @Query("SELECT * FROM songs ORDER BY updatedAt DESC")
  suspend fun getAllSongsSync(): List<SongEntity>

  @Query("SELECT * FROM songs WHERE archived = 1 ORDER BY updatedAt DESC")
  fun getArchivedSongs(): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs WHERE favorite = 1 AND archived = 0 ORDER BY updatedAt DESC")
  fun getFavoriteSongs(): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs WHERE albumId = :albumId AND archived = 0 ORDER BY updatedAt DESC")
  fun getSongsByAlbum(albumId: String): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs WHERE albumId = :albumId AND archived = 0 ORDER BY updatedAt DESC")
  suspend fun getSongsByAlbumSync(albumId: String): List<SongEntity>

  @Query("SELECT * FROM songs WHERE id = :id")
  fun getSongByIdFlow(id: String): Flow<SongEntity?>

  @Query("SELECT * FROM songs WHERE id = :id")
  suspend fun getSongById(id: String): SongEntity?

  @Query("SELECT * FROM songs WHERE (title LIKE '%' || :query || '%' OR lyrics LIKE '%' || :query || '%') AND archived = 0 ORDER BY updatedAt DESC")
  fun searchSongs(query: String): Flow<List<SongEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSong(song: SongEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSongs(songs: List<SongEntity>)

  @Update
  suspend fun updateSong(song: SongEntity)

  @Delete
  suspend fun deleteSong(song: SongEntity)

  @Query("DELETE FROM songs WHERE id = :id")
  suspend fun deleteSongById(id: String)

  @Query("UPDATE songs SET title = :newTitle, updatedAt = :updatedAt WHERE id = :id")
  suspend fun renameSong(id: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE songs SET favorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateFavorite(id: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE songs SET archived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateArchived(id: String, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE songs SET lyrics = :lyrics, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateLyrics(id: String, lyrics: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE songs SET albumId = :albumId, updatedAt = :updatedAt WHERE id = :songId")
  suspend fun setSongAlbum(songId: String, albumId: String?, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE songs SET albumId = NULL WHERE albumId = :albumId")
  suspend fun clearAlbumFromSongs(albumId: String)

  @Query("SELECT COUNT(*) FROM songs WHERE archived = 0")
  fun getSongCount(): Flow<Int>
}
