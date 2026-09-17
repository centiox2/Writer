package com.example.domain.models

data class Recording(
  val id: String,
  val songId: String,
  val name: String,
  val uri: String,
  val duration: Long = 0L,
  val createdAt: Long = System.currentTimeMillis()
)
