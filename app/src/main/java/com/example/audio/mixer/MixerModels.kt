package com.example.audio.mixer

import java.io.File
import kotlin.math.abs

/**
 * Represents the configuration and state of a single channel strip in the mixer.
 */
data class TrackChannel(
  val id: String,
  val name: String,
  val filePath: String,
  val type: String, // "BEAT", "VOCAL", "RECORDING", "IMPORTED_AUDIO"
  val volume: Float = 1.0f,
  val isMuted: Boolean = false,
  val isSolo: Boolean = false,
  val durationMs: Long = 0L
) {
  val fileExists: Boolean
    get() = File(filePath).exists()
}

/**
 * Timeline status emitted during multi-track playback.
 */
data class MixerPlaybackState(
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val masterVolume: Float = 1.0f,
  val activeTrackCount: Int = 0,
  val soloActive: Boolean = false,
  val peakOutputLevel: Float = 0f, // 0.0f to 1.0f
  val isClippingPrevented: Boolean = false
)

/**
 * Result of audio mixing down a PCM frame buffer.
 */
class MixedAudioFrame(
  val buffer: ShortArray,
  val peakAmplitude: Float,
  val clippingDetectedAndPrevented: Boolean
)
