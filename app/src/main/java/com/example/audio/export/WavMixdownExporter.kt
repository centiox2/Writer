package com.example.audio.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.audio.mixer.AudioMixerDsp
import com.example.audio.mixer.TrackChannel
import com.example.audio.mixer.TrackStreamSource
import com.example.audio.mixer.TrackStreamSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Status and progress emitted during the mixdown rendering process.
 */
data class MixdownProgress(
  val fraction: Float = 0f, // 0.0f to 1.0f
  val currentPositionMs: Long = 0L,
  val totalDurationMs: Long = 0L,
  val isClippingDetected: Boolean = false,
  val peakLevel: Float = 0f,
  val statusMessage: String = ""
)

/**
 * Result of a completed mixdown export.
 */
data class MixdownExportResult(
  val success: Boolean,
  val file: File? = null,
  val contentUri: Uri? = null,
  val durationMs: Long = 0L,
  val totalBytesWritten: Long = 0L,
  val clippingPrevented: Boolean = false,
  val validation: WavValidationResult? = null,
  val errorMessage: String? = null
)

/**
 * High-performance, non-blocking Multi-Track WAV Mixdown Exporter.
 *
 * Renders all active track channels with individual volumes, mute/solo matrix,
 * master gain, and hyperbolic tangent soft-limiting into a standard 44.1kHz 16-bit
 * Stereo WAV file.
 */
