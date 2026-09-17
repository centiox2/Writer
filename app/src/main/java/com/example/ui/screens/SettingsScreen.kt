package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.audio.RecordingQuality
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodels.StorageInfo
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  currentThemeMode: ThemeMode,
  onThemeModeSelected: (ThemeMode) -> Unit,
  fontSizeSp: Float = 18f,
  onFontSizeChanged: (Float) -> Unit = {},
  lineSpacingMultiplier: Float = 1.5f,
  onLineSpacingChanged: (Float) -> Unit = {},
  keepScreenAwakeDefault: Boolean = false,
  onKeepScreenAwakeChanged: (Boolean) -> Unit = {},
  autosaveEnabled: Boolean = true,
  onAutosaveChanged: (Boolean) -> Unit = {},
  recordingQuality: RecordingQuality = RecordingQuality.HIGH,
  onRecordingQualityChanged: (RecordingQuality) -> Unit = {},
  communicationModeEnabled: Boolean = false,
  onCommunicationModeChanged: (Boolean) -> Unit = {},
  defaultVolume: Float = 1.0f,
  onDefaultVolumeChanged: (Float) -> Unit = {},
  onExportFullBackup: () -> Unit = {},
  onImportBackup: () -> Unit = {},
  isBusy: Boolean = false,
  busyMessage: String = "",
  storageInfo: StorageInfo = StorageInfo(),
  onRefreshStorage: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  var showLicensesDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

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
      text = "Appearance, editor, audio, storage & about",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // --- APPEARANCE ---
    SettingsSectionHeader("Appearance")
    SettingsCard {
      Text(
        text = "Theme",
        style = MaterialTheme.typography.bodyMedium,
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
          label = { Text("Dark") },
          leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StudioBlue,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("theme_dark_chip")
        )
        FilterChip(
          selected = currentThemeMode == ThemeMode.LIGHT,
          onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
          label = { Text("Light") },
          leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StudioBlue,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("theme_light_chip")
        )
        FilterChip(
          selected = currentThemeMode == ThemeMode.SYSTEM,
          onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
          label = { Text("System") },
          leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp)) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StudioBlue,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("theme_system_chip")
        )
      }
    }

    // --- EDITOR ---
    SettingsSectionHeader("Editor")
    SettingsCard {
      SettingsSliderRow(
        label = "Font size",
        valueLabel = "${fontSizeSp.roundToInt()} sp",
        value = fontSizeSp,
        valueRange = 12f..32f,
        onValueChange = onFontSizeChanged,
        testTag = "font_size_slider"
      )
      Spacer(modifier = Modifier.height(16.dp))
      SettingsSliderRow(
        label = "Line spacing",
        valueLabel = String.format("%.1fx", lineSpacingMultiplier),
        value = lineSpacingMultiplier,
        valueRange = 1.1f..2.5f,
        onValueChange = onLineSpacingChanged,
        testTag = "line_spacing_slider"
      )
      Spacer(modifier = Modifier.height(6.dp))
      SettingsToggleRow(
        label = "Keep screen awake while writing",
        checked = keepScreenAwakeDefault,
        onCheckedChange = onKeepScreenAwakeChanged,
        testTag = "keep_screen_awake_switch"
      )
      SettingsToggleRow(
        label = "Autosave while typing",
        checked = autosaveEnabled,
        onCheckedChange = onAutosaveChanged,
        testTag = "autosave_switch"
      )
    }

    // --- AUDIO ---
    SettingsSectionHeader("Audio")
    SettingsCard {
      Text(
        text = "Recording quality",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        RecordingQuality.values().forEach { quality ->
          FilterChip(
            selected = recordingQuality == quality,
            onClick = { onRecordingQualityChanged(quality) },
            label = { Text(quality.name.lowercase().replaceFirstChar { it.uppercase() }) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioBlue,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("recording_quality_${quality.name.lowercase()}_chip")
          )
        }
      }
      Text(
        text = recordingQuality.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))
      SettingsToggleRow(
        label = "Communication Mode (reduces earpiece bleed while recording)",
        checked = communicationModeEnabled,
        onCheckedChange = onCommunicationModeChanged,
        testTag = "communication_mode_switch"
      )

      Spacer(modifier = Modifier.height(10.dp))
      SettingsSliderRow(
        label = "Default volume",
        valueLabel = "${(defaultVolume * 100).roundToInt()}%",
        value = defaultVolume,
        valueRange = 0f..1f,
        onValueChange = onDefaultVolumeChanged,
        testTag = "default_volume_slider"
      )
    }

    // --- STORAGE ---
    SettingsSectionHeader("Storage")
    SettingsCard {
      Row(verticalAlignment = Alignment.CenterVertically) {
        SettingsIconBadge(icon = Icons.Default.FolderZip)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Backup & Restore",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Save or restore your entire library as a .songproject archive",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (isBusy) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Text(
            text = busyMessage.ifBlank { "Processing archive..." },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onExportFullBackup,
          enabled = !isBusy,
          modifier = Modifier.weight(1f).testTag("export_backup_button")
        ) {
          Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Backup")
        }
        OutlinedButton(
          onClick = onImportBackup,
          enabled = !isBusy,
          modifier = Modifier.weight(1f).testTag("import_backup_button")
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Restore")
        }
      }
    }

    SettingsCard {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          SettingsIconBadge(icon = Icons.Default.Storage)
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Storage information",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
        }
        IconButton(onClick = onRefreshStorage, modifier = Modifier.testTag("refresh_storage_button")) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh storage info")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (storageInfo.isLoading) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Text(
            text = "Calculating storage usage...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        StorageInfoRow(label = "Songs", value = storageInfo.songCount.toString())
        StorageInfoRow(label = "Albums", value = storageInfo.albumCount.toString())
        StorageInfoRow(label = "Recordings", value = storageInfo.recordingCount.toString())
        StorageInfoRow(label = "Audio files on disk", value = formatBytes(storageInfo.audioStorageBytes))
      }
    }

    // --- ABOUT ---
    SettingsSectionHeader("About")
    SettingsCard {
      Row(verticalAlignment = Alignment.CenterVertically) {
        SettingsIconBadge(icon = Icons.Default.Info)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Songwriter Studio",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      AboutLinkRow(
        icon = Icons.Default.MusicNote,
        label = "Licenses",
        onClick = { showLicensesDialog = true },
        testTag = "licenses_row"
      )
      AboutLinkRow(
        icon = Icons.Default.Lock,
        label = "Privacy",
        onClick = { showPrivacyDialog = true },
        testTag = "privacy_row"
      )
    }

    Spacer(modifier = Modifier.height(100.dp))
  }

  if (showLicensesDialog) {
    AlertDialog(
      onDismissRequest = { showLicensesDialog = false },
      title = { Text("Open Source Licenses") },
      text = {
        Text(
          "Songwriter Studio is built with the following open-source software:\n\n" +
            "• Android Jetpack (AndroidX), Jetpack Compose, Material 3 — Apache 2.0\n" +
            "• Kotlin & Kotlin Coroutines — Apache 2.0\n" +
            "• Room, Navigation, DataStore — Apache 2.0\n" +
            "• Retrofit, OkHttp, Moshi (Square) — Apache 2.0\n" +
            "• Firebase Android SDK — Apache 2.0\n\n" +
            "Full license texts are available from each project's public repository."
        )
      },
      confirmButton = {
        TextButton(onClick = { showLicensesDialog = false }) { Text("Close") }
      },
      modifier = Modifier.testTag("licenses_dialog")
    )
  }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy") },
      text = {
        Text(
          "Songwriter Studio is 100% offline-first. Your songs, lyrics, recordings, and " +
            "backups are stored only on this device. Nothing is uploaded to a server, no " +
            "account is required, and no usage data is tracked or shared with third parties. " +
            "Exporting a .songproject archive is the only way your data leaves the device, " +
            "and that only happens when you choose to share or back it up yourself."
        )
      },
      confirmButton = {
        TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
      },
      modifier = Modifier.testTag("privacy_dialog")
    )
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Spacer(modifier = Modifier.height(20.dp))
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold
  )
  Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp), content = content)
  }
}

