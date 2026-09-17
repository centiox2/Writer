package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AlbumCoverPreset(
  val id: String,
  val name: String,
  val gradientStart: Color,
  val gradientEnd: Color,
  val icon: ImageVector
)

val ALBUM_COVER_PRESETS = listOf(
  AlbumCoverPreset("preset:studio_blue", "Studio Blue", Color(0xFF1E3A8A), Color(0xFF3B82F6), Icons.Default.Album),
  AlbumCoverPreset("preset:neon_cyan", "Neon Wave", Color(0xFF0F766E), Color(0xFF2DD4BF), Icons.Default.GraphicEq),
  AlbumCoverPreset("preset:midnight_purple", "Midnight Violet", Color(0xFF581C87), Color(0xFFA855F7), Icons.Default.MusicNote),
  AlbumCoverPreset("preset:sunset_amber", "Sunset Gold", Color(0xFF78350F), Color(0xFFF59E0B), Icons.Default.Radio),
  AlbumCoverPreset("preset:crimson_tape", "Crimson Tape", Color(0xFF7F1D1D), Color(0xFFEF4444), Icons.Default.Speaker)
)

fun getCoverPreset(id: String?): AlbumCoverPreset {
  return ALBUM_COVER_PRESETS.find { it.id == id } ?: ALBUM_COVER_PRESETS.first()
}

@Composable
fun AlbumArtworkThumbnail(
  artworkUri: String?,
  size: Dp = 48.dp,
  shapeRadius: Dp = 10.dp,
  modifier: Modifier = Modifier
) {
  val preset = getCoverPreset(artworkUri)

  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(shapeRadius))
      .background(
        Brush.linearGradient(
          listOf(preset.gradientStart, preset.gradientEnd)
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    // Vinyl groove accent circle
    Box(
      modifier = Modifier
        .size(size * 0.7f)
        .clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.25f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = preset.icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.size(size * 0.45f)
      )
    }
  }
}

@Composable
fun AlbumCoverPresetPicker(
  selectedPresetId: String?,
  onSelectPreset: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    ALBUM_COVER_PRESETS.forEach { preset ->
      val isSelected = (selectedPresetId == preset.id) || (selectedPresetId == null && preset == ALBUM_COVER_PRESETS.first())
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Brush.linearGradient(listOf(preset.gradientStart, preset.gradientEnd)))
          .clickable { onSelectPreset(preset.id) },
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Selected artwork",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}
