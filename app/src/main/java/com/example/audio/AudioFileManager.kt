package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ImportedAudioResult(
  val fileName: String,
  val localPath: String,
  val durationMs: Long,
  val mimeType: String,
  val fileSize: Long
)

class AudioFileManager(private val context: Context) {

  private val beatsDir: File
    get() = File(context.filesDir, "beats").apply { if (!exists()) mkdirs() }

  private val recordingsDir: File
    get() = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }

  private val waveformsDir: File
    get() = File(context.cacheDir, "waveforms").apply { if (!exists()) mkdirs() }

  /**
   * Creates a dedicated destination file for a new recording take.
   */
  fun createNewRecordingFile(songId: String, extension: String = "m4a"): File {
    val timestamp = System.currentTimeMillis()
    val cleanSongId = songId.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
    val fileName = "take_${cleanSongId}_${timestamp}.$extension"
    return File(recordingsDir, fileName)
  }

  /**
   * Duplicates an audio file on disk, returning the new file path.
   */
  fun duplicateAudioFile(sourcePath: String, prefix: String = "copy_"): String? {
    return try {
      val source = File(sourcePath)
      if (!source.exists()) return null
      val ext = source.extension
      val baseName = source.nameWithoutExtension
      val targetName = "${prefix}${baseName}_${System.currentTimeMillis()}.$ext"
      val destFile = File(source.parentFile ?: recordingsDir, targetName)
      source.copyTo(destFile, overwrite = true)
      destFile.absolutePath
    } catch (e: Exception) {
      Log.e("AudioFileManager", "Failed to duplicate audio file: ${e.message}", e)
      null
    }
  }

  /**
   * Imports an audio file chosen by the user via Storage Access Framework.
   * Copies the content to internal app storage for reliable offline access and permanence.
   */
  suspend fun importBeatFile(uri: Uri, songId: String): Result<ImportedAudioResult> =
    importAudioFile(uri, songId)

  suspend fun importAudioFile(uri: Uri, songId: String): Result<ImportedAudioResult> =
    withContext(Dispatchers.IO) {
      try {
        val contentResolver = context.contentResolver
        var originalName = "beat"
        var reportedSize = 0L

        // Query display name and size from content provider
        try {
          contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
              val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
              if (nameIndex >= 0) {
                val queriedName = cursor.getString(nameIndex)
                if (!queriedName.isNullOrBlank()) {
                  originalName = queriedName
                }
              }
              val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
              if (sizeIndex >= 0) {
                reportedSize = cursor.getLong(sizeIndex)
              }
            }
          }
        } catch (e: Exception) {
          Log.w("AudioFileManager", "Could not query content URI metadata: ${e.message}")
        }

        // Determine MIME type and safe extension
        val resolvedMime = contentResolver.getType(uri) ?: getMimeTypeFromExtension(originalName)
        val extension = getExtensionForMime(resolvedMime, originalName)

        val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val sanitizedBase = cleanName.substringBeforeLast(".")
        val targetFileName = "${songId}_${System.currentTimeMillis()}_$sanitizedBase.$extension"
        val destinationFile = File(beatsDir, targetFileName)

        // Stream copy from content resolver
        contentResolver.openInputStream(uri)?.use { inputStream ->
          FileOutputStream(destinationFile).use { outputStream ->
            inputStream.copyTo(outputStream)
          }
        } ?: return@withContext Result.failure(IllegalStateException("Unable to open input stream for audio URI"))

        if (!destinationFile.exists() || destinationFile.length() == 0L) {
          destinationFile.delete()
          return@withContext Result.failure(IllegalStateException("Imported audio file is empty or missing"))
        }

        // Extract real duration using MediaMetadataRetriever
        val durationMs = extractDuration(destinationFile.absolutePath)

        val result = ImportedAudioResult(
          fileName = originalName,
          localPath = destinationFile.absolutePath,
          durationMs = durationMs,
          mimeType = resolvedMime,
          fileSize = destinationFile.length()
        )
        Result.success(result)
      } catch (e: Throwable) {
        Log.e("AudioFileManager", "Failed to import beat file: ${e.message}", e)
        Result.failure(e)
      }
    }

  /**
   * Safely deletes a file from internal storage (beats or recordings).
   */
  fun deleteBeatFile(path: String) = deleteAudioFile(path)

  fun deleteAudioFile(path: String) {
    try {
      val file = File(path)
      if (file.exists()) {
        file.delete()
      }
    } catch (e: Exception) {
      Log.w("AudioFileManager", "Failed to delete audio file $path: ${e.message}")
    }
  }

  /**
   * Total bytes used by app-generated files (recordings, beats, artwork, etc.)
   * under internal storage. Used for the Settings > Storage information display.
   */
  fun getAppStorageUsageBytes(): Long {
    return try {
      context.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } catch (e: Exception) {
      0L
    }
  }

  /**
   * Checks if an audio file exists and is readable on disk.
   */
  fun fileExists(path: String): Boolean {
    val file = File(path)
    return file.exists() && file.length() > 0
  }

  /**
   * Extracts duration in milliseconds using MediaMetadataRetriever.
   */
  fun extractDuration(filePath: String): Long {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(filePath)
      val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      durationString?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
      Log.w("AudioFileManager", "Failed to extract duration for $filePath: ${e.message}")
      0L
    } finally {
      try {
        retriever.release()
      } catch (ignored: Exception) {}
    }
  }

  private fun getMimeTypeFromExtension(filename: String): String {
    val ext = filename.substringAfterLast(".", "").lowercase()
    return when (ext) {
      "mp3" -> "audio/mpeg"
      "wav" -> "audio/wav"
      "m4a", "aac" -> "audio/mp4"
      "ogg" -> "audio/ogg"
      "flac" -> "audio/flac"
      else -> "audio/*"
    }
  }

  private fun getExtensionForMime(mimeType: String, fallbackFilename: String): String {
    val extFromFilename = fallbackFilename.substringAfterLast(".", "").lowercase()
    if (extFromFilename in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")) {
      return extFromFilename
    }
    return when {
      mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
      mimeType.contains("wav") -> "wav"
      mimeType.contains("m4a") -> "m4a"
      mimeType.contains("aac") -> "aac"
      mimeType.contains("ogg") -> "ogg"
      mimeType.contains("flac") -> "flac"
      else -> "mp3"
    }
  }

  companion object {
    val SUPPORTED_MIME_TYPES = arrayOf(
      "audio/*",
      "audio/mpeg",
      "audio/mp3",
      "audio/wav",
      "audio/x-wav",
      "audio/mp4",
      "audio/m4a",
      "audio/x-m4a",
      "audio/aac",
      "audio/ogg",
      "audio/flac",
      "audio/x-flac"
    )
  }
}
