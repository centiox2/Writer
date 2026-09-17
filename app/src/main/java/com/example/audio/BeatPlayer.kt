package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class AudioOutputRoute {
  SPEAKER,
  WIRED_HEADSET,
  BLUETOOTH
}

data class BeatPlaybackState(
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val volume: Float = 1.0f,
  val isMuted: Boolean = false,
  val isLooping: Boolean = false,
  val isPrepared: Boolean = false,
  val isMissingFile: Boolean = false,
  val currentFilePath: String? = null,
  val audioOutputRoute: AudioOutputRoute = AudioOutputRoute.SPEAKER,
  val errorMessage: String? = null
)

class BeatPlayer(
  private val context: Context,
  private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private var mediaPlayer: MediaPlayer? = null
  private var positionTickerJob: Job? = null

  private val _playbackState = MutableStateFlow(BeatPlaybackState())
  val playbackState: StateFlow<BeatPlaybackState> = _playbackState.asStateFlow()

  // Audio Focus
  private var audioFocusRequest: AudioFocusRequest? = null
  private var wasPlayingBeforeTransientLoss = false

  // Audio Focus Listener
  private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        wasPlayingBeforeTransientLoss = false
        pause()
        abandonAudioFocus()
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
        if (_playbackState.value.isPlaying) {
          wasPlayingBeforeTransientLoss = true
          pause()
        }
      }
      AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
        // Duck volume
        val targetVol = if (_playbackState.value.isMuted) 0f else (_playbackState.value.volume * 0.25f)
        mediaPlayer?.setVolume(targetVol, targetVol)
      }
      AudioManager.AUDIOFOCUS_GAIN -> {
        // Restore volume
        val targetVol = if (_playbackState.value.isMuted) 0f else _playbackState.value.volume
        mediaPlayer?.setVolume(targetVol, targetVol)
        if (wasPlayingBeforeTransientLoss) {
          wasPlayingBeforeTransientLoss = false
          play()
        }
      }
    }
  }

  // Headphones / Bluetooth Disconnection (Becoming Noisy) Receiver
  private val becomingNoisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
      if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
        Log.i("BeatPlayer", "Audio becoming noisy (headphones/bluetooth unplugged). Pausing playback.")
        pause()
        updateAudioOutputRoute()
      }
    }
  }

  // Headset Plug / Bluetooth connection broadcast receiver
  private val audioDeviceReceiver = object : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
      updateAudioOutputRoute()
    }
  }

  private var isReceiversRegistered = false

  init {
    registerAudioReceivers()
    updateAudioOutputRoute()
  }

  private fun registerAudioReceivers() {
    if (isReceiversRegistered) return
    try {
      val noisyFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
      context.registerReceiver(becomingNoisyReceiver, noisyFilter)

      val deviceFilter = IntentFilter().apply {
        addAction(Intent.ACTION_HEADSET_PLUG)
        addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED")
        addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
      }
      context.registerReceiver(audioDeviceReceiver, deviceFilter)
      isReceiversRegistered = true
    } catch (e: Exception) {
      Log.w("BeatPlayer", "Failed to register audio receivers: ${e.message}")
    }
  }

  private fun unregisterAudioReceivers() {
    if (!isReceiversRegistered) return
    try {
      context.unregisterReceiver(becomingNoisyReceiver)
      context.unregisterReceiver(audioDeviceReceiver)
      isReceiversRegistered = false
    } catch (e: Exception) {
      Log.w("BeatPlayer", "Failed to unregister audio receivers: ${e.message}")
    }
  }

  fun updateAudioOutputRoute() {
    try {
      val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
      var route = AudioOutputRoute.SPEAKER

      for (device in devices) {
        when (device.type) {
          AudioDeviceInfo.TYPE_WIRED_HEADSET,
          AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
          AudioDeviceInfo.TYPE_USB_HEADSET -> {
            route = AudioOutputRoute.WIRED_HEADSET
            break
          }
          AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
          AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
            route = AudioOutputRoute.BLUETOOTH
          }
          else -> {}
        }
      }
      _playbackState.update { it.copy(audioOutputRoute = route) }
    } catch (e: Exception) {
      Log.w("BeatPlayer", "Could not query audio devices: ${e.message}")
    }
  }

  /**
   * Loads a beat file from a local path.
   * Gracefully handles missing files without crashing.
   */
  fun loadBeat(filePath: String, initialVolume: Float = 1.0f) {
    // If the file path is the same and already prepared, don't recreate
    if (_playbackState.value.currentFilePath == filePath && _playbackState.value.isPrepared && !_playbackState.value.isMissingFile) {
      return
    }

    val file = File(filePath)
    if (!file.exists() || file.length() == 0L) {
      Log.w("BeatPlayer", "Beat file not found on disk: $filePath")
      releasePlayer()
      _playbackState.update {
        it.copy(
          isMissingFile = true,
          isPrepared = false,
          isPlaying = false,
          currentFilePath = filePath,
          errorMessage = "Beat file missing from disk"
        )
      }
      return
    }

    releasePlayer()

    try {
      val player = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        )
        setDataSource(file.absolutePath)
        isLooping = _playbackState.value.isLooping
        prepare()
      }

      val duration = player.duration.toLong().coerceAtLeast(0L)
      val actualVol = if (_playbackState.value.isMuted) 0f else initialVolume.coerceIn(0f, 1f)
      player.setVolume(actualVol, actualVol)

      player.setOnCompletionListener {
        if (!_playbackState.value.isLooping) {
          pause()
          seekTo(0L)
        }
      }

      player.setOnErrorListener { _, what, extra ->
        Log.e("BeatPlayer", "MediaPlayer error: what=$what extra=$extra")
        _playbackState.update {
          it.copy(
            isPlaying = false,
            errorMessage = "Playback error (code: $what)"
          )
        }
        true
      }

      mediaPlayer = player
      _playbackState.update {
        it.copy(
          isPrepared = true,
          isMissingFile = false,
          isPlaying = false,
          durationMs = duration,
          currentPositionMs = 0L,
          volume = initialVolume,
          currentFilePath = filePath,
          errorMessage = null
        )
      }
    } catch (e: Exception) {
      Log.e("BeatPlayer", "Failed to load audio source: ${e.message}", e)
      _playbackState.update {
        it.copy(
          isPrepared = false,
          isMissingFile = true,
          isPlaying = false,
          currentFilePath = filePath,
          errorMessage = "Could not load audio file: ${e.localizedMessage}"
        )
      }
    }
  }

  fun play() {
    val state = _playbackState.value
    if (state.isMissingFile || !state.isPrepared) return

    val player = mediaPlayer ?: return
    val focusGranted = requestAudioFocus()

    if (focusGranted) {
      try {
        val vol = if (state.isMuted) 0f else state.volume
        player.setVolume(vol, vol)
        player.start()
        _playbackState.update { it.copy(isPlaying = true) }
        startPositionTicker()
      } catch (e: Exception) {
        Log.e("BeatPlayer", "Failed to start playback: ${e.message}")
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
      Log.w("BeatPlayer", "Error pausing player: ${e.message}")
    }
    _playbackState.update { it.copy(isPlaying = false) }
  }

  fun togglePlayPause() {
    if (_playbackState.value.isPlaying) {
      pause()
    } else {
      play()
    }
  }

  fun seekTo(positionMs: Long) {
    val player = mediaPlayer ?: return
    val duration = _playbackState.value.durationMs
    val target = positionMs.coerceIn(0L, duration.coerceAtLeast(0L))

    try {
      player.seekTo(target.toInt())
      _playbackState.update { it.copy(currentPositionMs = target) }
    } catch (e: Exception) {
      Log.w("BeatPlayer", "Error seeking player: ${e.message}")
    }
  }

  fun seekRelative(deltaMs: Long) {
    val current = _playbackState.value.currentPositionMs
    seekTo(current + deltaMs)
  }

  fun setVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    _playbackState.update { it.copy(volume = clamped) }
    if (!_playbackState.value.isMuted) {
      try {
        mediaPlayer?.setVolume(clamped, clamped)
      } catch (e: Exception) {
        Log.w("BeatPlayer", "Error setting volume: ${e.message}")
      }
    }
  }

  fun toggleMute() {
    val newMuted = !_playbackState.value.isMuted
    _playbackState.update { it.copy(isMuted = newMuted) }
    val vol = if (newMuted) 0f else _playbackState.value.volume
    try {
      mediaPlayer?.setVolume(vol, vol)
    } catch (e: Exception) {
      Log.w("BeatPlayer", "Error toggling mute: ${e.message}")
    }
  }

  fun toggleLoop() {
    val newLooping = !_playbackState.value.isLooping
    _playbackState.update { it.copy(isLooping = newLooping) }
    try {
      mediaPlayer?.isLooping = newLooping
    } catch (e: Exception) {
      Log.w("BeatPlayer", "Error toggling loop: ${e.message}")
    }
  }

  private fun startPositionTicker() {
    positionTickerJob?.cancel()
    positionTickerJob = coroutineScope.launch {
      while (isActive && _playbackState.value.isPlaying) {
        val player = mediaPlayer
        if (player != null && player.isPlaying) {
          val pos = player.currentPosition.toLong().coerceAtLeast(0L)
          _playbackState.update { it.copy(currentPositionMs = pos) }
        }
        delay(40L) // 25 updates per second for smooth waveform scrubbing
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
        .setAcceptsDelayedFocusGain(false)
        .setOnAudioFocusChangeListener(audioFocusChangeListener)
        .build()

      audioFocusRequest = request
      val res = audioManager.requestAudioFocus(request)
      res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
      @Suppress("DEPRECATION")
      val res = audioManager.requestAudioFocus(
        audioFocusChangeListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN
      )
      res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus(audioFocusChangeListener)
    }
  }

  private fun releasePlayer() {
    positionTickerJob?.cancel()
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (ignored: Exception) {}
    mediaPlayer = null
  }

  fun release() {
    pause()
    releasePlayer()
    abandonAudioFocus()
    unregisterAudioReceivers()
  }
}
