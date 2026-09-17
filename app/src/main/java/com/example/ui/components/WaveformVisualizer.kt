package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import kotlin.math.sin

/**
 * Interactive Waveform Visualizer.
 * Renders real audio amplitude peaks with played/unplayed zones and a seeking playhead.
 * Supports direct tapping and smooth dragging to scrub through the audio file.
 */
@Composable
fun WaveformVisualizer(
  amplitudes: FloatArray?,
  progress: Float, // 0.0f to 1.0f
  isLoading: Boolean,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
  height: Dp = 56.dp,
  activeColor: Color = StudioMint,
  inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
  playheadColor: Color = StudioBlue
) {
  val clampedProgress = progress.coerceIn(0f, 1f)
  var isDragging by remember { mutableStateOf(false) }
  var dragProgress by remember { mutableFloatStateOf(0f) }

  val effectiveProgress = if (isDragging) dragProgress else clampedProgress

  // Animated shimmer for loading state
  val transition = rememberInfiniteTransition(label = "waveform_loading")
  val shimmerOffset by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shimmer_anim"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .testTag("waveform_visualizer")
      .pointerInput(amplitudes, isLoading) {
        if (isLoading || amplitudes == null || amplitudes.isEmpty()) return@pointerInput

        detectTapGestures(
          onPress = { offset ->
            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
            onSeek(fraction)
          }
        )
      }
      .pointerInput(amplitudes, isLoading) {
        if (isLoading || amplitudes == null || amplitudes.isEmpty()) return@pointerInput

        detectDragGestures(
          onDragStart = { offset ->
            isDragging = true
            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
            dragProgress = fraction
            onSeek(fraction)
          },
          onDragEnd = {
            isDragging = false
          },
          onDragCancel = {
            isDragging = false
          },
          onDrag = { change, _ ->
            change.consume()
            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
            dragProgress = fraction
            onSeek(fraction)
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
      val canvasWidth = size.width
      val canvasHeight = size.height
      val centerY = canvasHeight / 2f

      if (isLoading || amplitudes == null || amplitudes.isEmpty()) {
        // Draw elegant pulsating skeleton waveform
        val skeletonBars = 48
        val barSpacing = canvasWidth / skeletonBars
        val barWidth = (barSpacing * 0.55f).coerceIn(2f, 8f)

        for (i in 0 until skeletonBars) {
          val x = i * barSpacing + (barSpacing / 2f)
          val normX = i.toFloat() / skeletonBars
          val waveFactor = ((sin((normX * 8f) + (shimmerOffset * 6.28f)) + 1f) / 2f).coerceIn(0.15f, 0.9f)
          val barHeight = (canvasHeight * 0.75f * waveFactor).coerceAtLeast(6f)
          val top = centerY - (barHeight / 2f)

          drawRoundRect(
            color = inactiveColor.copy(alpha = 0.2f + (0.3f * waveFactor)),
            topLeft = Offset(x - (barWidth / 2f), top),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
          )
        }
        return@Canvas
      }

      val count = amplitudes.size
      if (count == 0) return@Canvas

      val barSpacing = canvasWidth / count
      val barWidth = (barSpacing * 0.65f).coerceIn(2f, 6f)
      val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

      val playheadX = canvasWidth * effectiveProgress

      for (i in 0 until count) {
        val x = i * barSpacing + (barSpacing / 2f)
        val amp = amplitudes[i].coerceIn(0.06f, 1.0f)
        val barHeight = (canvasHeight * 0.88f * amp).coerceAtLeast(6f)
        val top = centerY - (barHeight / 2f)

        val isPlayed = x <= playheadX
        val barColor = if (isPlayed) activeColor else inactiveColor

        drawRoundRect(
          color = barColor,
          topLeft = Offset(x - (barWidth / 2f), top),
          size = Size(barWidth, barHeight),
          cornerRadius = cornerRadius
        )
      }

      // Draw glowing playhead vertical scrubber line
      val playheadLineWidth = 3.dp.toPx()
      drawLine(
        brush = Brush.verticalGradient(
          listOf(
            playheadColor.copy(alpha = 0.4f),
            playheadColor,
            playheadColor,
            playheadColor.copy(alpha = 0.4f)
          )
        ),
        start = Offset(playheadX, 2.dp.toPx()),
        end = Offset(playheadX, canvasHeight - 2.dp.toPx()),
        strokeWidth = playheadLineWidth,
        cap = StrokeCap.Round
      )

      // Draw top and bottom scrubber indicator caps
      drawCircle(
        color = playheadColor,
        radius = 4.5.dp.toPx(),
        center = Offset(playheadX, 4.5.dp.toPx())
      )
      drawCircle(
        color = playheadColor,
        radius = 4.5.dp.toPx(),
        center = Offset(playheadX, canvasHeight - 4.5.dp.toPx())
      )
    }
  }
}
