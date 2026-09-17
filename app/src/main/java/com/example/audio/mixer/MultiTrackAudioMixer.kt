package com.example.audio.mixer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import kotlin.math.max

/**
 * High-performance, low-latency Multi-Track Audio Mixer Engine.
 *
 * Implements a shared timeline driving synchronized multi-track playback.
 *
 * Audio pipeline:
 * decode (streaming on-demand chunks)
 *   → 16-bit PCM
 *   → resample (linear interpolation to 44.1kHz stereo)
 *   → synchronize (aligned frame offsets across timeline)
 *   → track gain
 *   → mute/solo matrix
 *   → channel summation
 *   → master gain
 *   → soft-knee anti-clipping limiter (tanh saturation)
 *   → AudioTrack streaming output
 */
class MultiTrackAudioMixer(
  private val context: Context,
  private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

  // Channel strips managed by the mixer
  private val _tracks = MutableStateFlow<List<TrackChannel>>(emptyList())
  val tracks: StateFlow<List<TrackChannel>> = _tracks.asStateFlow()

  // Timeline playback state
  private val _playbackState = MutableStateFlow(MixerPlaybackState())
  val playbackState: StateFlow<MixerPlaybackState> = _playbackState.asStateFlow()

  // Active audio stream decoders keyed by track ID
  private val trackDecoders = HashMap<String, TrackStreamSource>()

  // Shared timeline state
  @Volatile private var isPlaying = false
  @Volatile private var currentPositionUs = 0L
  @Volatile private var totalDurationUs = 0L
  @Volatile private var masterVolume = 1.0f

  // AudioTrack hardware sink
  private var audioTrackSink: AudioTrack? = null
  private var playbackJob: Job? = null

  // Frames processed per mixing cycle (1024 frames = ~23.2ms at 44.1kHz stereo)
  private val bufferFrameSize = 1024
  private val sampleRate = AudioMixerDsp.STANDARD_SAMPLE_RATE
  private val channels = AudioMixerDsp.STANDARD_CHANNELS

  init {
    initAudioSink()
  }

  private fun initAudioSink() {
    try {
      val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      val bufferSize = max(minBufferSize, bufferFrameSize * channels * 4)

      val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

      val format = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .build()

      audioTrackSink = AudioTrack.Builder()
        .setAudioAttributes(attributes)
        .setAudioFormat(format)
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()
    } catch (e: Exception) {
      Log.w("MultiTrackAudioMixer", "AudioTrack sink init error (expected in local unit tests): ${e.message}")
    }
  }

  // ==========================================
  // Track Management
  // ==========================================

  @Synchronized
  fun setTracks(newTracks: List<TrackChannel>) {
    _tracks.value = newTracks

    // Close and remove decoders for tracks no longer present
    val currentIds = newTracks.map { it.id }.toSet()
    val iterator = trackDecoders.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      if (!currentIds.contains(entry.key)) {
        entry.value.close()
        iterator.remove()
      }
    }

    // Initialize decoders for new tracks and calculate overall timeline duration
    var maxDurationUs = 0L
    for (t in newTracks) {
      val file = File(t.filePath)
      if (file.exists() && !trackDecoders.containsKey(t.id)) {
        try {
          val decoder = TrackStreamSourceFactory.create(t.filePath)
          trackDecoders[t.id] = decoder
          if (decoder.durationUs > 0) {
            maxDurationUs = max(maxDurationUs, decoder.durationUs)
          }
        } catch (e: Exception) {
          Log.w("MultiTrackAudioMixer", "Failed to create decoder for track ${t.name}: ${e.message}")
        }
      } else if (trackDecoders.containsKey(t.id)) {
        val dur = trackDecoders[t.id]?.durationUs ?: 0L
        maxDurationUs = max(maxDurationUs, dur)
      } else if (t.durationMs > 0) {
        maxDurationUs = max(maxDurationUs, t.durationMs * 1000L)
      }
    }

    totalDurationUs = maxDurationUs
    val hasSolo = newTracks.any { it.isSolo }

    _playbackState.update {
      it.copy(
        durationMs = totalDurationUs / 1000L,
        activeTrackCount = newTracks.size,
        soloActive = hasSolo
      )
    }
  }

  fun addTrack(track: TrackChannel) {
    val current = _tracks.value.toMutableList()
    current.removeAll { it.id == track.id }
    current.add(track)
    setTracks(current)
  }

  fun removeTrack(trackId: String) {
    val current = _tracks.value.toMutableList()
    current.removeAll { it.id == trackId }
    setTracks(current)
  }

  fun setTrackVolume(trackId: String, volume: Float) {
    _tracks.update { list ->
      list.map { if (it.id == trackId) it.copy(volume = volume.coerceIn(0f, 2.0f)) else it }
    }
  }

  fun toggleTrackMute(trackId: String) {
    _tracks.update { list ->
      list.map { if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it }
    }
  }

  fun toggleTrackSolo(trackId: String) {
    _tracks.update { list ->
      val updated = list.map { if (it.id == trackId) it.copy(isSolo = !it.isSolo) else it }
      val anySolo = updated.any { it.isSolo }
      _playbackState.update { it.copy(soloActive = anySolo) }
      updated
    }
  }

  fun setMasterVolume(volume: Float) {
    masterVolume = volume.coerceIn(0f, 2.0f)
    _playbackState.update { it.copy(masterVolume = masterVolume) }
  }

  // ==========================================
  // Timeline Playback Controls
  // ==========================================

  @Synchronized
  fun play() {
    if (isPlaying) return
    if (_tracks.value.isEmpty()) return

    isPlaying = true
    _playbackState.update { it.copy(isPlaying = true) }

    try {
      audioTrackSink?.play()
    } catch (e: Exception) {
      Log.w("MultiTrackAudioMixer", "audioTrackSink play error: ${e.message}")
    }

    playbackJob?.cancel()
    playbackJob = coroutineScope.launch {
      runMixingLoop()
    }
  }

  @Synchronized
  fun pause() {
    isPlaying = false
    playbackJob?.cancel()
    playbackJob = null
    try {
      audioTrackSink?.pause()
    } catch (e: Exception) {
      Log.w("MultiTrackAudioMixer", "audioTrackSink pause error: ${e.message}")
    }
    _playbackState.update { it.copy(isPlaying = false) }
  }

  @Synchronized
  fun stop() {
    pause()
    seekTo(0L)
  }

  @Synchronized
  fun seekTo(positionMs: Long) {
    val boundedPosMs = positionMs.coerceIn(0L, max(1L, totalDurationUs / 1000L))
    currentPositionUs = boundedPosMs * 1000L

    // Seek all track decoders in sync to the exact same timeline timestamp
    for ((_, decoder) in trackDecoders) {
      try {
        decoder.seekTo(currentPositionUs)
      } catch (e: Exception) {
        Log.w("MultiTrackAudioMixer", "Seek error for decoder: ${e.message}")
      }
    }

    try {
      audioTrackSink?.pause()
      audioTrackSink?.flush()
      if (isPlaying) {
        audioTrackSink?.play()
      }
    } catch (ignored: Exception) {}

    _playbackState.update { it.copy(currentPositionMs = boundedPosMs) }
  }

  // ==========================================
  // Shared Timeline Audio Mixing Loop
  // ==========================================

  private suspend fun runMixingLoop() {
    val frameCount = bufferFrameSize
    val sampleCount = frameCount * channels
    val frameDurationUs = ((frameCount.toLong() * 1_000_000L) / sampleRate)

    while (isPlaying && coroutineScope.isActive) {
      val currentTracks = _tracks.value
      if (currentTracks.isEmpty()) {
        pause()
        break
      }

      val anySolo = currentTracks.any { it.isSolo }
      val channelFloatBuffers = ArrayList<FloatArray>()

      // 1. Read PCM frames synchronously from each active track stream
      for (track in currentTracks) {
        val decoder = trackDecoders[track.id]
        val isAudible = AudioMixerDsp.isTrackAudible(track.isMuted, track.isSolo, anySolo)

        if (decoder != null && isAudible) {
          // Streaming decode on-demand without loading full file into memory
          val rawPcm = decoder.readNextFrames(frameCount)
          val processedBuffer = if (rawPcm.size < sampleCount) {
            // Pad end with silence if track is shorter than timeline or reached EOS
            val padded = ShortArray(sampleCount)
            System.arraycopy(rawPcm, 0, padded, 0, rawPcm.size)
            padded
          } else {
            rawPcm
          }
          val floatGained = AudioMixerDsp.applyGain(processedBuffer, track.volume, isAudible = true)
          channelFloatBuffers.add(floatGained)
        }
      }

      // 2. Mixdown channels, apply master gain, and run anti-clipping soft limiter
      val mixedFrame = AudioMixerDsp.mixAndLimit(
        channelBuffers = channelFloatBuffers,
        frameSize = sampleCount,
        masterGain = masterVolume
      )

      // 3. Write mixed 16-bit PCM to AudioTrack hardware sink
      audioTrackSink?.let { sink ->
        try {
          sink.write(mixedFrame.buffer, 0, mixedFrame.buffer.size)
        } catch (e: Exception) {
          Log.w("MultiTrackAudioMixer", "AudioTrack write error: ${e.message}")
        }
      }

      // 4. Advance shared master timeline clock
      currentPositionUs += frameDurationUs
      val currentMs = currentPositionUs / 1000L

      // Check for completion of entire project timeline
      if (totalDurationUs > 0 && currentPositionUs >= totalDurationUs) {
        seekTo(0L)
        pause()
        break
      }

      // Update state for UI visualizers (metering, progress, clipping warning)
      _playbackState.update {
        it.copy(
          currentPositionMs = currentMs,
          peakOutputLevel = mixedFrame.peakAmplitude,
          isClippingPrevented = mixedFrame.clippingDetectedAndPrevented
        )
      }

      // Yield control slightly so UI thread and coroutines remain responsive
      delay(5)
    }
  }

  fun release() {
    pause()
    for ((_, decoder) in trackDecoders) {
      decoder.close()
    }
    trackDecoders.clear()
    try {
      audioTrackSink?.release()
    } catch (ignored: Exception) {}
    audioTrackSink = null
  }
}
