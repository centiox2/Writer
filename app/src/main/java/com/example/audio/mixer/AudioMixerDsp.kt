package com.example.audio.mixer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Pure DSP utility for:
 * - Linear gain application
 * - Multi-channel summation
 * - Soft-knee limiting / tanh saturation to prevent digital clipping
 * - Hard clipping guard [-32768, 32767]
 * - Sample rate conversion (linear interpolation)
 * - Stereo <-> Mono channel replication/downmixing
 */
object AudioMixerDsp {

  const val STANDARD_SAMPLE_RATE = 44100
  const val STANDARD_CHANNELS = 2 // Stereo output
  const val MAX_16_BIT = 32767.0
  const val MIN_16_BIT = -32768.0

  /**
   * Resamples a buffer of 16-bit PCM samples from sourceSampleRate to targetSampleRate
   * using linear interpolation.
   */
  fun resampleLinear(
    input: ShortArray,
    srcSampleRate: Int,
    dstSampleRate: Int,
    channels: Int = 1
  ): ShortArray {
    if (srcSampleRate == dstSampleRate || input.isEmpty()) return input
    if (srcSampleRate <= 0 || dstSampleRate <= 0) return input

    val inFrames = input.size / channels
    if (inFrames == 0) return ShortArray(0)

    val outFrames = ((inFrames.toLong() * dstSampleRate) / srcSampleRate).toInt()
    if (outFrames == 0) return ShortArray(0)

    val output = ShortArray(outFrames * channels)
    val ratio = srcSampleRate.toDouble() / dstSampleRate.toDouble()

    for (outFrame in 0 until outFrames) {
      val srcPos = outFrame * ratio
      val inIndex1 = srcPos.toInt().coerceIn(0, inFrames - 1)
      val inIndex2 = min(inIndex1 + 1, inFrames - 1)
      val frac = (srcPos - inIndex1).toFloat()

      for (ch in 0 until channels) {
        val s1 = input[inIndex1 * channels + ch].toFloat()
        val s2 = input[inIndex2 * channels + ch].toFloat()
        val interpolated = s1 + frac * (s2 - s1)
        output[outFrame * channels + ch] = interpolated.toInt().coerceIn(MIN_16_BIT.toInt(), MAX_16_BIT.toInt()).toShort()
      }
    }
    return output
  }

  /**
   * Converts mono PCM to stereo PCM by duplicating samples: [M1, M2] -> [M1, M1, M2, M2]
   */
  fun monoToStereo(input: ShortArray): ShortArray {
    val output = ShortArray(input.size * 2)
    var outIdx = 0
    for (i in input.indices) {
      val sample = input[i]
      output[outIdx++] = sample
      output[outIdx++] = sample
    }
    return output
  }

  /**
   * Converts stereo PCM to mono PCM by averaging left and right channels: [L1, R1] -> [(L1+R1)/2]
   */
  fun stereoToMono(input: ShortArray): ShortArray {
    val output = ShortArray(input.size / 2)
    for (i in output.indices) {
      val left = input[i * 2].toInt()
      val right = input[i * 2 + 1].toInt()
      output[i] = ((left + right) / 2).toShort()
    }
    return output
  }

  /**
   * Applies track gain factor and mute/solo logic to a track's audio buffer.
   * If isAudible is false, samples are effectively zeroed (or skipped).
   */
  fun applyGain(
    buffer: ShortArray,
    gain: Float,
    isAudible: Boolean
  ): FloatArray {
    val output = FloatArray(buffer.size)
    if (!isAudible || gain <= 0f) {
      return output // All zeros
    }
    for (i in buffer.indices) {
      output[i] = buffer[i] * gain
    }
    return output
  }

  /**
   * Mixes multiple audio float channel buffers together, applying master gain
   * and anti-clipping soft-limiting.
   *
   * @param channelBuffers List of float buffers representing each track's audible output for this window
   * @param frameSize Number of samples to mix in this window
   * @param masterGain Master fader multiplier (0.0f to 2.0f)
   * @return MixedAudioFrame containing 16-bit PCM output, peak amplitude, and clipping status
   */
  fun mixAndLimit(
    channelBuffers: List<FloatArray>,
    frameSize: Int,
    masterGain: Float = 1.0f
  ): MixedAudioFrame {
    val sumBuffer = FloatArray(frameSize)

    // 1. Sum channels
    for (channel in channelBuffers) {
      val len = min(frameSize, channel.size)
      for (i in 0 until len) {
        sumBuffer[i] += channel[i]
      }
    }

    // 2. Apply Master Gain
    for (i in 0 until frameSize) {
      sumBuffer[i] *= masterGain
    }

    // 3. Peak detection & Soft-knee limiter / Tanh saturation
    var peakRaw = 0f
    for (i in 0 until frameSize) {
      val absVal = abs(sumBuffer[i])
      if (absVal > peakRaw) {
        peakRaw = absVal
      }
    }

    val outputShorts = ShortArray(frameSize)
    var clippingPrevented = false

    // If peak exceeds 32767, we prevent digital square-wave distortion clipping
    // by applying hyperbolic tangent soft-limiting above threshold.
    val threshold = 28000.0f
    val ceiling = 32760.0f

    for (i in 0 until frameSize) {
      val sample = sumBuffer[i]
      val absSample = abs(sample)

      val limitedSample = if (absSample > threshold) {
        clippingPrevented = true
        // Soft compression curve: threshold + (ceiling - threshold) * tanh((abs - threshold) / headroom)
        val over = absSample - threshold
        val headroom = (32767.0f - threshold) * 1.5f
        val compressed = threshold + (ceiling - threshold) * tanh((over / headroom).toDouble()).toFloat()
        if (sample < 0) -compressed else compressed
      } else {
        sample
      }

      // Final safety clamp to prevent integer overflow
      outputShorts[i] = limitedSample.toInt().coerceIn(MIN_16_BIT.toInt(), MAX_16_BIT.toInt()).toShort()
    }

    val normalizedPeak = (peakRaw / 32767.0f).coerceIn(0f, 1.5f)
    return MixedAudioFrame(outputShorts, normalizedPeak, clippingPrevented)
  }

  /**
   * Computes whether a track is audible based on its own mute flag, its solo flag,
   * and whether ANY track in the mixer is currently soloed.
   */
  fun isTrackAudible(
    trackMuted: Boolean,
    trackSolo: Boolean,
    isAnyTrackSolo: Boolean
  ): Boolean {
    if (isAnyTrackSolo) {
      // In Solo mode: only tracks with solo = true AND muted = false are heard
      return trackSolo && !trackMuted
    }
    // Normal mode: audible if not muted
    return !trackMuted
  }
}
