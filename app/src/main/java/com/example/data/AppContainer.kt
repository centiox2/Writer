package com.example.data

import android.content.Context
import com.example.audio.AudioFileManager
import com.example.audio.WaveformAnalyzer
import com.example.data.database.SongDatabase
import com.example.data.repositories.AlbumRepository
import com.example.data.repositories.AudioRepository
import com.example.data.repositories.SongRepository

interface AppContainer {
  val songRepository: SongRepository
  val albumRepository: AlbumRepository
  val audioRepository: AudioRepository
  val audioFileManager: AudioFileManager
  val waveformAnalyzer: WaveformAnalyzer
}

class DefaultAppContainer(private val context: Context) : AppContainer {
  private val database: SongDatabase by lazy {
    SongDatabase.getInstance(context)
  }

  override val songRepository: SongRepository by lazy {
    SongRepository(database.songDao())
  }

  override val albumRepository: AlbumRepository by lazy {
    AlbumRepository(database.albumDao(), database.songDao())
  }

  override val audioRepository: AudioRepository by lazy {
    AudioRepository(database.audioTrackDao(), database.recordingDao())
  }

  override val audioFileManager: AudioFileManager by lazy {
    AudioFileManager(context)
  }

  override val waveformAnalyzer: WaveformAnalyzer by lazy {
    WaveformAnalyzer(context)
  }
}
