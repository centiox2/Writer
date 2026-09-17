package com.example.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "audio_tracks",
  foreignKeys = [
    ForeignKey(
      entity = SongEntity::class,
      parentColumns = ["id"],
      childColumns = ["songId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["songId"])]
)
data class AudioTrackEntity(
  @PrimaryKey
  val id: String,
  val songId: String,
  val name: String,
  val uri: String,
  val type: String, // "BEAT", "VOCAL", "RECORDING", "GUIDE", "INSTRUMENTAL"
  val duration: Long = 0L, // duration in milliseconds
  val volume: Float = 1.0f,
  val muted: Boolean = false,
  val solo: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)
