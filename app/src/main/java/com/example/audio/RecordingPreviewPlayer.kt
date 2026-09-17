package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class RecordingPreviewState(
  val activeRecordingId: String? = null,
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val isMuted: Boolean = false,
  val isSolo: Boolean = false,
  val errorMessage: String? = null
)

/**
 * High-performance audio player for auditioning, scrubbing, and previewing vocal takes and idea memos.
 */
class RecordingPreviewPlayer(
  private val context: Context,
  private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var mediaPlayer: MediaPlayer? = null
  private var positionTickerJob: Job? = null
  private var audioFocusRequest: AudioFocusRequest? = null

  private val _state = MutableStateFlow(RecordingPreviewState())
  val state: StateFlow<RecordingPreviewState> = _state.asStateFlow()

  private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> pause()
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        val vol = if (_state.value.isMuted) 0f else 0.2f
        mediaPlayer?.setVolume(vol, vol)
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        val vol = if (_state.value.isMuted) 0f else 1.0f
        mediaPlayer?.setVolume(vol, vol)
      }
    }
  }

  fun playTake(recordingId: String, filePath: String) {
    if (_state.value.activeRecordingId == recordingId && mediaPlayer != null) {
      if (_state.value.isPlaying) {
        pause()
      } else {
        resume()
      }
      return
    }

    // Switch to new file
    stopInternal()

    val file = File(filePath)
    if (!file.exists() || file.length() == 0L) {
      _state.update {
        it.copy(
          activeRecordingId = recordingId,
          isPlaying = false,
          errorMessage = "Audio take file missing from storage"
        )
      }
      return
    }

    try {
      val player = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        )
        setDataSource(file.absolutePath)
        prepare()
      }

      val duration = player.duration.toLong().coerceAtLeast(0L)
      val vol = if (_state.value.isMuted) 0f else 1.0f
      player.setVolume(vol, vol)

      player.setOnCompletionListener {
        _state.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
        positionTickerJob?.cancel()
      }

      player.setOnErrorListener { _, what, extra ->
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        _state.update { it.copy(isPlaying = false, errorMessage = "Playback error: $what") }
        true
      }

      mediaPlayer = player

      val focusGranted = requestAudioFocus()
      if (focusGranted) {
        player.start()
        _state.update {
          it.copy(
            activeRecordingId = recordingId,
            isPlaying = true,
            durationMs = duration,
            currentPositionMs = 0L,
            errorMessage = null
          )
        }
        startPositionTicker()
      } else {
        _state.update {
          it.copy(
            activeRecordingId = recordingId,
            isPlaying = false,
            durationMs = duration,
            errorMessage = "Could not obtain audio focus"
          )
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error playing audio take: ${e.message}", e)
      _state.update {
        it.copy(
          activeRecordingId = recordingId,
          isPlaying = false,
          errorMessage = "Could not preview take: ${e.localizedMessage}"
        )
      }
    }
  }

  fun pause() {
    positionTickerJob?.cancel()
    try {
      if (mediaPlayer?.isPlaying == true) {
        mediaPlayer?.pause()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error pausing: ${e.message}")
    }
    _state.update { it.copy(isPlaying = false) }
  }

  fun resume() {
    val player = mediaPlayer ?: return
    val focusGranted = requestAudioFocus()
    if (focusGranted) {
      try {
        val vol = if (_state.value.isMuted) 0f else 1.0f
        player.setVolume(vol, vol)
        player.start()
        _state.update { it.copy(isPlaying = true) }
        startPositionTicker()
      } catch (e: Exception) {
        Log.e(TAG, "Error resuming: ${e.message}")
      }
    }
  }

  fun seekTo(positionMs: Long) {
    val player = mediaPlayer ?: return
    val target = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(0L))
    try {
      player.seekTo(target.toInt())
      _state.update { it.copy(currentPositionMs = target) }
    } catch (e: Exception) {
      Log.w(TAG, "Error seeking: ${e.message}")
    }
  }

  fun setMuted(muted: Boolean) {
    _state.update { it.copy(isMuted = muted) }
    val vol = if (muted) 0f else 1.0f
    try {
      mediaPlayer?.setVolume(vol, vol)
    } catch (ignored: Exception) {}
  }

  fun toggleSolo(recordingId: String) {
    _state.update {
      val isNowSolo = !(it.isSolo && it.activeRecordingId == recordingId)
      it.copy(isSolo = isNowSolo)
    }
  }

  private fun startPositionTicker() {
    positionTickerJob?.cancel()
    positionTickerJob = coroutineScope.launch {
      while (isActive && _state.value.isPlaying) {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
          val pos = player.currentPosition.toLong().coerceAtLeast(0L)
          _state.update { it.copy(currentPositionMs = pos) }
        }
        delay(40L)
      }
    }
  }

  private fun requestAudioFocus(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

      val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener(focusChangeListener)
        .build()

      audioFocusRequest = request
      audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(
        focusChangeListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN
      ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(focusChangeListener)
    }
  }

  private fun stopInternal() {
    positionTickerJob?.cancel()
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (ignored: Exception) {}
    mediaPlayer = null
  }

  fun release() {
    stopInternal()
    abandonAudioFocus()
    _state.update { RecordingPreviewState() }
  }

  companion object {
    private const val TAG = "RecordingPreviewPlayer"
  }
}
