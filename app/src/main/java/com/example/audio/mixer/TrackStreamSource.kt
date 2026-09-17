package com.example.audio.mixer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Interface representing a streamable audio source that can read decoded 16-bit PCM chunks
 * on demand without loading the entire file into memory.
 */
interface TrackStreamSource {
  val filePath: String
  val durationUs: Long
  val sampleRate: Int
  val channelCount: Int

  /**
   * Seeks the stream read head to presentation timestamp in microseconds.
   */
  fun seekTo(positionUs: Long)

  /**
   * Reads up to maxFrames of stereo 44.1kHz PCM samples.
   * Returns a ShortArray of interleaved stereo samples (size = readFrames * 2).
   */
  fun readNextFrames(maxFrames: Int): ShortArray

  /**
   * Releases underlying media codecs and file handles.
   */
  fun close()
}

/**
 * Streaming decoder for WAV files (direct buffered stream read with zero RAM bloat).
 */
class WavStreamSource(
  override val filePath: String
) : TrackStreamSource {

  override var durationUs: Long = 0L
    private set
  override var sampleRate: Int = 44100
    private set
  override var channelCount: Int = 2
    private set

  private val file = File(filePath)
  private var fis: FileInputStream? = null
  private var dataStartOffset: Long = 44L
  private var dataSizeBytes: Long = 0L
  private var bytesPerFrame: Int = 4
  private var currentFileBytePos: Long = 44L

  init {
    openAndParseHeader()
  }

  private fun openAndParseHeader() {
    if (!file.exists() || file.length() <= 44) return

    try {
      fis = FileInputStream(file)
      val header = ByteArray(44)
      val read = fis?.read(header) ?: 0
      if (read >= 44) {
        // Parse sample rate (offset 24, 4 bytes little endian)
        sampleRate = (header[24].toInt() and 0xFF) or
            ((header[25].toInt() and 0xFF) shl 8) or
            ((header[26].toInt() and 0xFF) shl 16) or
            ((header[27].toInt() and 0xFF) shl 24)
        if (sampleRate <= 0) sampleRate = 44100

        // Parse channels (offset 22, 2 bytes little endian)
        channelCount = (header[22].toInt() and 0xFF) or ((header[23].toInt() and 0xFF) shl 8)
        if (channelCount <= 0) channelCount = 2

        val bitsPerSample = (header[34].toInt() and 0xFF) or ((header[35].toInt() and 0xFF) shl 8)
        val bytesPerSample = if (bitsPerSample > 0) bitsPerSample / 8 else 2
        bytesPerFrame = channelCount * bytesPerSample

        dataStartOffset = 44L
        dataSizeBytes = file.length() - dataStartOffset
        val totalFrames = if (bytesPerFrame > 0) dataSizeBytes / bytesPerFrame else 0
        durationUs = if (sampleRate > 0) (totalFrames * 1_000_000L) / sampleRate else 0L

        currentFileBytePos = dataStartOffset
      }
    } catch (e: Exception) {
      Log.w("WavStreamSource", "Error parsing WAV header: ${e.message}")
    }
  }

  override fun seekTo(positionUs: Long) {
    if (sampleRate <= 0 || bytesPerFrame <= 0) return
    val targetFrame = (positionUs * sampleRate) / 1_000_000L
    val targetOffset = dataStartOffset + (targetFrame * bytesPerFrame)
    currentFileBytePos = targetOffset.coerceIn(dataStartOffset, file.length())

    try {
      fis?.close()
      fis = FileInputStream(file)
      fis?.channel?.position(currentFileBytePos)
    } catch (e: Exception) {
      Log.w("WavStreamSource", "Error seeking WAV: ${e.message}")
    }
  }

  override fun readNextFrames(maxFrames: Int): ShortArray {
    if (fis == null || currentFileBytePos >= file.length()) {
      return ShortArray(0)
    }

    val targetBytes = maxFrames * bytesPerFrame
    val rawBytes = ByteArray(targetBytes)
    var bytesRead = 0

    try {
      bytesRead = fis?.read(rawBytes) ?: 0
      if (bytesRead > 0) {
        currentFileBytePos += bytesRead
      }
    } catch (e: Exception) {
      Log.w("WavStreamSource", "Read error: ${e.message}")
    }

    if (bytesRead <= 0) return ShortArray(0)

    val validSamples = bytesRead / 2
    val shorts = ShortArray(validSamples)
    for (i in 0 until validSamples) {
      val low = rawBytes[i * 2].toInt() and 0xFF
      val high = rawBytes[i * 2 + 1].toInt()
      shorts[i] = ((high shl 8) or low).toShort()
    }

    // 1. Convert to Stereo if mono
    val stereoShorts = if (channelCount == 1) {
      AudioMixerDsp.monoToStereo(shorts)
    } else {
      shorts
    }

    // 2. Resample to standard 44.1kHz if needed
    return if (sampleRate != AudioMixerDsp.STANDARD_SAMPLE_RATE) {
      AudioMixerDsp.resampleLinear(
        stereoShorts,
        sampleRate,
        AudioMixerDsp.STANDARD_SAMPLE_RATE,
        channels = 2
      )
    } else {
      stereoShorts
    }
  }

  override fun close() {
    try {
      fis?.close()
    } catch (ignored: Exception) {}
    fis = null
  }
}

/**
 * Streaming decoder for compressed audio formats (MP3, M4A/AAC, OGG, FLAC)
 * using MediaExtractor and MediaCodec in on-demand chunk streaming mode.
 */
