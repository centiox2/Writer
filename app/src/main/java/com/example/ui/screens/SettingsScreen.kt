package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioMint
import com.example.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
  currentThemeMode: ThemeMode,
  onThemeModeSelected: (ThemeMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp)
      .testTag("settings_screen")
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Text(
      text = "Studio Settings",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "App preferences, audio architecture & storage",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Theme Mode Section
    Text(
      text = "Appearance",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Theme Palette",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = currentThemeMode == ThemeMode.DARK,
            onClick = { onThemeModeSelected(ThemeMode.DARK) },
            label = { Text("Dark Studio") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioBlue,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )

          FilterChip(
            selected = currentThemeMode == ThemeMode.LIGHT,
            onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
            label = { Text("Light") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioBlue,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )

          FilterChip(
            selected = currentThemeMode == ThemeMode.SYSTEM,
            onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
            label = { Text("System") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioBlue,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Offline & Privacy Section
    Text(
      text = "Data & Privacy",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(imageVector = Icons.Default.CloudOff, contentDescription = null, tint = StudioMint, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("100% Offline-First", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("No accounts, tracking, or cloud uploads required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = StudioBlue, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Crash-Safe Autosave", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Debounced local SQLite writes with lifecycle flush", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Audio Engine Specs
    Text(
      text = "Audio Architecture",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = StudioBlue, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text("Native Android Audio Pipeline", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("AudioRecord 44.1 kHz 16-bit PCM capture • Earpiece Bleed Reduction • Multi-track PCM mixer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // App Information
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text("Songwriter Studio v1.0.0", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
          Text("Professional offline songwriting & recording platform", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }

    Spacer(modifier = Modifier.height(100.dp))
  }
}
