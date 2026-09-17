package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteOrder
import kotlin.math.abs

class WaveformAnalyzer(private val context: Context) {

  // Memory LruCache: holds up to 40 analyzed waveforms in RAM for instantaneous (0ms) access
  private val memoryCache = LruCache<String, FloatArray>(40)

  private val cacheDir: File
    get() = File(context.cacheDir, "waveforms").apply { if (!exists()) mkdirs() }

  /**
   * Asynchronously extracts real normalized audio amplitude peaks into a fixed number of buckets.
   * Leverages two-level caching (RAM LruCache and disk cache) so subsequent requests return instantly.
   * Large files are handled via stride-sampling during decoding to minimize CPU & memory overhead.
   */
  suspend fun extractWaveform(
    filePath: String,
    targetBuckets: Int = 120
  ): FloatArray = withContext(Dispatchers.Default) {
    val file = File(filePath)
    if (!file.exists() || file.length() == 0L) {
      return@withContext FloatArray(targetBuckets) { 0.1f }
    }

    val cacheKey = "${file.name}_${file.length()}_${file.lastModified()}_$targetBuckets"

    // 1. Check in-memory LRU cache
    memoryCache.get(cacheKey)?.let { return@withContext it }

    // 2. Check disk cache
    val diskCacheFile = File(cacheDir, "${cacheKey.hashCode()}.wf")
    if (diskCacheFile.exists() && diskCacheFile.length() > 0) {
      try {
        val text = diskCacheFile.readText()
        val parsed = text.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
        if (parsed.size == targetBuckets) {
          memoryCache.put(cacheKey, parsed)
          return@withContext parsed
        }
      } catch (e: Exception) {
        Log.w("WaveformAnalyzer", "Error reading disk cache for $filePath: ${e.message}")
      }
    }

    // 3. Perform real audio decoding & amplitude analysis
    val waveform = decodeAudioAmplitudes(file, targetBuckets)

    // Store in memory cache
    memoryCache.put(cacheKey, waveform)

    // Persist to disk cache
    try {
      diskCacheFile.writeText(waveform.joinToString(","))
    } catch (e: Exception) {
      Log.w("WaveformAnalyzer", "Failed to write disk cache for $filePath: ${e.message}")
    }

    waveform
  }