@Composable
private fun SettingsIconBadge(icon: ImageVector) {
  Box(
    modifier = Modifier
      .size(36.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.primaryContainer),
    contentAlignment = Alignment.Center
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
  }
}

@Composable
private fun SettingsToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f).padding(end = 12.dp)
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedTrackColor = StudioBlue),
      modifier = Modifier.testTag(testTag)
    )
  }
}

@Composable
private fun SettingsSliderRow(
  label: String,
  valueLabel: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  testTag: String
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      Text(text = valueLabel, style = MaterialTheme.typography.bodyMedium, color = StudioBlue, fontWeight = FontWeight.SemiBold)
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(
        thumbColor = StudioBlue,
        activeTrackColor = StudioBlue,
        inactiveTrackColor = StudioBlue.copy(alpha = 0.24f)
      ),
      modifier = Modifier.fillMaxWidth().testTag(testTag)
    )
  }
}

@Composable
private fun StorageInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun AboutLinkRow(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(10.dp))
      Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
    TextButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
      Text("View")
    }
  }
}

private fun formatBytes(bytes: Long): String {
  if (bytes <= 0) return "0 MB"
  val mb = bytes / (1024.0 * 1024.0)
  return if (mb >= 1024.0) {
    String.format("%.2f GB", mb / 1024.0)
  } else {
    String.format("%.1f MB", mb)
  }
}
