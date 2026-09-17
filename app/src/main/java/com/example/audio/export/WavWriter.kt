package com.example.audio.export

import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Low-level utility for constructing standard RIFF WAVE headers and writing 16-bit PCM audio.
 */
object WavWriter {

  /**
   * Generates a 44-byte standard PCM WAV header.
   */
  fun createHeader(
    sampleRate: Int = 44100,
    channels: Int = 2,
    bitsPerSample: Int = 16,
    totalAudioDataBytes: Long = 0L
  ): ByteArray {
    val totalDataLen = totalAudioDataBytes + 36
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val blockAlign = channels * bitsPerSample / 8

    val header = ByteArray(44)
    val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

    // 0..3: RIFF
    buffer.put('R'.code.toByte())
    buffer.put('I'.code.toByte())
    buffer.put('F'.code.toByte())
    buffer.put('F'.code.toByte())

    // 4..7: File size minus 8
    buffer.putInt((totalDataLen and 0xFFFFFFFFL).toInt())

    // 8..11: WAVE
    buffer.put('W'.code.toByte())
    buffer.put('A'.code.toByte())
    buffer.put('V'.code.toByte())
    buffer.put('E'.code.toByte())

    // 12..15: "fmt "
    buffer.put('f'.code.toByte())
    buffer.put('m'.code.toByte())
    buffer.put('t'.code.toByte())
    buffer.put(' '.code.toByte())

    // 16..19: Subchunk1Size (16 for PCM)
    buffer.putInt(16)

    // 20..21: AudioFormat (1 for PCM)
    buffer.putShort(1.toShort())

    // 22..23: NumChannels
    buffer.putShort(channels.toShort())

    // 24..27: SampleRate
    buffer.putInt(sampleRate)

    // 28..31: ByteRate
    buffer.putInt(byteRate)

    // 32..33: BlockAlign
    buffer.putShort(blockAlign.toShort())

    // 34..35: BitsPerSample
    buffer.putShort(bitsPerSample.toShort())

    // 36..39: "data"
    buffer.put('d'.code.toByte())
    buffer.put('a'.code.toByte())
    buffer.put('t'.code.toByte())
    buffer.put('a'.code.toByte())

    // 40..43: Data chunk size in bytes
    buffer.putInt((totalAudioDataBytes and 0xFFFFFFFFL).toInt())

    return header
  }

  /**
   * Writes the initial WAV header placeholder to an OutputStream.
   */
  fun writeHeader(
    outputStream: OutputStream,
    sampleRate: Int = 44100,
    channels: Int = 2,
    bitsPerSample: Int = 16,
    totalAudioDataBytes: Long = 0L
  ) {
    val header = createHeader(sampleRate, channels, bitsPerSample, totalAudioDataBytes)
    outputStream.write(header)
  }

  /**
   * Converts a ShortArray of 16-bit PCM samples to little-endian bytes.
   */
  fun shortsToLittleEndianBytes(shorts: ShortArray, length: Int = shorts.size): ByteArray {
    val count = length.coerceIn(0, shorts.size)
    val bytes = ByteArray(count * 2)
    for (i in 0 until count) {
      val sample = shorts[i].toInt()
      bytes[i * 2] = (sample and 0xFF).toByte()
      bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
    }
    return bytes
  }

  /**
   * Updates the chunk sizes in the WAV header of an existing RandomAccessFile.
   */
  fun finalizeWavHeader(file: RandomAccessFile, totalAudioDataBytes: Long) {
    val totalDataLen = totalAudioDataBytes + 36

    // Update RIFF chunk size at byte offset 4
    file.seek(4)
    val riffSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((totalDataLen and 0xFFFFFFFFL).toInt()).array()
    file.write(riffSize)

    // Update data chunk size at byte offset 40
    file.seek(40)
    val dataSize = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((totalAudioDataBytes and 0xFFFFFFFFL).toInt()).array()
    file.write(dataSize)
  }

  /**
   * Updates the chunk sizes in the WAV header of a file.
   */
  fun finalizeWavHeader(targetFile: File, totalAudioDataBytes: Long) {
    RandomAccessFile(targetFile, "rw").use { raf ->
      finalizeWavHeader(raf, totalAudioDataBytes)
    }
  }
}
