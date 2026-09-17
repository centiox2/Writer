package com.example.domain.models

enum class TrackType {
  BEAT,
  VOCAL,
  RECORDING,
  GUIDE,
  INSTRUMENTAL,
  IMPORTED_AUDIO;

  companion object {
    fun fromString(value: String): TrackType {
      return entries.find { it.name.equals(value, ignoreCase = true) } ?: RECORDING
    }
  }
}
