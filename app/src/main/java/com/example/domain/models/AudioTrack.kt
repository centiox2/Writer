package com.example.domain.models

data class AudioTrack(
  val id: String,
  val songId: String,
  val name: String,
  val uri: String,
  val type: TrackType,
  val duration: Long = 0L,
  val volume: Float = 1.0f,
  val muted: Boolean = false,
  val solo: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)
