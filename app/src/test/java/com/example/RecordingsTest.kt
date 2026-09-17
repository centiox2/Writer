package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioFileManager
import com.example.audio.AudioRecorderEngine
import com.example.audio.RecordingPreviewPlayer
import com.example.audio.WaveformAnalyzer
import com.example.data.database.SongDatabase
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.TrackType
import com.example.ui.viewmodels.RecordingsViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RecordingsTest {

  private lateinit var context: Context
  private lateinit var database: SongDatabase
  private lateinit var songRepository: SongRepository
  private lateinit var audioRepository: AudioRepository
  private lateinit var audioFileManager: AudioFileManager
  private lateinit var waveformAnalyzer: WaveformAnalyzer
  private lateinit var previewPlayer: RecordingPreviewPlayer
  private lateinit var recorderEngine: AudioRecorderEngine
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    database = Room.inMemoryDatabaseBuilder(context, SongDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    songRepository = SongRepository(database.songDao())
    audioRepository = AudioRepository(database.audioTrackDao(), database.recordingDao())
    audioFileManager = AudioFileManager(context)
    waveformAnalyzer = WaveformAnalyzer(context)
    previewPlayer = RecordingPreviewPlayer(context)
    recorderEngine = AudioRecorderEngine(context)
  }

  @After
  fun teardown() {
    database.close()
    previewPlayer.release()
    recorderEngine.release()
    Dispatchers.resetMain()
  }

  @Test
  fun testUnlimitedPracticalRecordingTakesPerSong() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Magnum Opus")

    // Create 100 takes for this song to verify unlimited capability
    val takeCount = 100
    for (i in 1..takeCount) {
      val testFile = File(context.filesDir, "take_$i.m4a").apply {
        if (!exists()) writeBytes(ByteArray(128))
      }
      audioRepository.addRecording(
        songId = song.id,
        name = "Take $i - Lead Vocal",
        uri = testFile.absolutePath,
        duration = 45000L + (i * 1000L)
      )
    }

    val takes = audioRepository.getRecordingsForSong(song.id).first()
    assertEquals(takeCount, takes.size)
    assertEquals("Take 100 - Lead Vocal", takes[0].name)
    assertEquals("Take 1 - Lead Vocal", takes[99].name)
  }

  @Test
  fun testRenameTake() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Summer Breeze")
    val take = audioRepository.addRecording(
      songId = song.id,
      name = "Draft Take 1",
      uri = "/fake/path/take1.m4a",
      duration = 30000L
    )

    audioRepository.renameRecording(take.id, "Polished Chorus Take")

    val updated = audioRepository.getRecordingByIdSync(take.id)
    assertNotNull(updated)
    assertEquals("Polished Chorus Take", updated?.name)
  }

  @Test
  fun testDeleteTake() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Midnight Jam")
    val take = audioRepository.addRecording(
      songId = song.id,
      name = "Bad Take",
      uri = "/fake/path/take_bad.m4a",
      duration = 15000L
    )

    assertEquals(1, audioRepository.getRecordingsForSong(song.id).first().size)

    audioRepository.deleteRecording(take.id)

    val afterDelete = audioRepository.getRecordingsForSong(song.id).first()
    assertTrue(afterDelete.isEmpty())
  }

  @Test
  fun testDuplicateTake() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Harmony Project")
    val take = audioRepository.addRecording(
      songId = song.id,
      name = "Main Vocal Take",
      uri = "/fake/path/take_main.m4a",
      duration = 60000L
    )

    val duplicated = audioRepository.duplicateRecording(take.id)
    assertNotNull(duplicated)
    assertEquals("Main Vocal Take (Copy)", duplicated?.name)
    assertEquals(take.songId, duplicated?.songId)
    assertEquals(take.duration, duplicated?.duration)

    val allTakes = audioRepository.getRecordingsForSong(song.id).first()
    assertEquals(2, allTakes.size)
  }

  @Test
  fun testAttachTakeToMixer() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Studio Track")
    val take = audioRepository.addRecording(
      songId = song.id,
      name = "Hook Vocal Take 1",
      uri = "/fake/path/hook.m4a",
      duration = 42000L
    )

    // Attach as vocal track
    val track = audioRepository.attachRecordingToMixer(
      recordingId = take.id,
      targetSongId = song.id,
      trackType = TrackType.VOCAL
    )

    assertNotNull(track)
    assertEquals(take.name, track?.name)
    assertEquals(take.uri, track?.uri)
    assertEquals(TrackType.VOCAL, track?.type)
    assertEquals(42000L, track?.duration)

    // Verify track is now visible in mixer tracks for song
    val mixerTracks = audioRepository.getTracksForSongSync(song.id)
    assertEquals(1, mixerTracks.size)
    assertEquals(track?.id, mixerTracks[0].id)
  }

  @Test
  fun testRecordingsViewModelFilteringAndSearch() = runTest(testDispatcher) {
    val song1 = songRepository.createSong(title = "Acoustic Ballad")
    val song2 = songRepository.createSong(title = "Electronic Anthem")

    audioRepository.addRecording(song1.id, "Acoustic Intro Guitar", "/path/g1.m4a", 20000L)
    audioRepository.addRecording(song1.id, "Acoustic Verse Vocal", "/path/v1.m4a", 35000L)
    audioRepository.addRecording(song2.id, "Synth Lead Solo", "/path/s1.m4a", 15000L)

    val viewModel = RecordingsViewModel(
      audioRepository = audioRepository,
      songRepository = songRepository,
      audioFileManager = audioFileManager,
      waveformAnalyzer = waveformAnalyzer,
      previewPlayer = previewPlayer,
      recorderEngine = recorderEngine
    )

    viewModel.refreshDataSync()

    // Filter by song1
    viewModel.setSongFilter(song1.id)
    assertEquals(song1.id, viewModel.uiState.value.selectedSongFilterId)
    assertEquals(2, viewModel.uiState.value.recordings.size)

    // Search within filtered or reset filter
    viewModel.setSongFilter(null)
    viewModel.setSearchQuery("Synth")
    assertEquals(1, viewModel.uiState.value.recordings.size)
    assertEquals("Synth Lead Solo", viewModel.uiState.value.recordings[0].recording.name)
  }

  @Test
  fun testMuteAndSoloToggles() = runTest(testDispatcher) {
    val song = songRepository.createSong(title = "Vocals Only")
    val take = audioRepository.addRecording(song.id, "Harmonies", "/path/h1.m4a", 18000L)

    val viewModel = RecordingsViewModel(
      audioRepository = audioRepository,
      songRepository = songRepository,
      audioFileManager = audioFileManager,
      waveformAnalyzer = waveformAnalyzer,
      previewPlayer = previewPlayer,
      recorderEngine = recorderEngine
    )

    // Toggle Mute
    viewModel.toggleMute(take.id)
    assertTrue(viewModel.uiState.value.isMuted)

    viewModel.toggleMute(take.id)
    assertFalse(viewModel.uiState.value.isMuted)

    // Toggle Solo
    viewModel.toggleSolo(take.id)
    assertEquals(take.id, viewModel.uiState.value.soloedRecordingId)

    viewModel.toggleSolo(take.id)
    assertNull(viewModel.uiState.value.soloedRecordingId)
  }
}
