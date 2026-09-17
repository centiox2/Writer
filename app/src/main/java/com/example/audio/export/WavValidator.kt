package com.example.audio.export

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Result of WAV file structural and audio payload validation.
 */
data class WavValidationResult(
  val isValid: Boolean,
  val sampleRate: Int = 0,
  val channels: Int = 0,
  val bitsPerSample: Int = 0,
  val durationMs: Long = 0L,
  val dataByteCount: Long = 0L,
  val fileSize: Long = 0L,
  val errorMessage: String? = null
)

/**
 * Validates WAV audio files to verify correct RIFF structure, fmt chunk,
 * 16-bit linear PCM format, sample rate, channels, and data chunk integrity.
 */
object WavValidator {

  private const val RIFF_HEADER = "RIFF"
  private const val WAVE_HEADER = "WAVE"
  private const val FMT_CHUNK = "fmt "
  private const val DATA_CHUNK = "data"

  /**
   * Validates a File on disk.
   */
  fun validate(file: File): WavValidationResult {
    if (!file.exists()) {
      return WavValidationResult(isValid = false, errorMessage = "File does not exist: ${file.path}")
    }
    if (file.length() < 44) {
      return WavValidationResult(
        isValid = false,
        fileSize = file.length(),
        errorMessage = "File size (${file.length()} bytes) is smaller than standard 44-byte WAV header"
      )
    }

    return try {
      FileInputStream(file).use { stream ->
        validate(stream, file.length())
      }
    } catch (e: Exception) {
      WavValidationResult(isValid = false, fileSize = file.length(), errorMessage = "I/O error reading WAV: ${e.message}")
    }
  }

  /**
   * Validates a WAV byte stream.
   */
  fun validate(stream: InputStream, totalStreamLength: Long = -1): WavValidationResult {
    val headerBytes = ByteArray(44)
    var bytesRead = 0
    while (bytesRead < 44) {
      val read = stream.read(headerBytes, bytesRead, 44 - bytesRead)
      if (read < 0) break
      bytesRead += read
    }

    if (bytesRead < 44) {
      return WavValidationResult(
        isValid = false,
        fileSize = bytesRead.toLong(),
        errorMessage = "Stream ended prematurely while reading header ($bytesRead/44 bytes)"
      )
    }

    return validateHeaderBytes(headerBytes, totalStreamLength)
  }

  /**
   * Validates in-memory WAV byte array.
   */
  fun validate(bytes: ByteArray): WavValidationResult {
    if (bytes.size < 44) {
      return WavValidationResult(
        isValid = false,
        fileSize = bytes.size.toLong(),
        errorMessage = "Byte array size (${bytes.size}) is smaller than 44-byte header"
      )
    }
    return validateHeaderBytes(bytes.copyOfRange(0, 44), bytes.size.toLong())
  }

  private fun validateHeaderBytes(header: ByteArray, totalSize: Long): WavValidationResult {
    val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

    // 1. "RIFF" chunk ID
    val riffId = String(header, 0, 4)
    if (riffId != RIFF_HEADER) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid RIFF identifier: found '$riffId', expected '$RIFF_HEADER'"
      )
    }

    // 2. Chunk size (offset 4, 4 bytes)
    buffer.position(4)
    val riffChunkSize = buffer.getInt().toLong() and 0xFFFFFFFFL

    // 3. "WAVE" format
    val waveId = String(header, 8, 4)
    if (waveId != WAVE_HEADER) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid WAVE identifier: found '$waveId', expected '$WAVE_HEADER'"
      )
    }

    // 4. "fmt " subchunk ID
    val fmtId = String(header, 12, 4)
    if (fmtId != FMT_CHUNK) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid fmt chunk identifier: found '$fmtId', expected '$FMT_CHUNK'"
      )
    }

    // 5. Subchunk1 size (offset 16, 4 bytes)
    buffer.position(16)
    val fmtSize = buffer.getInt()
    if (fmtSize < 16) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid fmt chunk size: $fmtSize (must be at least 16 for PCM)"
      )
    }

    // 6. Audio format (offset 20, 2 bytes)
    val audioFormat = buffer.getShort().toInt() and 0xFFFF
    if (audioFormat != 1) { // 1 = PCM
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Unsupported audio format: $audioFormat (only linear PCM 1 is supported)"
      )
    }

    // 7. Channels (offset 22, 2 bytes)
    val numChannels = buffer.getShort().toInt() and 0xFFFF
    if (numChannels <= 0 || numChannels > 8) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid channel count: $numChannels"
      )
    }

    // 8. Sample rate (offset 24, 4 bytes)
    val sampleRate = buffer.getInt()
    if (sampleRate <= 0 || sampleRate > 384000) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid sample rate: $sampleRate Hz"
      )
    }

    // 9. Byte rate (offset 28, 4 bytes)
    val byteRate = buffer.getInt().toLong() and 0xFFFFFFFFL

    // 10. Block align (offset 32, 2 bytes)
    val blockAlign = buffer.getShort().toInt() and 0xFFFF

    // 11. Bits per sample (offset 34, 2 bytes)
    val bitsPerSample = buffer.getShort().toInt() and 0xFFFF
    if (bitsPerSample != 16 && bitsPerSample != 8 && bitsPerSample != 24 && bitsPerSample != 32) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Unsupported bits per sample: $bitsPerSample"
      )
    }

    // Validate block align calculation
    val expectedBlockAlign = numChannels * (bitsPerSample / 8)
    if (blockAlign != expectedBlockAlign) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Block align mismatch: found $blockAlign, expected $expectedBlockAlign"
      )
    }

    // 12. "data" subchunk ID (offset 36, 4 bytes)
    val dataId = String(header, 36, 4)
    if (dataId != DATA_CHUNK) {
      return WavValidationResult(
        isValid = false,
        fileSize = totalSize,
        errorMessage = "Invalid data chunk identifier: found '$dataId', expected '$DATA_CHUNK'"
      )
    }

    // 13. Data chunk size (offset 40, 4 bytes)
    buffer.position(40)
    val dataSizeBytes = buffer.getInt().toLong() and 0xFFFFFFFFL

    // Calculate duration
    val bytesPerSecond = sampleRate.toLong() * blockAlign
    val durationMs = if (bytesPerSecond > 0) (dataSizeBytes * 1000L) / bytesPerSecond else 0L

    return WavValidationResult(
      isValid = true,
      sampleRate = sampleRate,
      channels = numChannels,
      bitsPerSample = bitsPerSample,
      durationMs = durationMs,
      dataByteCount = dataSizeBytes,
      fileSize = if (totalSize > 0) totalSize else (dataSizeBytes + 44),
      errorMessage = null
    )
  }
}
