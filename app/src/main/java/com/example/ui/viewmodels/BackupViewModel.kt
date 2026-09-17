package com.example.ui.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.ImportMode
import com.example.backup.ImportReport
import com.example.backup.ProjectArchiveManager
import com.example.backup.ValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

data class BackupUiState(
  val isBusy: Boolean = false,
  val busyMessage: String = "",
  val validationReport: ValidationReport? = null,
  val pendingImportUri: Uri? = null,
  val selectedImportMode: ImportMode = ImportMode.MERGE,
  val lastImportReport: ImportReport? = null,
  val feedbackMessage: String? = null,
  val isError: Boolean = false
)

class BackupViewModel(
  private val projectArchiveManager: ProjectArchiveManager
) : ViewModel() {

  private val _uiState = MutableStateFlow(BackupUiState())
  val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

  fun setImportMode(mode: ImportMode) {
    _uiState.update { it.copy(selectedImportMode = mode) }
  }

  fun exportSongToTempFile(songId: String, onReady: (File) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Exporting song project...") }
      try {
        val file = projectArchiveManager.exportSongToTempFile(songId)
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Song exported successfully", isError = false) }
        launch(Dispatchers.Main) { onReady(file) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Export failed: ${e.localizedMessage ?: e.message}", isError = true) }
      }
    }
  }

  fun exportAlbumToTempFile(albumId: String, onReady: (File) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Exporting album project...") }
      try {
        val file = projectArchiveManager.exportAlbumToTempFile(albumId)
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Album exported successfully", isError = false) }
        launch(Dispatchers.Main) { onReady(file) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Export failed: ${e.localizedMessage ?: e.message}", isError = true) }
      }
    }
  }

  fun exportFullBackupToTempFile(onReady: (File) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Exporting complete library backup...") }
      try {
        val file = projectArchiveManager.exportFullBackupToTempFile()
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Complete backup created successfully", isError = false) }
        launch(Dispatchers.Main) { onReady(file) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Backup failed: ${e.localizedMessage ?: e.message}", isError = true) }
      }
    }
  }

  fun exportSongToUri(songId: String, uri: Uri, contentResolver: ContentResolver) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Saving song project...") }
      try {
        contentResolver.openOutputStream(uri)?.use { out ->
          projectArchiveManager.exportSong(songId, out)
        }
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Song project saved successfully!", isError = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Failed to save file: ${e.localizedMessage ?: e.message}", isError = true) }
      }
    }
  }

  fun exportAlbumToUri(albumId: String, uri: Uri, contentResolver: ContentResolver) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Saving album project...") }
      try {
        contentResolver.openOutputStream(uri)?.use { out ->
          projectArchiveManager.exportAlbum(albumId, out)
        }
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Album project saved successfully!", isError = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Failed to save album: ${e.localizedMessage ?: e.message}", isError = true) }
      }
    }
  }

  fun exportFullBackupToUri(uri: Uri, contentResolver: ContentResolver) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Saving backup...") }
      try {
        contentResolver.openOutputStream(uri)?.use { out ->
          projectArchiveManager.exportFullBackup(out)
        }
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Complete backup saved successfully!", isError = false) }
      } catch (e: Exception) {
        _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Failed to save backup: ${e.localizedMessage ?: e.message}", isError = true) }
      }
    }
  }

  fun validateSelectedFile(uri: Uri, contentResolver: ContentResolver) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Validating project archive...") }
      try {
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
          _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Could not open selected file", isError = true) }
          return@launch
        }

        val report = projectArchiveManager.validateArchive(inputStream)
        _uiState.update {
          it.copy(
            isBusy = false,
            busyMessage = "",
            validationReport = report,
            pendingImportUri = uri,
            selectedImportMode = ImportMode.MERGE // Default to Merge
          )
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isBusy = false,
            busyMessage = "",
            feedbackMessage = "Validation failed: ${e.localizedMessage ?: e.message}",
            isError = true
          )
        }
      }
    }
  }

  fun confirmImport(contentResolver: ContentResolver, onComplete: () -> Unit = {}) {
    val uri = _uiState.value.pendingImportUri ?: return
    val mode = _uiState.value.selectedImportMode

    viewModelScope.launch(Dispatchers.IO) {
      _uiState.update { it.copy(isBusy = true, busyMessage = "Importing project ($mode)...") }
      try {
        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
          _uiState.update { it.copy(isBusy = false, busyMessage = "", feedbackMessage = "Cannot read archive stream", isError = true) }
          return@launch
        }

        val report = projectArchiveManager.importArchive(inputStream, mode)
        _uiState.update {
          it.copy(
            isBusy = false,
            busyMessage = "",
            validationReport = null,
            pendingImportUri = null,
            lastImportReport = report,
            feedbackMessage = if (report.isSuccess) report.message else "Import failed: ${report.error}",
            isError = !report.isSuccess
          )
        }
        if (report.isSuccess) {
          launch(Dispatchers.Main) { onComplete() }
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isBusy = false,
            busyMessage = "",
            validationReport = null,
            pendingImportUri = null,
            feedbackMessage = "Import error: ${e.localizedMessage ?: e.message}",
            isError = true
          )
        }
      }
    }
  }

  fun dismissValidationDialog() {
    _uiState.update { it.copy(validationReport = null, pendingImportUri = null) }
  }

  fun clearFeedback() {
    _uiState.update { it.copy(feedbackMessage = null, lastImportReport = null) }
  }
}
