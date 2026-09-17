package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.export.WavMixdownExporter
import com.example.audio.export.WavValidator
import com.example.audio.export.WavWriter
import com.example.audio.mixer.TrackChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WavExportTest {

  private lateinit var context: Context
  private lateinit var testDir: File

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    testDir = File(context.cacheDir, "wav_test_${System.currentTimeMillis()}").apply { mkdirs() }
  }

  /**
   * Helper to generate a 16-bit 44.1kHz Stereo PCM WAV file with a sine wave or silence.
   */
  private fun createSyntheticWav(
    file: File,
    durationMs: Long,
    frequency: Double = 440.0,
    amplitude: Float = 0.5f,
    sampleRate: Int = 44100
  ) {
    val numFrames = (sampleRate * (durationMs / 1000.0)).toInt()
    val buffer = ShortArray(2048) // 1024 frames (stereo)
    var framesWritten = 0

    FileOutputStream(file).use { outputStream ->
      WavWriter.writeHeader(outputStream, sampleRate, 2, 16, 0L)
      while (framesWritten < numFrames) {
        val framesToWrite = minOf(1024, numFrames - framesWritten)
        for (i in 0 until framesToWrite) {
          val sampleIndex = framesWritten + i
          val t = sampleIndex.toDouble() / sampleRate
          val sampleVal = (Math.sin(2.0 * Math.PI * frequency * t) * amplitude * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
          buffer[i * 2] = sampleVal     // Left
          buffer[i * 2 + 1] = sampleVal // Right
        }
        val bytes = WavWriter.shortsToLittleEndianBytes(buffer, framesToWrite * 2)
        outputStream.write(bytes)
        framesWritten += framesToWrite
      }
    }
    val totalPcmBytes = numFrames.toLong() * 2 * 2
    WavWriter.finalizeWavHeader(file, totalPcmBytes)
  }

  @Test
  fun testWavWriterAndValidator_ValidFile() {
    val wavFile = File(testDir, "test_valid.wav")

    // Write 44100 frames (1 second of audio)
    val testData = ShortArray(2000) { (it % 1000).toShort() }
    val totalFrames = 44100
    var totalWritten = 0

    FileOutputStream(wavFile).use { outputStream ->
      WavWriter.writeHeader(outputStream, 44100, 2, 16, 0L)
      while (totalWritten < totalFrames * 2) {
        val toWrite = minOf(testData.size, (totalFrames * 2) - totalWritten)
        val bytes = WavWriter.shortsToLittleEndianBytes(testData, toWrite)
        outputStream.write(bytes)
        totalWritten += toWrite
      }
    }
    val totalPcmBytes = totalFrames.toLong() * 2 * 2
    WavWriter.finalizeWavHeader(wavFile, totalPcmBytes)

    assertTrue(wavFile.exists())
    assertTrue(wavFile.length() > 44)

    // Validate with WavValidator
    val validation = WavValidator.validate(wavFile)
    assertTrue("Validation failed: ${validation.errorMessage}", validation.isValid)
    assertEquals(44100, validation.sampleRate)
    assertEquals(2, validation.channels)
    assertEquals(16, validation.bitsPerSample)
    assertEquals(44100 * 2 * 2L, validation.dataByteCount)
    assertEquals(1000L, validation.durationMs)
  }

  @Test
  fun testWavValidator_CorruptedHeadersDetected() {
    val badFile = File(testDir, "corrupted.wav")
    badFile.writeBytes(ByteArray(20)) // Truncated header

    val validation = WavValidator.validate(badFile)
    assertFalse(validation.isValid)
    assertNotNull(validation.errorMessage)

    // Invalid RIFF header
    val badHeaderFile = File(testDir, "bad_riff.wav")
    val bytes = ByteArray(44)
    System.arraycopy("TEST".toByteArray(), 0, bytes, 0, 4)
    badHeaderFile.writeBytes(bytes)

    val validation2 = WavValidator.validate(badHeaderFile)
    assertFalse(validation2.isValid)
  }

  @Test
  fun testMixdownExport_MultiTrackSyncAndVolumes() = runBlocking {
    val track1File = File(testDir, "beat_track.wav")
    val track2File = File(testDir, "vocal_track.wav")

    createSyntheticWav(track1File, durationMs = 2000, frequency = 220.0, amplitude = 0.4f)
    createSyntheticWav(track2File, durationMs = 3000, frequency = 440.0, amplitude = 0.4f)

    val channel1 = TrackChannel(id = "t1", name = "Beat", filePath = track1File.absolutePath, type = "BEAT", volume = 0.8f, durationMs = 2000)
    val channel2 = TrackChannel(id = "t2", name = "Vocal", filePath = track2File.absolutePath, type = "VOCAL", volume = 1.0f, durationMs = 3000)

    val exporter = WavMixdownExporter(context)
    val progressUpdates = mutableListOf<Float>()

    val result = exporter.exportMixToFile(
      tracks = listOf(channel1, channel2),
      masterVolume = 1.0f,
      fileName = "export_multitrack_test.wav",
      onProgress = { p -> progressUpdates.add(p.fraction) }
    )

    assertTrue("Export should succeed: ${result.errorMessage}", result.success)
    assertNotNull(result.file)
    assertTrue(result.file!!.exists())
    assertEquals(3000L, result.durationMs) // Takes max duration of active tracks
    assertTrue(result.validation?.isValid == true)
    assertTrue(progressUpdates.isNotEmpty())
    assertTrue(progressUpdates.last() >= 0.99f)

    // Verify WAV file structure
    val validation = WavValidator.validate(result.file!!)
    assertTrue(validation.isValid)
    assertEquals(44100, validation.sampleRate)
    assertEquals(2, validation.channels)
    assertEquals(16, validation.bitsPerSample)
  }

  @Test
  fun testMixdownExport_MuteAndSoloLogic() = runBlocking {
    val track1File = File(testDir, "track1.wav")
    val track2File = File(testDir, "track2.wav")

    createSyntheticWav(track1File, durationMs = 2000, frequency = 300.0, amplitude = 0.5f)
    createSyntheticWav(track2File, durationMs = 4000, frequency = 600.0, amplitude = 0.5f)

    // Track 2 is soloed -> only Track 2 should be exported
    val channel1 = TrackChannel(id = "t1", name = "Beat", filePath = track1File.absolutePath, type = "BEAT", volume = 1.0f, isSolo = false, durationMs = 2000)
    val channel2 = TrackChannel(id = "t2", name = "Vocal", filePath = track2File.absolutePath, type = "VOCAL", volume = 1.0f, isSolo = true, durationMs = 4000)

    val exporter = WavMixdownExporter(context)
    val result = exporter.exportMixToFile(
      tracks = listOf(channel1, channel2),
      masterVolume = 1.0f,
      fileName = "export_solo_test.wav"
    )

    assertTrue(result.success)
    assertEquals(4000L, result.durationMs)
  }

  @Test
  fun testMixdownExport_ClippingPrevention() = runBlocking {
    val track1File = File(testDir, "loud1.wav")
    val track2File = File(testDir, "loud2.wav")

    // Both tracks have high amplitude -> sum would exceed 1.0f without limiting
    createSyntheticWav(track1File, durationMs = 1500, frequency = 440.0, amplitude = 0.9f)
    createSyntheticWav(track2File, durationMs = 1500, frequency = 440.0, amplitude = 0.9f)

    val channel1 = TrackChannel(id = "t1", name = "Loud 1", filePath = track1File.absolutePath, type = "RECORDING", volume = 1.0f, durationMs = 1500)
    val channel2 = TrackChannel(id = "t2", name = "Loud 2", filePath = track2File.absolutePath, type = "RECORDING", volume = 1.0f, durationMs = 1500)

    val exporter = WavMixdownExporter(context)
    val result = exporter.exportMixToFile(
      tracks = listOf(channel1, channel2),
      masterVolume = 1.2f, // Extra master boost
      fileName = "export_clipped_test.wav"
    )

    assertTrue(result.success)
    assertTrue("Clipping should have been detected and prevented by tanh limiter", result.clippingPrevented)

    // Verify rendered samples never wrap or overflow
    val wavFile = result.file!!
    val bytes = wavFile.readBytes()
    val shortBuffer = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
    var maxVal = 0
    while (shortBuffer.hasRemaining()) {
      val sample = Math.abs(shortBuffer.get().toInt())
      if (sample > maxVal) maxVal = sample
    }
    assertTrue("Max sample ($maxVal) must be within 16-bit range", maxVal <= 32767)
  }

  @Test
  fun testMixdownExport_Cancellation() = runBlocking {
    val track1File = File(testDir, "long_track.wav")
    createSyntheticWav(track1File, durationMs = 10000, frequency = 220.0)

    val channel = TrackChannel(id = "t1", name = "Long", filePath = track1File.absolutePath, type = "BEAT", durationMs = 10000)
    val exporter = WavMixdownExporter(context)

    var job: Job? = null
    var caughtCancellation = false

    job = launch {
      try {
        exporter.exportMixToFile(
          tracks = listOf(channel),
          fileName = "cancelled.wav",
          onProgress = { p ->
            if (p.fraction > 0.05f) {
              job?.cancel()
            }
          }
        )
      } catch (e: CancellationException) {
        caughtCancellation = true
      }
    }

    job.join()
    // Verification that the coroutine handled cancellation gracefully
    assertTrue(job.isCancelled)
  }
}
