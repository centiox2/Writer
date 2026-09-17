package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
  val route: String,
  val title: String,
  val icon: ImageVector
) {
  data object Songs : Screen("songs", "Songs", Icons.Default.MusicNote)
  data object Albums : Screen("albums", "Albums", Icons.Default.Album)
  data object Recordings : Screen("recordings", "Recordings", Icons.Default.Mic)
  data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
  data object LyricEditor : Screen("lyric_editor/{songId}", "Lyric Editor", Icons.Default.MusicNote) {
    fun createRoute(songId: String) = "lyric_editor/$songId"
  }
  data object MultiTrackMixer : Screen("mixer/{songId}", "Mixer", Icons.Default.MusicNote) {
    fun createRoute(songId: String) = "mixer/$songId"
  }

  companion object {
    val bottomNavItems: List<Screen>
      get() = listOf(Songs, Albums, Recordings, Settings)
  }
}
