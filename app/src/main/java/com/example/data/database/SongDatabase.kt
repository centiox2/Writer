package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AlbumDao
import com.example.data.dao.AudioTrackDao
import com.example.data.dao.RecordingDao
import com.example.data.dao.SongDao
import com.example.data.entities.AlbumEntity
import com.example.data.entities.AudioTrackEntity
import com.example.data.entities.RecordingEntity
import com.example.data.entities.SongEntity

@Database(
  entities = [
    SongEntity::class,
    AlbumEntity::class,
    AudioTrackEntity::class,
    RecordingEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class SongDatabase : RoomDatabase() {
  abstract fun songDao(): SongDao
  abstract fun albumDao(): AlbumDao
  abstract fun audioTrackDao(): AudioTrackDao
  abstract fun recordingDao(): RecordingDao

  companion object {
    @Volatile
    private var INSTANCE: SongDatabase? = null

    fun getInstance(context: Context): SongDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SongDatabase::class.java,
          "songwriter.db"
        )
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