class MediaCodecStreamSource(
  override val filePath: String
) : TrackStreamSource {

  override var durationUs: Long = 0L
    private set
  override var sampleRate: Int = 44100
    private set
  override var channelCount: Int = 2
    private set

  private val file = File(filePath)
  private var extractor: MediaExtractor? = null
  private var codec: MediaCodec? = null
  private val bufferInfo = MediaCodec.BufferInfo()
  private var isExtractorEOS = false
  private var isCodecEOS = false
  private var audioTrackIndex = -1
  private var mime = ""
  private var mediaFormat: MediaFormat? = null

  // Rolling PCM accumulator for frames decoded ahead
  private val outputQueue = ArrayList<Short>()

  init {
    initDecoder()
  }

  private fun initDecoder() {
    if (!file.exists() || file.length() == 0L) return

    try {
      extractor = MediaExtractor()
      extractor?.setDataSource(file.absolutePath)

      val count = extractor?.trackCount ?: 0
      for (i in 0 until count) {
        val format = extractor?.getTrackFormat(i)
        val formatMime = format?.getString(MediaFormat.KEY_MIME) ?: ""
        if (format != null && formatMime.startsWith("audio/")) {
          audioTrackIndex = i
          mediaFormat = format
          mime = formatMime
          sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          } else {
            44100
          }
          channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          } else {
            2
          }
          durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
            format.getLong(MediaFormat.KEY_DURATION)
          } else {
            0L
          }
          break
        }
      }

      if (audioTrackIndex >= 0 && mediaFormat != null) {
        extractor?.selectTrack(audioTrackIndex)
        codec = MediaCodec.createDecoderByType(mime)
        codec?.configure(mediaFormat, null, null, 0)
        codec?.start()
      }
    } catch (e: Exception) {
      Log.w("MediaCodecStreamSource", "Init error for $filePath: ${e.message}")
    }
  }

  override fun seekTo(positionUs: Long) {
    outputQueue.clear()
    try {
      extractor?.seekTo(positionUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
      codec?.flush()
      isExtractorEOS = false
      isCodecEOS = false
    } catch (e: Exception) {
      Log.w("MediaCodecStreamSource", "Seek error: ${e.message}")
    }
  }

  override fun readNextFrames(maxFrames: Int): ShortArray {
    val neededSamples = maxFrames * 2 // Interleaved stereo

    // Decode until we have enough samples or reach EOS
    while (outputQueue.size < neededSamples && !isCodecEOS) {
      val decodedMore = decodeNextChunk()
      if (!decodedMore && isCodecEOS) break
    }

    if (outputQueue.isEmpty()) return ShortArray(0)

    val countToReturn = min(neededSamples, outputQueue.size)
    val result = ShortArray(countToReturn)
    for (i in 0 until countToReturn) {
      result[i] = outputQueue[i]
    }
    // Remove retrieved samples
    outputQueue.subList(0, countToReturn).clear()
    return result
  }

  private fun decodeNextChunk(): Boolean {
    val c = codec ?: return false
    val ext = extractor ?: return false
    val timeoutUs = 3000L
    var producedSamples = false

    // Feed input to codec
    if (!isExtractorEOS) {
      val inIndex = c.dequeueInputBuffer(timeoutUs)
      if (inIndex >= 0) {
        val inputBuffer = c.getInputBuffer(inIndex)
        if (inputBuffer != null) {
          val sampleSize = ext.readSampleData(inputBuffer, 0)
          if (sampleSize < 0) {
            c.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            isExtractorEOS = true
          } else {
            val sampleTime = ext.sampleTime
            c.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
            ext.advance()
          }
        }
      }
    }

    // Retrieve decoded output
    val outIndex = c.dequeueOutputBuffer(bufferInfo, timeoutUs)
    if (outIndex >= 0) {
      if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
        isCodecEOS = true
      }

      val outBuffer = c.getOutputBuffer(outIndex)
      if (outBuffer != null && bufferInfo.size > 0) {
        outBuffer.position(bufferInfo.offset)
        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val shortBuffer = outBuffer.asShortBuffer()
        val numShorts = shortBuffer.remaining()
        val rawChunk = ShortArray(numShorts)
        shortBuffer.get(rawChunk)

        // Ensure Stereo
        val stereoChunk = if (channelCount == 1) {
          AudioMixerDsp.monoToStereo(rawChunk)
        } else {
          rawChunk
        }

        // Resample if source sample rate differs from 44.1kHz
        val normalizedChunk = if (sampleRate != AudioMixerDsp.STANDARD_SAMPLE_RATE) {
          AudioMixerDsp.resampleLinear(
            stereoChunk,
            sampleRate,
            AudioMixerDsp.STANDARD_SAMPLE_RATE,
            channels = 2
          )
        } else {
          stereoChunk
        }

        for (sample in normalizedChunk) {
          outputQueue.add(sample)
        }
        producedSamples = true
      }
      c.releaseOutputBuffer(outIndex, false)
    }
    return producedSamples
  }

  override fun close() {
    outputQueue.clear()
    try {
      codec?.stop()
      codec?.release()
    } catch (ignored: Exception) {}
    codec = null

    try {
      extractor?.release()
    } catch (ignored: Exception) {}
    extractor = null
  }
}

object TrackStreamSourceFactory {
  fun create(filePath: String): TrackStreamSource {
    return if (filePath.endsWith(".wav", ignoreCase = true)) {
      WavStreamSource(filePath)
    } else {
      MediaCodecStreamSource(filePath)
    }
  }
}
