package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.ImportMode
import com.example.backup.ProjectType
import com.example.backup.ValidationReport

@Composable
fun ProjectImportDialog(
  report: ValidationReport,
  selectedMode: ImportMode,
  onModeSelected: (ImportMode) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
              if (report.isValid) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.errorContainer
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (report.isValid) Icons.Default.FolderZip else Icons.Default.Error,
            contentDescription = null,
            tint = if (report.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          )
        }
        Column {
          Text(
            text = "Import .songproject",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (report.isValid) "Project Validated" else "Validation Failed",
            style = MaterialTheme.typography.bodySmall,
            color = if (report.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Project Overview Card
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = report.title.ifBlank { "Untitled Project" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = when (report.type) {
                    ProjectType.SONG -> "Song"
                    ProjectType.ALBUM -> "Album"
                    ProjectType.FULL_BACKUP -> "Full Backup"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Text(
              text = "Format Version: v${report.version}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Counts Grid
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              CountPill(icon = Icons.Default.MusicNote, count = report.songCount, label = "Songs")
              if (report.albumCount > 0) {
                CountPill(icon = Icons.Default.Album, count = report.albumCount, label = "Albums")
              }
              CountPill(icon = Icons.Default.Audiotrack, count = report.trackCount, label = "Tracks")
              CountPill(icon = Icons.Default.Mic, count = report.recordingCount, label = "Takes")
            }

            if (report.totalAudioSizeBytes > 0) {
              val sizeMb = report.totalAudioSizeBytes / (1024.0 * 1024.0)
              Text(
                text = "Audio Assets: ${String.format("%.1f", sizeMb)} MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Errors & Warnings
        if (report.errors.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Error,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Errors",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.error,
                  fontWeight = FontWeight.Bold
                )
              }
              report.errors.forEach { err ->
                Text(
                  text = "• $err",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer
                )
              }
            }
          }
        }

        if (report.warnings.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Warnings",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.tertiary,
                  fontWeight = FontWeight.Bold
                )
              }
              report.warnings.forEach { warn ->
                Text(
                  text = "• $warn",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer
                )
              }
            }
          }
        }

        // Lyrics Preview (if available)
        if (report.lyricsPreview.isNotBlank()) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Lyrics Preview",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold
            )
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth(),
              shadowElevation = 1.dp
            ) {
              Text(
                text = report.lyricsPreview,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }

        // Import Mode Selection
        if (report.isValid) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Import Option",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )

            // MERGE Option (Default)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onModeSelected(ImportMode.MERGE) }
                .background(
                  if (selectedMode == ImportMode.MERGE)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                  else Color.Transparent
                )
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedMode == ImportMode.MERGE,
                onClick = { onModeSelected(ImportMode.MERGE) },
                modifier = Modifier.testTag("import_mode_merge")
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Merge (Recommended)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Add imported content to existing library. Does not overwrite current work.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // REPLACE Option
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onModeSelected(ImportMode.REPLACE) }
                .background(
                  if (selectedMode == ImportMode.REPLACE)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                  else Color.Transparent
                )
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedMode == ImportMode.REPLACE,
                onClick = { onModeSelected(ImportMode.REPLACE) },
                modifier = Modifier.testTag("import_mode_replace")
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Replace / Overwrite",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = if (report.type == ProjectType.FULL_BACKUP)
                    "Replace current library completely with backup contents."
                  else "Overwrite existing matching songs/albums with imported data.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = report.isValid,
        modifier = Modifier.testTag("confirm_import_button")
      ) {
        Text("Import Now")
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_import_button")
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun CountPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  count: Int,
  label: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(14.dp),
      tint = MaterialTheme.colorScheme.primary
    )
    Text(
      text = "$count $label",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium
    )
  }
}
