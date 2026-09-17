package com.example

import com.example.audio.mixer.AudioMixerDsp
import com.example.audio.mixer.MixedAudioFrame
import com.example.audio.mixer.TrackChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MultiTrackMixerTest {

  @Test
  fun testVolumeScaling() {
    // 1. Single channel at 50% volume
    val buffer = shortArrayOf(1000, -2000, 4000, -8000)
    val gained = AudioMixerDsp.applyGain(buffer, gain = 0.5f, isAudible = true)

    assertEquals(500f, gained[0], 0.01f)
    assertEquals(-1000f, gained[1], 0.01f)
    assertEquals(2000f, gained[2], 0.01f)
    assertEquals(-4000f, gained[3], 0.01f)

    // 2. Muted channel -> all zeros
    val mutedGained = AudioMixerDsp.applyGain(buffer, gain = 1.0f, isAudible = false)
    for (sample in mutedGained) {
      assertEquals(0f, sample, 0.001f)
    }
  }

  @Test
  fun testMultipleTracksSumMixing() {
    val track1 = floatArrayOf(1000f, -500f, 2000f, -1000f)
    val track2 = floatArrayOf(2000f, -1500f, 1000f, -500f)
    val track3 = floatArrayOf(500f, 200f, -500f, 300f)

    val mixed: MixedAudioFrame = AudioMixerDsp.mixAndLimit(
      channelBuffers = listOf(track1, track2, track3),
      frameSize = 4,
      masterGain = 1.0f
    )

    assertEquals(4, mixed.buffer.size)
    // 1000 + 2000 + 500 = 3500
    assertEquals(3500.toShort(), mixed.buffer[0])
    // -500 + -1500 + 200 = -1800
    assertEquals((-1800).toShort(), mixed.buffer[1])
    // 2000 + 1000 + -500 = 2500
    assertEquals(2500.toShort(), mixed.buffer[2])
    // -1000 + -500 + 300 = -1200
    assertEquals((-1200).toShort(), mixed.buffer[3])

    assertFalse(mixed.clippingDetectedAndPrevented)
  }

  @Test
  fun testMasterGainScaling() {
    val track = floatArrayOf(1000f, -2000f)
    val mixed = AudioMixerDsp.mixAndLimit(
      channelBuffers = listOf(track),
      frameSize = 2,
      masterGain = 1.5f
    )

    assertEquals(1500.toShort(), mixed.buffer[0])
    assertEquals((-3000).toShort(), mixed.buffer[1])
  }

  @Test
  fun testClippingPreventionSoftLimiter() {
    // Large amplitude values that would severely clip above 32767
    val track1 = floatArrayOf(30000f, -30000f)
    val track2 = floatArrayOf(30000f, -30000f)

    val mixed = AudioMixerDsp.mixAndLimit(
      channelBuffers = listOf(track1, track2), // Sum would be 60000 and -60000
      frameSize = 2,
      masterGain = 1.0f
    )

    assertTrue("Clipping prevention should be flagged", mixed.clippingDetectedAndPrevented)
    // Output must stay cleanly within valid 16-bit PCM range [-32768, 32767]
    assertTrue("Positive peak must not exceed 32767", mixed.buffer[0] <= 32767)
    assertTrue("Negative peak must not go below -32768", mixed.buffer[1] >= -32768)
    // Should be compressed smoothly near ceiling without wrapping
    assertTrue("Sample 0 should be softly limited above threshold", mixed.buffer[0] > 28000)
    assertTrue("Sample 1 should be softly limited below -threshold", mixed.buffer[1] < -28000)
  }

  @Test
  fun testMuteAndSoloLogic() {
    // Normal state: track muted -> not audible
    assertFalse(AudioMixerDsp.isTrackAudible(trackMuted = true, trackSolo = false, isAnyTrackSolo = false))
    // Normal state: track unmuted -> audible
    assertTrue(AudioMixerDsp.isTrackAudible(trackMuted = false, trackSolo = false, isAnyTrackSolo = false))

    // Solo mode active (someone is soloed):
    // Non-solo track should be silent
    assertFalse(AudioMixerDsp.isTrackAudible(trackMuted = false, trackSolo = false, isAnyTrackSolo = true))
    // Solo track should be heard
    assertTrue(AudioMixerDsp.isTrackAudible(trackMuted = false, trackSolo = true, isAnyTrackSolo = true))
    // Solo track that is also muted should NOT be heard
    assertFalse(AudioMixerDsp.isTrackAudible(trackMuted = true, trackSolo = true, isAnyTrackSolo = true))
  }

  @Test
  fun testDifferentTrackDurationsAndPadding() {
    // Track 1 has 4 samples, Track 2 has only 2 samples (shorter duration)
    val track1 = floatArrayOf(1000f, 2000f, 3000f, 4000f)
    val track2 = floatArrayOf(500f, 500f) // ends early

    val mixed = AudioMixerDsp.mixAndLimit(
      channelBuffers = listOf(track1, track2),
      frameSize = 4,
      masterGain = 1.0f
    )

    assertEquals(4, mixed.buffer.size)
    assertEquals((1000 + 500).toShort(), mixed.buffer[0])
    assertEquals((2000 + 500).toShort(), mixed.buffer[1])
    // After track 2 ends, track 1 continues with silence for track 2
    assertEquals(3000.toShort(), mixed.buffer[2])
    assertEquals(4000.toShort(), mixed.buffer[3])
  }

  @Test
  fun testSampleRateResampling() {
    // Linear resample from 22050 to 44100 (2x upsampling)
    val input = shortArrayOf(100, 200, 300)
    val resampled = AudioMixerDsp.resampleLinear(
      input = input,
      srcSampleRate = 22050,
      dstSampleRate = 44100,
      channels = 1
    )

    assertEquals(6, resampled.size)
    assertEquals(100.toShort(), resampled[0])
    assertEquals(150.toShort(), resampled[1]) // Interpolated between 100 and 200
    assertEquals(200.toShort(), resampled[2])
    assertEquals(250.toShort(), resampled[3]) // Interpolated between 200 and 300
    assertEquals(300.toShort(), resampled[4])
  }

  @Test
  fun testMonoToStereoAndStereoToMono() {
    val mono = shortArrayOf(1000, -1000, 2000)
    val stereo = AudioMixerDsp.monoToStereo(mono)

    assertEquals(6, stereo.size)
    assertEquals(1000.toShort(), stereo[0])
    assertEquals(1000.toShort(), stereo[1])
    assertEquals((-1000).toShort(), stereo[2])
    assertEquals((-1000).toShort(), stereo[3])

    val backToMono = AudioMixerDsp.stereoToMono(stereo)
    assertEquals(3, backToMono.size)
    assertEquals(1000.toShort(), backToMono[0])
    assertEquals((-1000).toShort(), backToMono[1])
    assertEquals(2000.toShort(), backToMono[2])
  }

  @Test
  fun testTrackChannelDataModel() {
    val track = TrackChannel(
      id = "t1",
      name = "Drum Loop",
      filePath = "/dummy/path/beat.wav",
      type = "BEAT",
      volume = 1.2f,
      isMuted = false,
      isSolo = true,
      durationMs = 120_000L
    )

    assertEquals("t1", track.id)
    assertEquals("Drum Loop", track.name)
    assertEquals("BEAT", track.type)
    assertEquals(1.2f, track.volume, 0.001f)
    assertFalse(track.isMuted)
    assertTrue(track.isSolo)
    assertEquals(120_000L, track.durationMs)
  }
}
