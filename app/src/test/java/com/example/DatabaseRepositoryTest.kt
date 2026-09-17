package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.SongDatabase
import com.example.data.repositories.AlbumRepository
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository
import com.example.domain.models.TrackType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseRepositoryTest {

  private lateinit var database: SongDatabase
  private lateinit var songRepository: SongRepository
  private lateinit var albumRepository: AlbumRepository
  private lateinit var audioRepository: AudioRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, SongDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    songRepository = SongRepository(database.songDao())
    albumRepository = AlbumRepository(database.albumDao(), database.songDao())
    audioRepository = AudioRepository(database.audioTrackDao(), database.recordingDao())
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun testCreateAndRetrieveSong() = runBlocking {
    val song = songRepository.createSong(title = "Sunset Melody", lyrics = "First line of the verse")
    assertNotNull(song.id)

    val activeSongs = songRepository.getActiveSongs().first()
    assertEquals(1, activeSongs.size)
    assertEquals("Sunset Melody", activeSongs[0].title)
    assertEquals("First line of the verse", activeSongs[0].lyrics)
  }

  @Test
  fun testRenameAndDuplicateSong() = runBlocking {
    val original = songRepository.createSong(title = "Rough Demo", lyrics = "Chorus line here")
    songRepository.renameSong(original.id, "Final Master Demo")

    val renamed = songRepository.getSongByIdSync(original.id)
    assertEquals("Final Master Demo", renamed?.title)

    val duplicate = songRepository.duplicateSong(original.id)
    assertNotNull(duplicate)
    assertTrue(duplicate!!.title.contains("Copy"))
    assertEquals("Chorus line here", duplicate.lyrics)
    assertFalse(duplicate.id == original.id)

    val allSongs = songRepository.getActiveSongs().first()
    assertEquals(2, allSongs.size)
  }

  @Test
  fun testSongFavoriteAndArchive() = runBlocking {
    val song = songRepository.createSong(title = "Hit Track")

    songRepository.updateFavorite(song.id, true)
    val favorites = songRepository.getFavoriteSongs().first()
    assertEquals(1, favorites.size)
    assertTrue(favorites[0].favorite)

    songRepository.updateArchived(song.id, true)
    val active = songRepository.getActiveSongs().first()
    assertEquals(0, active.size)

    val archived = songRepository.getArchivedSongs().first()
    assertEquals(1, archived.size)
  }

  @Test
  fun testSearchSongs() = runBlocking {
    songRepository.createSong(title = "Electric Blue", lyrics = "Dancing in the shadows")
    songRepository.createSong(title = "Acoustic Sun", lyrics = "Morning light shines")

    val searchTitle = songRepository.searchSongs("Electric").first()
    assertEquals(1, searchTitle.size)
    assertEquals("Electric Blue", searchTitle[0].title)

    val searchLyrics = songRepository.searchSongs("shadows").first()
    assertEquals(1, searchLyrics.size)
    assertEquals("Electric Blue", searchLyrics[0].title)
  }

  @Test
  fun testAlbumCreationAndSongLinking() = runBlocking {
    val album = albumRepository.createAlbum(name = "Debut EP", description = "My first 4 tracks")
    val song = songRepository.createSong(title = "Track One", albumId = album.id)

    val albumSongs = songRepository.getSongsByAlbum(album.id).first()
    assertEquals(1, albumSongs.size)
    assertEquals("Track One", albumSongs[0].title)

    val albums = albumRepository.getAllAlbums().first()
    assertEquals(1, albums.size)
    assertEquals(1, albums[0].songCount)
  }

  @Test
  fun testDeletingAlbumDoesNotDeleteSongs() = runBlocking {
    val album = albumRepository.createAlbum(name = "Mixtape 2026")
    val song1 = songRepository.createSong(title = "Intro", albumId = album.id)
    val song2 = songRepository.createSong(title = "Outro", albumId = album.id)

    // Verify songs are linked to album
    val beforeDelete = albumRepository.getAllAlbums().first()
    assertEquals(1, beforeDelete.size)
    assertEquals(2, beforeDelete[0].songCount)

    // Delete the album
    albumRepository.deleteAlbum(album.id)

    // Verify album is gone
    val afterDelete = albumRepository.getAllAlbums().first()
    assertEquals(0, afterDelete.size)

    // CRITICAL: Verify both songs still exist and are safe in the library!
    val remainingSongs = songRepository.getActiveSongs().first()
    assertEquals(2, remainingSongs.size)
    val remainingSong1 = songRepository.getSongByIdSync(song1.id)
    val remainingSong2 = songRepository.getSongByIdSync(song2.id)
    assertNotNull(remainingSong1)
    assertNotNull(remainingSong2)
    assertNull(remainingSong1?.albumId)
    assertNull(remainingSong2?.albumId)
  }

  @Test
  fun testAddAndRemoveSongFromAlbum() = runBlocking {
    val album = albumRepository.createAlbum(name = "Concept Album")
    val song = songRepository.createSong(title = "Single Track")

    assertNull(song.albumId)

    // Add to album
    albumRepository.addSongToAlbum(song.id, album.id)
    var updatedSong = songRepository.getSongByIdSync(song.id)
    assertEquals(album.id, updatedSong?.albumId)

    // Remove from album
    albumRepository.removeSongFromAlbum(song.id)
    updatedSong = songRepository.getSongByIdSync(song.id)
    assertNull(updatedSong?.albumId)
  }

  @Test
  fun testAudioTrackAndRecording() = runBlocking {
    val song = songRepository.createSong(title = "Studio Session")

    val track = audioRepository.addTrack(
      songId = song.id,
      name = "Trap Beat 140BPM",
      uri = "file:///data/audio/beat.mp3",
      type = TrackType.BEAT,
      duration = 180000L
    )
    assertNotNull(track.id)

    val recording = audioRepository.addRecording(
      songId = song.id,
      name = "Hook Take 1",
      uri = "/data/recordings/take_1.wav",
      duration = 32000L
    )
    assertNotNull(recording.id)

    val tracks = audioRepository.getTracksForSong(song.id).first()
    assertEquals(1, tracks.size)

    val recordings = audioRepository.getRecordingsForSong(song.id).first()
    assertEquals(1, recordings.size)
  }
}
