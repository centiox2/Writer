package com.example.domain.models

data class Song(
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
  val keySignature: String? = null,
  val albumName: String? = null,
  val trackCount: Int = 0,
  val recordingCount: Int = 0
)