class WavMixdownExporter(
  private val context: Context
) {

  companion object {
    private const val TAG = "WavMixdownExporter"
    private const val EXPORT_DIR_NAME = "exports"
    private const val CHUNK_FRAMES = 2048 // 2048 stereo frames = 4096 samples = ~46.4ms
  }

  /**
   * Mixes tracks down into a local cache WAV file, reporting progress through a cold Flow.
   * Runs entirely on Dispatchers.IO to never block the main UI thread.
   */
  fun exportMixdownFlow(
    tracks: List<TrackChannel>,
    masterVolume: Float = 1.0f,
    outputFileName: String = "mixdown_${System.currentTimeMillis()}.wav"
  ): Flow<MixdownProgress> = flow {
    emit(MixdownProgress(fraction = 0f, statusMessage = "Preparing audio streams..."))

    val isAnyTrackSolo = tracks.any { it.isSolo }
    val audibleTracks = tracks.filter { track ->
      track.fileExists && AudioMixerDsp.isTrackAudible(track.isMuted, track.isSolo, isAnyTrackSolo)
    }

    if (audibleTracks.isEmpty()) {
      throw IllegalStateException("No audible or valid audio tracks to mix.")
    }

    // Ensure exports directory exists
    val exportDir = File(context.cacheDir, EXPORT_DIR_NAME).apply { mkdirs() }
    val outputFile = File(exportDir, outputFileName)
    if (outputFile.exists()) {
      outputFile.delete()
    }

    val decoders = HashMap<String, TrackStreamSource>()
    var maxDurationUs = 0L

    try {
      // 1. Initialize decoders for all audible tracks
      for (track in audibleTracks) {
        val decoder = TrackStreamSourceFactory.create(track.filePath)
        decoders[track.id] = decoder
        maxDurationUs = max(maxDurationUs, decoder.durationUs)
      }

      if (maxDurationUs <= 0L) {
        // Fallback to track data model durations
        val maxModelDurationMs = audibleTracks.maxOfOrNull { it.durationMs } ?: 0L
        maxDurationUs = maxModelDurationMs * 1000L
      }

      if (maxDurationUs <= 0L) {
        maxDurationUs = 5_000_000L // default minimum 5s if unknown
      }

      val totalDurationMs = maxDurationUs / 1000L
      val sampleRate = AudioMixerDsp.STANDARD_SAMPLE_RATE
      val channels = AudioMixerDsp.STANDARD_CHANNELS
      val totalFramesToRender = (maxDurationUs * sampleRate) / 1_000_000L

      emit(MixdownProgress(fraction = 0.05f, totalDurationMs = totalDurationMs, statusMessage = "Rendering multi-track mix..."))

      var totalAudioBytesWritten = 0L
      var anyClippingPrevented = false
      var highestPeak = 0f

      // Write PCM data to temp file with a standard 44-byte WAV header placeholder
      FileOutputStream(outputFile).buffered(64 * 1024).use { fos ->
        // Write initial header placeholder
        WavWriter.writeHeader(fos, sampleRate, channels, 16, 0L)

        var framesRendered = 0L
        var lastEmitMs = System.currentTimeMillis()

        while (framesRendered < totalFramesToRender) {
          // Check for coroutine cancellation
          currentCoroutineContext().ensureActive()

          val framesInChunk = min(CHUNK_FRAMES.toLong(), totalFramesToRender - framesRendered).toInt()
          val neededSamples = framesInChunk * channels

          // Collect decoded audio float buffers from each track
          val trackFloatBuffers = ArrayList<FloatArray>(audibleTracks.size)

          for (track in audibleTracks) {
            val decoder = decoders[track.id]
            val rawShorts = decoder?.readNextFrames(framesInChunk) ?: ShortArray(0)

            val paddedShorts = if (rawShorts.size < neededSamples) {
              val padded = ShortArray(neededSamples)
              System.arraycopy(rawShorts, 0, padded, 0, rawShorts.size)
              padded
            } else {
              rawShorts
            }

            val gainedFloats = AudioMixerDsp.applyGain(
              buffer = paddedShorts,
              gain = track.volume,
              isAudible = true
            )
            trackFloatBuffers.add(gainedFloats)
          }

          // Mix, apply master gain, and apply soft-knee anti-clipping limiter
          val mixedFrame = AudioMixerDsp.mixAndLimit(
            channelBuffers = trackFloatBuffers,
            frameSize = neededSamples,
            masterGain = masterVolume
          )

          if (mixedFrame.clippingDetectedAndPrevented) {
            anyClippingPrevented = true
          }
          if (mixedFrame.peakAmplitude > highestPeak) {
            highestPeak = mixedFrame.peakAmplitude
          }

          // Write 16-bit PCM bytes (little-endian)
          val pcmBytes = WavWriter.shortsToLittleEndianBytes(mixedFrame.buffer)
          fos.write(pcmBytes)
          totalAudioBytesWritten += pcmBytes.size
          framesRendered += framesInChunk

          val currentPositionMs = (framesRendered * 1000L) / sampleRate
          val progressFraction = (framesRendered.toFloat() / totalFramesToRender.toFloat()).coerceIn(0f, 1f)

          // Throttle UI emissions to ~30-50ms for smooth rendering
          val now = System.currentTimeMillis()
          if (now - lastEmitMs >= 40 || framesRendered >= totalFramesToRender) {
            lastEmitMs = now
            emit(
              MixdownProgress(
                fraction = progressFraction,
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs,
                isClippingDetected = anyClippingPrevented,
                peakLevel = highestPeak,
                statusMessage = "Mixing tracks (${(progressFraction * 100).toInt()}%)"
              )
            )
          }
        }
        fos.flush()
      }

      // Finalize WAV Header with exact data size
      WavWriter.finalizeWavHeader(outputFile, totalAudioBytesWritten)

      emit(MixdownProgress(fraction = 0.98f, totalDurationMs = totalDurationMs, statusMessage = "Validating exported WAV file..."))

      // Validate the exported file
      val validation = WavValidator.validate(outputFile)
      if (!validation.isValid) {
        throw IllegalStateException("WAV validation failed: ${validation.errorMessage}")
      }

      emit(
        MixdownProgress(
          fraction = 1.0f,
          currentPositionMs = totalDurationMs,
          totalDurationMs = totalDurationMs,
          isClippingDetected = anyClippingPrevented,
          peakLevel = highestPeak,
          statusMessage = "Mixdown complete!"
        )
      )
    } catch (c: CancellationException) {
      // Clean up partial file on cancellation
      if (outputFile.exists()) {
        outputFile.delete()
      }
      throw c
    } catch (e: Exception) {
      if (outputFile.exists()) {
        outputFile.delete()
      }
      throw e
    } finally {
      // Always close all decoders
      for (decoder in decoders.values) {
        try {
          decoder.close()
        } catch (ignored: Exception) {}
      }
      decoders.clear()
    }
  }.flowOn(Dispatchers.IO)

  /**
   * Executes mixdown export to a cached file.
   */
  suspend fun exportMixToFile(
    tracks: List<TrackChannel>,
    masterVolume: Float = 1.0f,
    fileName: String = "Mix_${System.currentTimeMillis()}.wav",
    onProgress: (MixdownProgress) -> Unit = {}
  ): MixdownExportResult = withContext(Dispatchers.IO) {
    try {
      var latestProgress = MixdownProgress()
      exportMixdownFlow(tracks, masterVolume, fileName).collect { progress ->
        latestProgress = progress
        onProgress(progress)
      }

      val exportDir = File(context.cacheDir, EXPORT_DIR_NAME)
      val file = File(exportDir, fileName)
      val validation = WavValidator.validate(file)

      val contentUri = getShareableUri(file)

      MixdownExportResult(
        success = validation.isValid,
        file = file,
        contentUri = contentUri,
        durationMs = validation.durationMs,
        totalBytesWritten = file.length(),
        clippingPrevented = latestProgress.isClippingDetected,
        validation = validation,
        errorMessage = validation.errorMessage
      )
    } catch (e: CancellationException) {
      MixdownExportResult(
        success = false,
        errorMessage = "Export was cancelled."
      )
    } catch (e: Exception) {
      Log.e(TAG, "Mixdown export failed: ${e.message}", e)
      MixdownExportResult(
        success = false,
        errorMessage = e.message ?: "Unknown export error occurred"
      )
    }
  }

  /**
   * Exports the mixdown directly to a SAF destination Uri selected by user via CreateDocument.
   */
  suspend fun exportMixToSafUri(
    safDestinationUri: Uri,
    tracks: List<TrackChannel>,
    masterVolume: Float = 1.0f,
    onProgress: (MixdownProgress) -> Unit = {}
  ): MixdownExportResult = withContext(Dispatchers.IO) {
    // 1. First render to temporary cache file for reliable header finalization & validation
    val tempFileName = "temp_export_${System.currentTimeMillis()}.wav"
    val result = exportMixToFile(tracks, masterVolume, tempFileName, onProgress)

    if (!result.success || result.file == null) {
      return@withContext result
    }

    try {
      // 2. Stream rendered verified WAV to SAF destination Uri
      context.contentResolver.openOutputStream(safDestinationUri)?.use { outputStream ->
        FileInputStream(result.file).use { inputStream ->
          inputStream.copyTo(outputStream)
        }
      } ?: throw IllegalStateException("Unable to open output stream for destination URI")

      // Return result with SAF URI
      MixdownExportResult(
        success = true,
        file = result.file,
        contentUri = safDestinationUri,
        durationMs = result.durationMs,
        totalBytesWritten = result.totalBytesWritten,
        clippingPrevented = result.clippingPrevented,
        validation = result.validation
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed writing to SAF URI: ${e.message}", e)
      MixdownExportResult(
        success = false,
        errorMessage = "Failed writing to selected storage location: ${e.message}"
      )
    }
  }

  /**
   * Creates an Android Share Sheet Intent for an exported mix file.
   */
  fun createShareIntent(file: File, songTitle: String = "Song Mix"): Intent {
    val uri = getShareableUri(file)
    return Intent(Intent.ACTION_SEND).apply {
      type = "audio/wav"
      if (uri != null) {
        putExtra(Intent.EXTRA_STREAM, uri)
      }
      putExtra(Intent.EXTRA_SUBJECT, "$songTitle - WAV Mix")
      putExtra(Intent.EXTRA_TEXT, "Here is the master WAV mix for \"$songTitle\".")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
  }

  /**
   * Generates a secure content URI using FileProvider with fallback.
   */
  fun getShareableUri(file: File): Uri? {
    return try {
      val authority = "${context.packageName}.fileprovider"
      FileProvider.getUriForFile(context, authority, file)
    } catch (e: Exception) {
      Log.w(TAG, "FileProvider URI resolution fallback: ${e.message}")
      try {
        Uri.fromFile(file)
      } catch (ignored: Exception) {
        null
      }
    }
  }
}
