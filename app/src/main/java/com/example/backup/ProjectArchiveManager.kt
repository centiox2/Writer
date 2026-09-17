package com.example.backup

import android.content.Context
import com.example.audio.AudioFileManager
import com.example.data.database.SongDatabase
import com.example.data.entities.AlbumEntity
import com.example.data.entities.AudioTrackEntity
import com.example.data.entities.RecordingEntity
import com.example.data.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectArchiveManager(
  private val context: Context,
  private val database: SongDatabase,
  private val audioFileManager: AudioFileManager
) {

  companion object {
    const val CURRENT_VERSION = 1
    const val APP_IDENTIFIER = "LyricStudio"
    const val PROJECT_JSON = "project.json"
    const val METADATA_JSON = "metadata.json"
    const val LYRICS_TXT = "lyrics.txt"
    const val AUDIO_DIR = "audio/"
    const val ARTWORK_DIR = "artwork/"
    const val FILE_EXTENSION = ".songproject"
  }

  // --- Export Operations ---

  suspend fun exportSong(songId: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
    val song = database.songDao().getSongById(songId)
      ?: throw IllegalArgumentException("Song not found with ID: $songId")
    val album = song.albumId?.let { database.albumDao().getAlbumById(it) }
    val tracks = database.audioTrackDao().getTracksForSongSync(songId)
    val recordings = database.recordingDao().getRecordingsForSongSync(songId)

    val albums = album?.let { listOf(it) } ?: emptyList()
    val songs = listOf(song)

    createArchive(
      type = ProjectType.SONG,
      title = song.title,
      albums = albums,
      songs = songs,
      tracksMap = mapOf(songId to tracks),
      recordingsMap = mapOf(songId to recordings),
      outputStream = outputStream
    )
  }

  suspend fun exportAlbum(albumId: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
    val album = database.albumDao().getAlbumById(albumId)
      ?: throw IllegalArgumentException("Album not found with ID: $albumId")
    val songs = database.songDao().getSongsByAlbumSync(albumId)
    val tracksMap = mutableMapOf<String, List<AudioTrackEntity>>()
    val recordingsMap = mutableMapOf<String, List<RecordingEntity>>()

    for (song in songs) {
      tracksMap[song.id] = database.audioTrackDao().getTracksForSongSync(song.id)
      recordingsMap[song.id] = database.recordingDao().getRecordingsForSongSync(song.id)
    }

    createArchive(
      type = ProjectType.ALBUM,
      title = album.name,
      albums = listOf(album),
      songs = songs,
      tracksMap = tracksMap,
      recordingsMap = recordingsMap,
      outputStream = outputStream
    )
  }

  suspend fun exportFullBackup(outputStream: OutputStream) = withContext(Dispatchers.IO) {
    val albums = database.albumDao().getAllAlbumsSync()
    val songs = database.songDao().getAllSongsSync()
    val tracksMap = mutableMapOf<String, List<AudioTrackEntity>>()
    val recordingsMap = mutableMapOf<String, List<RecordingEntity>>()

    for (song in songs) {
      tracksMap[song.id] = database.audioTrackDao().getTracksForSongSync(song.id)
      recordingsMap[song.id] = database.recordingDao().getRecordingsForSongSync(song.id)
    }

    createArchive(
      type = ProjectType.FULL_BACKUP,
      title = "LyricStudio Full Backup",
      albums = albums,
      songs = songs,
      tracksMap = tracksMap,
      recordingsMap = recordingsMap,
      outputStream = outputStream
    )
  }

  // --- Helper to export directly to a temporary cache file for sharing ---

  suspend fun exportSongToTempFile(songId: String): File = withContext(Dispatchers.IO) {
    val song = database.songDao().getSongById(songId)
      ?: throw IllegalArgumentException("Song not found")
    val sanitizedTitle = song.title.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifBlank { "song" }
    val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(cacheDir, "$sanitizedTitle$FILE_EXTENSION")
    FileOutputStream(file).use { out ->
      exportSong(songId, out)
    }
    file
  }

  suspend fun exportAlbumToTempFile(albumId: String): File = withContext(Dispatchers.IO) {
    val album = database.albumDao().getAlbumById(albumId)
      ?: throw IllegalArgumentException("Album not found")
    val sanitizedTitle = album.name.replace(Regex("[^a-zA-Z0-9.-]"), "_").ifBlank { "album" }
    val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(cacheDir, "$sanitizedTitle$FILE_EXTENSION")
    FileOutputStream(file).use { out ->
      exportAlbum(albumId, out)
    }
    file
  }

  suspend fun exportFullBackupToTempFile(): File = withContext(Dispatchers.IO) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(cacheDir, "LyricStudio_Backup_$timestamp$FILE_EXTENSION")
    FileOutputStream(file).use { out ->
      exportFullBackup(out)
    }
    file
  }

  // --- Archive Creation Engine ---

  private suspend fun createArchive(
    type: ProjectType,
    title: String,
    albums: List<AlbumEntity>,
    songs: List<SongEntity>,
    tracksMap: Map<String, List<AudioTrackEntity>>,
    recordingsMap: Map<String, List<RecordingEntity>>,
    outputStream: OutputStream
  ) {
    val zipOut = ZipOutputStream(BufferedOutputStream(outputStream))

    try {
      val audioFilesToInclude = mutableListOf<Pair<File, String>>() // local File -> zip path
      val artworkFilesToInclude = mutableListOf<Pair<File, String>>()

      var totalAudioSizeBytes = 0L

      // 1. Build project.json
      val projectJson = JSONObject().apply {
        put("version", CURRENT_VERSION)
        put("app", APP_IDENTIFIER)
        put("type", type.name)
        put("title", title)
        put("exportedAt", System.currentTimeMillis())

        val albumsArray = JSONArray()
        for (album in albums) {
          val albumObj = JSONObject().apply {
            put("id", album.id)
            put("name", album.name)
            put("description", album.description)
            put("artworkUri", album.artworkUri ?: "")
            put("createdAt", album.createdAt)
            put("updatedAt", album.updatedAt)
          }

          album.artworkUri?.let { uri ->
            if (uri.startsWith("/") || uri.startsWith("file://")) {
              val f = File(uri.removePrefix("file://"))
              if (f.exists() && f.isFile) {
                val zipArtPath = "$ARTWORK_DIR${album.id}_${f.name}"
                albumObj.put("artworkFile", zipArtPath)
                artworkFilesToInclude.add(f to zipArtPath)
              }
            }
          }

          albumsArray.put(albumObj)
        }
        put("albums", albumsArray)

        val songsArray = JSONArray()
        for (song in songs) {
          val songObj = JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("lyrics", song.lyrics)
            put("albumId", song.albumId ?: JSONObject.NULL)
            put("artworkUri", song.artworkUri ?: JSONObject.NULL)
            put("createdAt", song.createdAt)
            put("updatedAt", song.updatedAt)
            put("favorite", song.favorite)
            put("archived", song.archived)
            put("bpm", song.bpm ?: JSONObject.NULL)
            put("keySignature", song.keySignature ?: JSONObject.NULL)
          }

          song.artworkUri?.let { uri ->
            if (uri.startsWith("/") || uri.startsWith("file://")) {
              val f = File(uri.removePrefix("file://"))
              if (f.exists() && f.isFile) {
                val zipArtPath = "$ARTWORK_DIR${song.id}_${f.name}"
                songObj.put("artworkFile", zipArtPath)
                artworkFilesToInclude.add(f to zipArtPath)
              }
            }
          }

          val songTracks = tracksMap[song.id] ?: emptyList()
          val tracksArray = JSONArray()
          for (track in songTracks) {
            val trackObj = JSONObject().apply {
              put("id", track.id)
              put("songId", track.songId)
              put("name", track.name)
              put("type", track.type)
              put("duration", track.duration)
              put("volume", track.volume.toDouble())
              put("muted", track.muted)
              put("solo", track.solo)
              put("createdAt", track.createdAt)
            }

            val audioFile = File(track.uri.removePrefix("file://"))
            if (audioFile.exists() && audioFile.isFile) {
              val ext = audioFile.extension.ifBlank { "m4a" }
              val zipAudioPath = "$AUDIO_DIR${track.id}.$ext"
              trackObj.put("audioFile", zipAudioPath)
              audioFilesToInclude.add(audioFile to zipAudioPath)
              totalAudioSizeBytes += audioFile.length()
            }

            tracksArray.put(trackObj)
          }
          songObj.put("tracks", tracksArray)

          val songRecordings = recordingsMap[song.id] ?: emptyList()
          val recsArray = JSONArray()
          for (rec in songRecordings) {
            val recObj = JSONObject().apply {
              put("id", rec.id)
              put("songId", rec.songId)
              put("name", rec.name)
              put("duration", rec.duration)
              put("createdAt", rec.createdAt)
            }

            val audioFile = File(rec.uri.removePrefix("file://"))
            if (audioFile.exists() && audioFile.isFile) {
              val ext = audioFile.extension.ifBlank { "m4a" }
              val zipAudioPath = "$AUDIO_DIR${rec.id}.$ext"
              recObj.put("audioFile", zipAudioPath)
              audioFilesToInclude.add(audioFile to zipAudioPath)
              totalAudioSizeBytes += audioFile.length()
            }

            recsArray.put(recObj)
          }
          songObj.put("recordings", recsArray)

          songsArray.put(songObj)
        }
        put("songs", songsArray)
      }

      // 2. Build metadata.json
      val totalTracks = tracksMap.values.sumOf { it.size }
      val totalRecordings = recordingsMap.values.sumOf { it.size }
      val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

      val metadataJson = JSONObject().apply {
        put("title", title)
        put("type", type.name)
        put("version", CURRENT_VERSION)
        put("app", APP_IDENTIFIER)
        put("exportedAtFormatted", formattedDate)
        put("totalSongs", songs.size)
        put("totalAlbums", albums.size)
        put("totalTracks", totalTracks)
        put("totalRecordings", totalRecordings)
        put("totalAudioFiles", audioFilesToInclude.size)
        put("totalAudioSizeBytes", totalAudioSizeBytes)
      }

      // 3. Build human readable lyrics.txt
      val lyricsTxtBuilder = StringBuilder()
      lyricsTxtBuilder.append("========================================\n")
      lyricsTxtBuilder.append(" $title - LYRICS EXPORT\n")
      lyricsTxtBuilder.append(" Exported on: $formattedDate\n")
      lyricsTxtBuilder.append("========================================\n\n")

      for (song in songs) {
        val albumName = albums.find { it.id == song.albumId }?.name
        lyricsTxtBuilder.append("----------------------------------------\n")
        lyricsTxtBuilder.append(" SONG: ${song.title}\n")
        if (albumName != null) {
          lyricsTxtBuilder.append(" Album: $albumName\n")
        }
        val bpmStr = song.bpm?.let { "$it BPM" } ?: "BPM: Unset"
        val keyStr = song.keySignature?.let { "Key: $it" } ?: "Key: Unset"
        lyricsTxtBuilder.append(" Info: $bpmStr | $keyStr\n")
        lyricsTxtBuilder.append("----------------------------------------\n\n")
        lyricsTxtBuilder.append(song.lyrics.ifBlank { "[No lyrics written for this song yet]" })
        lyricsTxtBuilder.append("\n\n\n")
      }

      // Write project.json to ZIP
      writeZipStringEntry(zipOut, PROJECT_JSON, projectJson.toString(2))

      // Write metadata.json to ZIP
      writeZipStringEntry(zipOut, METADATA_JSON, metadataJson.toString(2))

      // Write lyrics.txt to ZIP
      writeZipStringEntry(zipOut, LYRICS_TXT, lyricsTxtBuilder.toString())

      // Write audio files to ZIP
      for ((file, zipPath) in audioFilesToInclude) {
        writeZipFileEntry(zipOut, file, zipPath)
      }

      // Write artwork files to ZIP
      for ((file, zipPath) in artworkFilesToInclude) {
        writeZipFileEntry(zipOut, file, zipPath)
      }

      zipOut.finish()
      zipOut.flush()
    } finally {
      // Do not close underlying output stream if caller manages it, but close zipOut
      try { zipOut.close() } catch (_: Exception) {}
    }
  }

  private fun writeZipStringEntry(zipOut: ZipOutputStream, entryName: String, content: String) {
    val entry = ZipEntry(entryName)
    zipOut.putNextEntry(entry)
    val bytes = content.toByteArray(Charsets.UTF_8)
    zipOut.write(bytes)
    zipOut.closeEntry()
  }

  private fun writeZipFileEntry(zipOut: ZipOutputStream, file: File, zipPath: String) {
    if (!file.exists() || !file.isFile) return
    val entry = ZipEntry(zipPath)
    zipOut.putNextEntry(entry)
    FileInputStream(file).use { input ->
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (input.read(buffer).also { bytesRead = it } != -1) {
        zipOut.write(buffer, 0, bytesRead)
      }
    }
    zipOut.closeEntry()
  }

  // --- Validation Engine ---

  suspend fun validateArchive(inputStream: InputStream): ValidationReport = withContext(Dispatchers.IO) {
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    var projectJsonContent: String? = null
    var metadataJsonContent: String? = null
    var lyricsTxtContent: String? = null
    val audioEntries = mutableSetOf<String>()
    val artworkEntries = mutableSetOf<String>()
    var totalAudioSizeBytes = 0L

    try {
      val zipIn = ZipInputStream(BufferedInputStream(inputStream))
      var entry: ZipEntry?

      while (zipIn.nextEntry.also { entry = it } != null) {
        val name = entry!!.name

        // Check for suspicious zip slip patterns
        if (name.contains("..") || name.startsWith("/")) {
          errors.add("Security warning: Archive contains illegal file path '$name'")
          continue
        }

        when {
          name == PROJECT_JSON -> {
            projectJsonContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
          }
          name == METADATA_JSON -> {
            metadataJsonContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
          }
          name == LYRICS_TXT -> {
            lyricsTxtContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
          }
          name.startsWith(AUDIO_DIR) && !entry!!.isDirectory -> {
            audioEntries.add(name)
            totalAudioSizeBytes += entry!!.size.takeIf { it > 0 } ?: 0L
          }
          name.startsWith(ARTWORK_DIR) && !entry!!.isDirectory -> {
            artworkEntries.add(name)
          }
        }
        zipIn.closeEntry()
      }
    } catch (e: Exception) {
      return@withContext ValidationReport(
        isValid = false,
        errors = listOf("Corrupted or invalid archive file: ${e.localizedMessage ?: e.message}")
      )
    }

    if (projectJsonContent == null) {
      return@withContext ValidationReport(
        isValid = false,
        errors = listOf("Missing required project.json descriptor. This file is not a valid LyricStudio project.")
      )
    }

    try {
      val json = JSONObject(projectJsonContent)
      val version = json.optInt("version", 0)
      if (version <= 0) {
        errors.add("Invalid or missing project version number in project.json")
      } else if (version > CURRENT_VERSION) {
        warnings.add("Project version $version is newer than current app version ($CURRENT_VERSION). Some features may not be supported.")
      }

      val typeStr = json.optString("type", "SONG")
      val type = try {
        ProjectType.valueOf(typeStr)
      } catch (_: Exception) {
        ProjectType.SONG
      }

      val title = json.optString("title", "Imported Project")
      val songsArray = json.optJSONArray("songs") ?: JSONArray()
      val albumsArray = json.optJSONArray("albums") ?: JSONArray()

      var totalTracksCount = 0
      var totalRecordingsCount = 0
      var missingAudioFiles = 0

      for (i in 0 until songsArray.length()) {
        val songObj = songsArray.getJSONObject(i)
        val tracksArray = songObj.optJSONArray("tracks") ?: JSONArray()
        totalTracksCount += tracksArray.length()
        for (j in 0 until tracksArray.length()) {
          val t = tracksArray.getJSONObject(j)
          val audioFile = t.optString("audioFile", "")
          if (audioFile.isNotBlank() && !audioEntries.contains(audioFile)) {
            missingAudioFiles++
          }
        }

        val recsArray = songObj.optJSONArray("recordings") ?: JSONArray()
        totalRecordingsCount += recsArray.length()
        for (j in 0 until recsArray.length()) {
          val r = recsArray.getJSONObject(j)
          val audioFile = r.optString("audioFile", "")
          if (audioFile.isNotBlank() && !audioEntries.contains(audioFile)) {
            missingAudioFiles++
          }
        }
      }

      if (missingAudioFiles > 0) {
        warnings.add("$missingAudioFiles audio take/track files are missing from archive audio folder.")
      }

      val lyricsPreview = lyricsTxtContent?.take(500) ?: ""

      ValidationReport(
        isValid = errors.isEmpty(),
        version = version,
        type = type,
        title = title,
        songCount = songsArray.length(),
        albumCount = albumsArray.length(),
        trackCount = totalTracksCount,
        recordingCount = totalRecordingsCount,
        totalAudioSizeBytes = totalAudioSizeBytes,
        missingAudioFilesCount = missingAudioFiles,
        errors = errors,
        warnings = warnings,
        lyricsPreview = lyricsPreview
      )
    } catch (e: Exception) {
      ValidationReport(
        isValid = false,
        errors = listOf("Malformed project.json schema: ${e.localizedMessage ?: e.message}")
      )
    }
  }

  // --- Import Engine ---

  suspend fun importArchive(
    inputStream: InputStream,
    mode: ImportMode = ImportMode.MERGE
  ): ImportReport = withContext(Dispatchers.IO) {
    // 1. Create a safe temporary staging folder
    val stagingDir = File(context.cacheDir, "import_staging_${UUID.randomUUID()}").apply { mkdirs() }

    try {
      // 2. Unpack archive to staging directory with Zip-Slip protection
      val zipIn = ZipInputStream(BufferedInputStream(inputStream))
      var entry: ZipEntry?
      val canonicalStagingPath = stagingDir.canonicalPath

      while (zipIn.nextEntry.also { entry = it } != null) {
        val destinationFile = File(stagingDir, entry!!.name)
        val canonicalDestPath = destinationFile.canonicalPath

        if (!canonicalDestPath.startsWith(canonicalStagingPath)) {
          throw SecurityException("Zip Slip attack detected in entry: ${entry!!.name}")
        }

        if (entry!!.isDirectory) {
          destinationFile.mkdirs()
        } else {
          destinationFile.parentFile?.mkdirs()
          FileOutputStream(destinationFile).use { fileOut ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
              fileOut.write(buffer, 0, bytesRead)
            }
          }
        }
        zipIn.closeEntry()
      }

      // 3. Read and validate project.json
      val projectJsonFile = File(stagingDir, PROJECT_JSON)
      if (!projectJsonFile.exists()) {
        throw IllegalArgumentException("Archive does not contain project.json")
      }

      val jsonString = projectJsonFile.readText(Charsets.UTF_8)
      val json = JSONObject(jsonString)
      val version = json.optInt("version", 1)
      val typeStr = json.optString("type", "SONG")
      val type = try { ProjectType.valueOf(typeStr) } catch (_: Exception) { ProjectType.SONG }

      val albumsArray = json.optJSONArray("albums") ?: JSONArray()
      val songsArray = json.optJSONArray("songs") ?: JSONArray()

      // 4. Handle REPLACE mode if specified
      if (mode == ImportMode.REPLACE) {
        if (type == ProjectType.FULL_BACKUP) {
          // Clear all current records
          val currentSongs = database.songDao().getAllSongsSync()
          for (s in currentSongs) {
            database.songDao().deleteSongById(s.id)
          }
          val currentAlbums = database.albumDao().getAllAlbumsSync()
          for (a in currentAlbums) {
            database.albumDao().deleteAlbumById(a.id)
          }
        }
      }

      var importedAlbums = 0
      var importedSongs = 0
      var importedTracks = 0
      var importedRecordings = 0

      val albumIdMap = mutableMapOf<String, String>() // old album ID -> new album ID
      val songIdMap = mutableMapOf<String, String>()   // old song ID -> new song ID

      // 5. Import Albums
      for (i in 0 until albumsArray.length()) {
        val albumObj = albumsArray.getJSONObject(i)
        val origAlbumId = albumObj.getString("id")
        val name = albumObj.getString("name")
        val desc = albumObj.optString("description", "")
        val origArtUri = albumObj.optString("artworkUri", null)
        val artFileRel = albumObj.optString("artworkFile", null)

        var finalArtworkUri: String? = origArtUri
        if (!artFileRel.isNullOrBlank()) {
          val stagedArtFile = File(stagingDir, artFileRel)
          if (stagedArtFile.exists() && stagedArtFile.isFile) {
            val destArtFile = File(context.filesDir, "artwork/art_${UUID.randomUUID()}.${stagedArtFile.extension}").apply {
              parentFile?.mkdirs()
            }
            stagedArtFile.copyTo(destArtFile, overwrite = true)
            finalArtworkUri = destArtFile.absolutePath
          }
        }

        val targetAlbumId = if (mode == ImportMode.MERGE) {
          // Check if an album with same ID exists
          val existing = database.albumDao().getAlbumById(origAlbumId)
          if (existing != null) {
            origAlbumId // Merge under existing album
          } else {
            origAlbumId
          }
        } else {
          // In replace mode, delete if exists
          database.albumDao().deleteAlbumById(origAlbumId)
          origAlbumId
        }

        albumIdMap[origAlbumId] = targetAlbumId

        val albumEntity = AlbumEntity(
          id = targetAlbumId,
          name = name,
          description = desc,
          artworkUri = finalArtworkUri,
          createdAt = albumObj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = albumObj.optLong("updatedAt", System.currentTimeMillis())
        )
        database.albumDao().insertAlbum(albumEntity)
        importedAlbums++
      }

      // 6. Import Songs & their Tracks and Recordings
      for (i in 0 until songsArray.length()) {
        val songObj = songsArray.getJSONObject(i)
        val origSongId = songObj.getString("id")
        val title = songObj.getString("title")
        val lyrics = songObj.optString("lyrics", "")
        val origAlbumId = songObj.optString("albumId", null).takeIf { it != "null" }
        val targetAlbumId = origAlbumId?.let { albumIdMap[it] ?: it }
        val origArtUri = songObj.optString("artworkUri", null).takeIf { it != "null" }
        val artFileRel = songObj.optString("artworkFile", null)
        val favorite = songObj.optBoolean("favorite", false)
        val archived = songObj.optBoolean("archived", false)
        val bpm = if (songObj.has("bpm") && !songObj.isNull("bpm")) songObj.getInt("bpm") else null
        val keySig = songObj.optString("keySignature", null).takeIf { it != "null" }

        var finalArtworkUri: String? = origArtUri
        if (!artFileRel.isNullOrBlank()) {
          val stagedArtFile = File(stagingDir, artFileRel)
          if (stagedArtFile.exists() && stagedArtFile.isFile) {
            val destArtFile = File(context.filesDir, "artwork/art_${UUID.randomUUID()}.${stagedArtFile.extension}").apply {
              parentFile?.mkdirs()
            }
            stagedArtFile.copyTo(destArtFile, overwrite = true)
            finalArtworkUri = destArtFile.absolutePath
          }
        }

        val targetSongId: String
        if (mode == ImportMode.MERGE) {
          val existingSong = database.songDao().getSongById(origSongId)
          targetSongId = if (existingSong != null) {
            // Generate a fresh ID for the merged copy so existing song is unaffected
            UUID.randomUUID().toString()
          } else {
            origSongId
          }
        } else {
          // Replace mode
          database.songDao().deleteSongById(origSongId)
          targetSongId = origSongId
        }

        songIdMap[origSongId] = targetSongId

        val songEntity = SongEntity(
          id = targetSongId,
          title = if (mode == ImportMode.MERGE && targetSongId != origSongId) "$title (Imported)" else title,
          lyrics = lyrics,
          albumId = targetAlbumId,
          artworkUri = finalArtworkUri,
          createdAt = songObj.optLong("createdAt", System.currentTimeMillis()),
          updatedAt = songObj.optLong("updatedAt", System.currentTimeMillis()),
          favorite = favorite,
          archived = archived,
          bpm = bpm,
          keySignature = keySig
        )
        database.songDao().insertSong(songEntity)
        importedSongs++

        // Import Mixer Tracks for this song
        val tracksArray = songObj.optJSONArray("tracks") ?: JSONArray()
        for (j in 0 until tracksArray.length()) {
          val trackObj = tracksArray.getJSONObject(j)
          val origTrackId = trackObj.getString("id")
          val trackName = trackObj.getString("name")
          val trackType = trackObj.optString("type", "RECORDING")
          val duration = trackObj.optLong("duration", 0L)
          val volume = trackObj.optDouble("volume", 1.0).toFloat()
          val muted = trackObj.optBoolean("muted", false)
          val solo = trackObj.optBoolean("solo", false)
          val audioFileRel = trackObj.optString("audioFile", null)

          var destAudioUri = ""
          if (!audioFileRel.isNullOrBlank()) {
            val stagedAudio = File(stagingDir, audioFileRel)
            if (stagedAudio.exists() && stagedAudio.isFile) {
              val destFile = File(context.filesDir, "audio_files/track_${UUID.randomUUID()}.${stagedAudio.extension}").apply {
                parentFile?.mkdirs()
              }
              stagedAudio.copyTo(destFile, overwrite = true)
              destAudioUri = destFile.absolutePath
            }
          }

          val newTrackId = UUID.randomUUID().toString()
          val trackEntity = AudioTrackEntity(
            id = newTrackId,
            songId = targetSongId,
            name = trackName,
            uri = destAudioUri,
            type = trackType,
            duration = duration,
            volume = volume,
            muted = muted,
            solo = solo,
            createdAt = trackObj.optLong("createdAt", System.currentTimeMillis())
          )
          database.audioTrackDao().insertTrack(trackEntity)
          importedTracks++
        }

        // Import Recording takes for this song
        val recsArray = songObj.optJSONArray("recordings") ?: JSONArray()
        for (j in 0 until recsArray.length()) {
          val recObj = recsArray.getJSONObject(j)
          val origRecId = recObj.getString("id")
          val recName = recObj.getString("name")
          val duration = recObj.optLong("duration", 0L)
          val audioFileRel = recObj.optString("audioFile", null)

          var destAudioUri = ""
          if (!audioFileRel.isNullOrBlank()) {
            val stagedAudio = File(stagingDir, audioFileRel)
            if (stagedAudio.exists() && stagedAudio.isFile) {
              val destFile = File(context.filesDir, "recordings/take_${UUID.randomUUID()}.${stagedAudio.extension}").apply {
                parentFile?.mkdirs()
              }
              stagedAudio.copyTo(destFile, overwrite = true)
              destAudioUri = destFile.absolutePath
            }
          }

          val newRecId = UUID.randomUUID().toString()
          val recEntity = RecordingEntity(
            id = newRecId,
            songId = targetSongId,
            name = recName,
            uri = destAudioUri,
            duration = duration,
            createdAt = recObj.optLong("createdAt", System.currentTimeMillis())
          )
          database.recordingDao().insertRecording(recEntity)
          importedRecordings++
        }
      }

      ImportReport(
        isSuccess = true,
        importedSongsCount = importedSongs,
        importedAlbumsCount = importedAlbums,
        importedTracksCount = importedTracks,
        importedRecordingsCount = importedRecordings,
        mode = mode,
        message = "Successfully imported $importedSongs song(s), $importedAlbums album(s), $importedTracks track(s), and $importedRecordings take(s)."
      )
    } catch (e: Exception) {
      ImportReport(
        isSuccess = false,
        error = e.localizedMessage ?: e.message ?: "Unknown error occurred during import"
      )
    } finally {
      // Clean up staging directory recursively
      stagingDir.deleteRecursively()
    }
  }
}
