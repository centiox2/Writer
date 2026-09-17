package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.AudioTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioTrackDao {
  @Query("SELECT * FROM audio_tracks WHERE songId = :songId ORDER BY createdAt ASC")
  fun getTracksForSong(songId: String): Flow<List<AudioTrackEntity>>

  @Query("SELECT * FROM audio_tracks WHERE songId = :songId AND type = :type LIMIT 1")
  fun getTrackBySongAndType(songId: String, type: String): Flow<AudioTrackEntity?>

  @Query("SELECT * FROM audio_tracks WHERE songId = :songId AND type = :type LIMIT 1")
  suspend fun getTrackBySongAndTypeSync(songId: String, type: String): AudioTrackEntity?

  @Query("SELECT * FROM audio_tracks WHERE songId = :songId")
  suspend fun getTracksForSongSync(songId: String): List<AudioTrackEntity>

  @Query("SELECT * FROM audio_tracks WHERE id = :id")
  suspend fun getTrackById(id: String): AudioTrackEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrack(track: AudioTrackEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTracks(tracks: List<AudioTrackEntity>)

  @Update
  suspend fun updateTrack(track: AudioTrackEntity)

  @Delete
  suspend fun deleteTrack(track: AudioTrackEntity)

  @Query("DELETE FROM audio_tracks WHERE id = :id")
  suspend fun deleteTrackById(id: String)

  @Query("DELETE FROM audio_tracks WHERE songId = :songId")
  suspend fun deleteTracksForSong(songId: String)
}
