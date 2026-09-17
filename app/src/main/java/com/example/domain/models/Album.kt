package com.example.domain.models

data class Album(
  val id: String,
  val name: String,
  val description: String = "",
  val artworkUri: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val songCount: Int = 0
)
