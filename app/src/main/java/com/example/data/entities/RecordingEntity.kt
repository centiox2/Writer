package com.example.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "recordings",
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
data class RecordingEntity(
  @PrimaryKey
  val id: String,
  val songId: String,
  val name: String,
  val uri: String,
  val duration: Long = 0L, // duration in milliseconds
  val createdAt: Long = System.currentTimeMillis()
)
