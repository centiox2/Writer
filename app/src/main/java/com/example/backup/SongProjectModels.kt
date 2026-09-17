package com.example.backup

enum class ProjectType {
  SONG,
  ALBUM,
  FULL_BACKUP
}

enum class ImportMode {
  MERGE,
  REPLACE
}

data class ValidationReport(
  val isValid: Boolean,
  val version: Int = 1,
  val type: ProjectType = ProjectType.SONG,
  val title: String = "",
  val songCount: Int = 0,
  val albumCount: Int = 0,
  val trackCount: Int = 0,
  val recordingCount: Int = 0,
  val totalAudioSizeBytes: Long = 0L,
  val missingAudioFilesCount: Int = 0,
  val errors: List<String> = emptyList(),
  val warnings: List<String> = emptyList(),
  val lyricsPreview: String = ""
)

data class ImportReport(
  val isSuccess: Boolean,
  val importedSongsCount: Int = 0,
  val importedAlbumsCount: Int = 0,
  val importedTracksCount: Int = 0,
  val importedRecordingsCount: Int = 0,
  val mode: ImportMode = ImportMode.MERGE,
  val error: String? = null,
  val message: String = ""
)
