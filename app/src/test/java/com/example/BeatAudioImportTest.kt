package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioFileManager
import com.example.audio.AudioOutputRoute
import com.example.audio.BeatPlaybackState
import com.example.audio.WaveformAnalyzer
import com.example.data.database.SongDatabase
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.TrackType
import com.example.ui.viewmodels.LyricEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BeatAudioImportTest {

  private lateinit var context: Context
  private lateinit var database: SongDatabase
  private lateinit var songRepository: SongRepository
  private lateinit var audioRepository: AudioRepository
  private lateinit var audioFileManager: AudioFileManager
  private lateinit var waveformAnalyzer: WaveformAnalyzer
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, SongDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    songRepository = SongRepository(database.songDao())
    audioRepository = AudioRepository(database.audioTrackDao(), database.recordingDao())
    audioFileManager = AudioFileManager(context)
    waveformAnalyzer = WaveformAnalyzer(context)
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  @Test
  fun testSupportedAudioMimeTypesList() {
    val types = AudioFileManager.SUPPORTED_MIME_TYPES
    assertTrue(types.contains("audio/mpeg"))
    assertTrue(types.contains("audio/wav"))
    assertTrue(types.contains("audio/mp4") || types.contains("audio/m4a"))
    assertTrue(types.contains("audio/ogg"))
    assertTrue(types.contains("audio/flac"))
  }

  @Test
  fun testWaveformGenerationAndCaching() = runTest(testDispatcher) {
    // Create a dummy WAV file with a valid 44-byte header + PCM audio data
    val audioFile = File(context.cacheDir, "sample_test_beat.wav")
    FileOutputStream(audioFile).use { fos ->
      // 44-byte dummy WAV header
      val header = ByteArray(44)
      header[0] = 'R'.code.toByte()
      header[1] = 'I'.code.toByte()
      header[2] = 'F'.code.toByte()
      header[3] = 'F'.code.toByte()
      header[8] = 'W'.code.toByte()
      header[9] = 'A'.code.toByte()
      header[10] = 'V'.code.toByte()
      header[11] = 'E'.code.toByte()
      fos.write(header)
      // Write sample PCM audio samples
      val pcmData = ByteArray(2048) { (it % 120).toByte() }
      fos.write(pcmData)
    }

    val waveform = waveformAnalyzer.extractWaveform(audioFile.absolutePath, targetBuckets = 60)
    assertNotNull(waveform)
    assertEquals(60, waveform.size)

    // Verify all amplitudes are normalized between 0.08 and 1.0
    for (peak in waveform) {
      assertTrue("Peak should be >= 0.08f but was $peak", peak >= 0.08f)
      assertTrue("Peak should be <= 1.0f but was $peak", peak <= 1.0f)
    }

    // Verify memory cache hit returns identical reference
    val cachedWaveform = waveformAnalyzer.extractWaveform(audioFile.absolutePath, targetBuckets = 60)
    assertEquals(waveform.size, cachedWaveform.size)
  }

  @Test
  fun testBeatTrackPersistenceInRoom() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Neon Lights", lyrics = "Walking through the rain...")

    // Add a beat track
    val track = audioRepository.addTrack(
      songId = song.id,
      name = "trap_808_beat.mp3",
      uri = "/data/user/0/com.example/files/beats/sample.mp3",
      type = TrackType.BEAT,
      duration = 185000L
    )

    assertEquals("trap_808_beat.mp3", track.name)
    assertEquals(TrackType.BEAT, track.type)
    assertEquals(185000L, track.duration)

    // Retrieve via Flow
    val loadedTrack = audioRepository.getBeatTrackForSong(song.id).first()
    assertNotNull(loadedTrack)
    assertEquals(track.id, loadedTrack?.id)
    assertEquals("trap_808_beat.mp3", loadedTrack?.name)

    // Replace beat
    val updatedTrack = loadedTrack!!.copy(name = "synthwave_groove.wav", duration = 210000L)
    audioRepository.updateTrack(updatedTrack)

    val replacedTrack = audioRepository.getBeatTrackForSong(song.id).first()
    assertEquals("synthwave_groove.wav", replacedTrack?.name)
    assertEquals(210000L, replacedTrack?.duration)

    // Remove beat
    audioRepository.deleteTrack(track.id)
    val deletedTrack = audioRepository.getBeatTrackForSong(song.id).first()
    assertNull(deletedTrack)
  }

  @Test
  fun testMissingFileDetection() {
    val nonExistentPath = "/data/user/0/com.example/files/beats/deleted_track.mp3"
    assertFalse(audioFileManager.fileExists(nonExistentPath))

    val playbackState = BeatPlaybackState(
      isMissingFile = !audioFileManager.fileExists(nonExistentPath),
      currentFilePath = nonExistentPath
    )
    assertTrue(playbackState.isMissingFile)
  }

  @Test
  fun testBeatPlaybackStateDefaults() {
    val state = BeatPlaybackState()
    assertFalse(state.isPlaying)
    assertFalse(state.isLooping)
    assertFalse(state.isMuted)
    assertEquals(1.0f, state.volume, 0.001f)
    assertEquals(0L, state.currentPositionMs)
    assertEquals(AudioOutputRoute.SPEAKER, state.audioOutputRoute)
  }

  @Test
  fun testLyricEditorViewModelBeatIntegration() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "City Echoes", lyrics = "Chorus here...")

    // Add beat to repository
    audioRepository.addTrack(
      songId = song.id,
      name = "acoustic_guitar.wav",
      uri = "/fake/path/acoustic_guitar.wav",
      type = TrackType.BEAT,
      duration = 120000L
    )

    val viewModel = LyricEditorViewModel(
      songId = song.id,
      songRepository = songRepository,
      audioRepository = audioRepository,
      audioFileManager = audioFileManager,
      waveformAnalyzer = waveformAnalyzer,
      beatPlayer = null,
      initialSong = song
    )

    // Verify track is picked up via Flow
    val uiState = viewModel.uiState.first { it.beatTrack != null }
    assertNotNull(uiState.beatTrack)
    assertEquals("acoustic_guitar.wav", uiState.beatTrack?.name)

    // Test beat removal
    viewModel.removeBeat()
    val afterRemovalTrack = audioRepository.getBeatTrackForSong(song.id).first()
    assertNull(afterRemovalTrack)
  }
}
