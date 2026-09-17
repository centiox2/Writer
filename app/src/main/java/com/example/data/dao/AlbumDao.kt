package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.AlbumEntity
import kotlinx.coroutines.flow.Flow

data class AlbumWithSongCount(
  val id: String,
  val name: String,
  val description: String,
  val artworkUri: String?,
  val createdAt: Long,
  val updatedAt: Long,
  val songCount: Int
)

@Dao
interface AlbumDao {
  @Query("SELECT * FROM albums ORDER BY updatedAt DESC, rowid DESC")
  fun getAllAlbums(): Flow<List<AlbumEntity>>

  @Query("SELECT * FROM albums ORDER BY updatedAt DESC, rowid DESC")
  suspend fun getAllAlbumsSync(): List<AlbumEntity>

  @Query("""
    SELECT 
      a.id, a.name, a.description, a.artworkUri, a.createdAt, a.updatedAt,
      COUNT(s.id) as songCount
    FROM albums a
    LEFT JOIN songs s ON s.albumId = a.id AND s.archived = 0
    GROUP BY a.id
    ORDER BY a.updatedAt DESC, a.rowid DESC
  """)
  fun getAlbumsWithSongCount(): Flow<List<AlbumWithSongCount>>

  @Query("SELECT * FROM albums WHERE id = :id")
  fun getAlbumByIdFlow(id: String): Flow<AlbumEntity?>

  @Query("SELECT * FROM albums WHERE id = :id")
  suspend fun getAlbumById(id: String): AlbumEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAlbum(album: AlbumEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAlbums(albums: List<AlbumEntity>)

  @Update
  suspend fun updateAlbum(album: AlbumEntity)

  @Query("UPDATE albums SET name = :name, description = :description, artworkUri = :artworkUri, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateAlbumDetails(
    id: String,
    name: String,
    description: String,
    artworkUri: String?,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Delete
  suspend fun deleteAlbum(album: AlbumEntity)

  @Query("DELETE FROM albums WHERE id = :id")
  suspend fun deleteAlbumById(id: String)
}
