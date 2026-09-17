package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
  @Query("SELECT * FROM recordings WHERE songId = :songId ORDER BY createdAt DESC")
  fun getRecordingsForSong(songId: String): Flow<List<RecordingEntity>>

  @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
  fun getAllRecordings(): Flow<List<RecordingEntity>>

  @Query("SELECT * FROM recordings WHERE songId = :songId")
  suspend fun getRecordingsForSongSync(songId: String): List<RecordingEntity>

  @Query("SELECT * FROM recordings WHERE id = :id")
  suspend fun getRecordingById(id: String): RecordingEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecording(recording: RecordingEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecordings(recordings: List<RecordingEntity>)

  @Update
  suspend fun updateRecording(recording: RecordingEntity)

  @Delete
  suspend fun deleteRecording(recording: RecordingEntity)

  @Query("DELETE FROM recordings WHERE id = :id")
  suspend fun deleteRecordingById(id: String)

  @Query("UPDATE recordings SET name = :newName WHERE id = :id")
  suspend fun renameRecording(id: String, newName: String)

  @Query("SELECT COUNT(*) FROM recordings")
  fun getTotalRecordingsCount(): Flow<Int>
}
