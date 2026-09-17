package com.example.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "songs",
  foreignKeys = [
    ForeignKey(
      entity = AlbumEntity::class,
      parentColumns = ["id"],
      childColumns = ["albumId"],
      onDelete = ForeignKey.SET_NULL
    )
  ],
  indices = [
    Index(value = ["albumId"]),
    Index(value = ["updatedAt"]),
    Index(value = ["favorite"]),
    Index(value = ["archived"])
  ]
)
data class SongEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val lyrics: String = "",
  val albumId: String? = null,
  val artworkUri: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val favorite: Boolean = false,
  val archived: Boolean = false,
  val bpm: Int? = null,
  val keySignature: String? = null
)
