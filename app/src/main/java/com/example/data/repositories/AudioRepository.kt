package com.example.data.repositories

import com.example.data.dao.AudioTrackDao
import com.example.data.dao.RecordingDao
import com.example.data.entities.AudioTrackEntity
import com.example.data.entities.RecordingEntity
import com.example.domain.models.AudioTrack
import com.example.domain.models.Recording
import com.example.domain.models.TrackType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AudioRepository(
  private val audioTrackDao: AudioTrackDao,
  private val recordingDao: RecordingDao
) {
  // Audio Tracks
  fun getTracksForSong(songId: String): Flow<List<AudioTrack>> {
    return audioTrackDao.getTracksForSong(songId).map { list -> list.map { it.toDomain() } }
  }

  fun getBeatTrackForSong(songId: String): Flow<AudioTrack?> {
    return audioTrackDao.getTrackBySongAndType(songId, TrackType.BEAT.name).map { it?.toDomain() }
  }

  suspend fun getBeatTrackForSongSync(songId: String): AudioTrack? {
    return audioTrackDao.getTrackBySongAndTypeSync(songId, TrackType.BEAT.name)?.toDomain()
  }

  suspend fun getTracksForSongSync(songId: String): List<AudioTrack> {
    return audioTrackDao.getTracksForSongSync(songId).map { it.toDomain() }
  }

  suspend fun addTrack(
    songId: String,
    name: String,
    uri: String,
    type: TrackType,
    duration: Long = 0L
  ): AudioTrack {
    val track = AudioTrack(
      id = UUID.randomUUID().toString(),
      songId = songId,
      name = name,
      uri = uri,
      type = type,
      duration = duration,
      createdAt = System.currentTimeMillis()
    )
    audioTrackDao.insertTrack(track.toEntity())
    return track
  }

  suspend fun updateTrack(track: AudioTrack) {
    audioTrackDao.updateTrack(track.toEntity())
  }

  suspend fun deleteTrack(trackId: String) {
    audioTrackDao.deleteTrackById(trackId)
  }

  // Recordings
  fun getRecordingsForSong(songId: String): Flow<List<Recording>> {
    return recordingDao.getRecordingsForSong(songId).map { list -> list.map { it.toDomain() } }
  }

  fun getAllRecordings(): Flow<List<Recording>> {
    return recordingDao.getAllRecordings().map { list -> list.map { it.toDomain() } }
  }

  suspend fun addRecording(
    songId: String,
    name: String,
    uri: String,
    duration: Long = 0L
  ): Recording {
    val recording = Recording(
      id = UUID.randomUUID().toString(),
      songId = songId,
      name = name,
      uri = uri,
      duration = duration,
      createdAt = System.currentTimeMillis()
    )
    recordingDao.insertRecording(recording.toEntity())
    return recording
  }

  suspend fun renameRecording(id: String, newName: String) {
    recordingDao.renameRecording(id, newName)
  }

  suspend fun deleteRecording(id: String) {
    recordingDao.deleteRecordingById(id)
  }

  fun getTotalRecordingsCount(): Flow<Int> = recordingDao.getTotalRecordingsCount()
}

fun AudioTrackEntity.toDomain(): AudioTrack = AudioTrack(
  id = id,
  songId = songId,
  name = name,
  uri = uri,
  type = TrackType.fromString(type),
  duration = duration,
  volume = volume,
  muted = muted,
  solo = solo,
  createdAt = createdAt
)

fun AudioTrack.toEntity(): AudioTrackEntity = AudioTrackEntity(
  id = id,
  songId = songId,
  name = name,
  uri = uri,
  type = type.name,
  duration = duration,
  volume = volume,
  muted = muted,
  solo = solo,
  createdAt = createdAt
)

fun RecordingEntity.toDomain(): Recording = Recording(
  id = id,
  songId = songId,
  name = name,
  uri = uri,
  duration = duration,
  createdAt = createdAt
)

fun Recording.toEntity(): RecordingEntity = RecordingEntity(
  id = id,
  songId = songId,
  name = name,
  uri = uri,
  duration = duration,
  createdAt = createdAt
)