  /**
   * Decodes audio file using Android MediaExtractor and MediaCodec to extract real PCM amplitudes.
   */
  private fun decodeAudioAmplitudes(file: File, targetBuckets: Int): FloatArray {
    val bucketPeaks = FloatArray(targetBuckets) { 0f }
    var extractor: MediaExtractor? = null
    var codec: MediaCodec? = null

    try {
      extractor = MediaExtractor()
      extractor.setDataSource(file.absolutePath)

      var audioTrackIndex = -1
      var format: MediaFormat? = null
      var mime = ""
      var durationUs = 0L

      for (i in 0 until extractor.trackCount) {
        val trackFormat = extractor.getTrackFormat(i)
        val trackMime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
        if (trackMime.startsWith("audio/")) {
          audioTrackIndex = i
          format = trackFormat
          mime = trackMime
          if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
            durationUs = trackFormat.getLong(MediaFormat.KEY_DURATION)
          }
          break
        }
      }

      if (audioTrackIndex < 0 || format == null) {
        return fallbackWaveformFromBytes(file, targetBuckets)
      }

      extractor.selectTrack(audioTrackIndex)

      if (durationUs <= 0L) {
        // Fallback default duration
        durationUs = 60_000_000L
      }

      codec = MediaCodec.createDecoderByType(mime)
      codec.configure(format, null, null, 0)
      codec.start()

      val bufferInfo = MediaCodec.BufferInfo()
      var isExtractorEOS = false
      var isCodecEOS = false
      val timeoutUs = 5000L

      while (!isCodecEOS) {
        // Feed encoded audio data to MediaCodec
        if (!isExtractorEOS) {
          val inIndex = codec.dequeueInputBuffer(timeoutUs)
          if (inIndex >= 0) {
            val inputBuffer = codec.getInputBuffer(inIndex)
            if (inputBuffer != null) {
              val sampleSize = extractor.readSampleData(inputBuffer, 0)
              if (sampleSize < 0) {
                codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isExtractorEOS = true
              } else {
                val sampleTime = extractor.sampleTime
                codec.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                extractor.advance()
              }
            }
          }
        }

        // Dequeue decoded 16-bit PCM output
        val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
        if (outIndex >= 0) {
          if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            isCodecEOS = true
          }

          val outBuffer = codec.getOutputBuffer(outIndex)
          if (outBuffer != null && bufferInfo.size > 0) {
            outBuffer.position(bufferInfo.offset)
            outBuffer.limit(bufferInfo.offset + bufferInfo.size)
            outBuffer.order(ByteOrder.LITTLE_ENDIAN)

            val shortBuffer = outBuffer.asShortBuffer()
            val sampleCount = shortBuffer.remaining()
            val presentationUs = bufferInfo.presentationTimeUs

            val bucket = if (durationUs > 0) {
              ((presentationUs.toDouble() / durationUs) * targetBuckets).toInt().coerceIn(0, targetBuckets - 1)
            } else {
              0
            }

            // Sample with stride for large files to keep decoding blazing fast
            val stride = if (sampleCount > 2048) 4 else 1
            var maxInChunk = 0f
            var i = 0
            while (i < sampleCount) {
              val sample = shortBuffer.get(i)
              val amp = abs(sample.toFloat()) / 32768f
              if (amp > maxInChunk) maxInChunk = amp
              i += stride
            }

            if (maxInChunk > bucketPeaks[bucket]) {
              bucketPeaks[bucket] = maxInChunk
            }
          }
          codec.releaseOutputBuffer(outIndex, false)
        } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
          if (isExtractorEOS) {
            break
          }
        }
      }
    } catch (e: Throwable) {
      Log.w("WaveformAnalyzer", "MediaCodec extraction encountered issue: ${e.message}. Using byte-level fallback.")
      return fallbackWaveformFromBytes(file, targetBuckets)
    } finally {
      try { codec?.stop() } catch (ignored: Exception) {}
      try { codec?.release() } catch (ignored: Exception) {}
      try { extractor?.release() } catch (ignored: Exception) {}
    }

    return normalizeWaveform(bucketPeaks)
  }

  /**
   * Fallback for direct byte reading (used for raw WAV files or in JVM/Robolectric test environments).
   */
  private fun fallbackWaveformFromBytes(file: File, targetBuckets: Int): FloatArray {
    val peaks = FloatArray(targetBuckets) { 0.12f }
    try {
      val length = file.length()
      if (length <= 44) return peaks
      val skipHeader = if (file.name.endsWith(".wav", ignoreCase = true)) 44L else 0L

      FileInputStream(file).use { stream ->
        stream.skip(skipHeader)
        val dataLen = length - skipHeader
        val bucketByteSize = (dataLen / targetBuckets).coerceAtLeast(2L)
        val buffer = ByteArray(2048)

        for (b in 0 until targetBuckets) {
          val readBytes = stream.read(buffer, 0, minOf(buffer.size.toLong(), bucketByteSize).toInt())
          if (readBytes <= 0) break
          var maxSample = 0
          for (i in 0 until readBytes - 1 step 2) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val signedSample = abs(sample.toShort().toInt())
            if (signedSample > maxSample) maxSample = signedSample
          }
          peaks[b] = (maxSample.toFloat() / 32768f).coerceIn(0f, 1f)
          val remaining = bucketByteSize - readBytes
          if (remaining > 0) {
            stream.skip(remaining)
          }
        }
      }
    } catch (e: Exception) {
      Log.w("WaveformAnalyzer", "Byte fallback failed: ${e.message}")
    }
    return normalizeWaveform(peaks)
  }

  /**
   * Normalizes the peak amplitudes so they scale visually across 0.08f to 0.95f without clipping.
   */
  private fun normalizeWaveform(peaks: FloatArray): FloatArray {
    val max = peaks.maxOrNull() ?: 1f
    val norm = if (max > 0.05f) 0.95f / max else 1f
    for (i in peaks.indices) {
      peaks[i] = (peaks[i] * norm).coerceIn(0.08f, 1.0f)
    }
    return peaks
  }

  fun clearMemoryCache() {
    memoryCache.evictAll()
  }
}
