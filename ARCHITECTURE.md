# Songwriter - Architecture Specification

This document details the system architecture, component boundaries, data flow pipelines, and separation of concerns for the Songwriter application.

---

## 1. Architectural Pattern: Clean Architecture + MVVM + UDF

Songwriter is designed with strict layer separation to guarantee offline reliability, testability, and responsiveness during real-time audio tasks.

```
┌─────────────────────────────────────────────────────────────┐
│                    UI LAYER (Jetpack Compose)               │
│  - Screens: Library, StudioEditor, AlbumDetail, MixerSheet   │
│  - ViewModels: LibraryViewModel, SongStudioViewModel, etc.  │
│  - Design System: Material 3 Expressive, Dark Studio Palette│
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow (UI State)
                               │ Actions / Events
┌──────────────────────────────▼──────────────────────────────┐
│                   DOMAIN LAYER (Kotlin Pure)                │
│  - Models: Song, Album, AudioTrack, Recording, RhymeResult  │
│  - Use Cases: SaveLyrics, Mixdown, SearchRhymes, Backup     │
└───────────────────┬───────────────────────┬─────────────────┘
                    │                       │
┌───────────────────▼───────────┐ ┌─────────▼─────────────────┐
│     DATA LAYER (Room + I/O)   │ │      AUDIO & LYRICS       │
│ - SQLite Room Database & DAOs │ │ - AudioEngine & Recorder  │
│ - Backup/Restore Zip Engine   │ │ - Multi-track AudioMixer  │
│ - Scoped AudioFileManager     │ │ - RhymeEngine & Phonetics │
└───────────────────────────────┘ └───────────────────────────┘
```

---

## 2. Directory Structure & Module Responsibilities

```
app/src/main/java/com/example/
├── data/
│   ├── database/
│   │   ├── SongDatabase.kt               # Room database definition
│   │   └── Converters.kt                 # TypeConverters for timestamps, track types, lists
│   ├── dao/
│   │   ├── SongDao.kt                    # Song CRUD & search queries
│   │   ├── AlbumDao.kt                   # Album queries with relation counts
│   │   ├── AudioTrackDao.kt              # Track queries scoped to song
│   │   └── RecordingDao.kt               # Recording take queries
│   ├── entities/
│   │   ├── SongEntity.kt                 # Database table schema for songs
│   │   ├── AlbumEntity.kt                # Database table schema for albums
│   │   ├── AudioTrackEntity.kt           # Database table schema for tracks
│   │   └── RecordingEntity.kt            # Database table schema for takes
│   └── repositories/
│       ├── SongRepository.kt             # Data mediation for songs and autosave
│       ├── AlbumRepository.kt            # Data mediation for albums
│       ├── AudioRepository.kt            # Mediation for audio tracks and recordings
│       └── BackupRepository.kt           # Import/Export .songproject packaging
│
├── domain/
│   ├── models/
│   │   ├── Song.kt                       # Domain representation of song project
│   │   ├── Album.kt                      # Domain representation of album
│   │   ├── AudioTrack.kt                 # Domain representation of audio track
│   │   ├── Recording.kt                  # Domain representation of vocal take
│   │   ├── TrackType.kt                  # BEAT, VOCAL, RECORDING, GUIDE, INSTRUMENTAL
│   │   ├── RhymeMatch.kt                 # Rhyme scoring and phonetic match
│   │   └── ProjectBackup.kt              # Structured project bundle representation
│   └── usecases/
│       ├── SaveLyricsUseCase.kt          # Debounced lyrics saving logic
│       ├── SearchRhymesUseCase.kt        # Syllable and phonetic rhyme filtering
│       └── MixAndExportWavUseCase.kt     # Background PCM mixing and WAV encoding
│
├── audio/
│   ├── AudioEngine.kt                    # Coordinator for playback, recording, and timeline
│   ├── AudioPlayer.kt                    # Low-latency playback engine with position tracking
│   ├── AudioRecorder.kt                  # Native AudioRecord with PCM streaming & live meter
│   ├── AudioMixer.kt                     # Software multitrack mixer, resampler & limiter
│   ├── WaveformAnalyzer.kt               # Fast background peak/RMS extractor with caching
│   ├── AudioFileManager.kt               # Sandboxed audio file management and safety checks
│   └── CommunicationModeHelper.kt        # Audio routing for earpiece bleed-prevention
│
├── lyrics/
│   ├── RhymeEngine.kt                    # Offline rhyme finder algorithms
│   ├── PhoneticDictionary.kt             # Phonetic transcriptions and sound patterns
│   └── RhymeRanker.kt                    # Scoring by syllable count, meter, and phonetics
│
└── ui/
    ├── navigation/
    │   └── Screen.kt                     # Type-safe navigation routes and arguments
    ├── theme/
    │   ├── Color.kt                      # Dark studio color definitions
    │   ├── Theme.kt                      # Material 3 Expressive theme setup
    │   └── Type.kt                       # Typography hierarchy
    ├── components/
    │   ├── WaveformVisualizer.kt         # Canvas-based static & dynamic waveform
    │   ├── AudioLevelMeter.kt            # Real-time VU / decibel amplitude meter
    │   ├── PlaybackControlBar.kt         # Sticky transport control bar
    │   ├── TrackFader.kt                 # Precision volume slider with mute/solo
    │   └── SectionChipRow.kt             # Quick lyric section tags
    ├── library/
    │   ├── LibraryScreen.kt              # Main project dashboard
    │   └── LibraryViewModel.kt           # Search, sort, and collection state
    ├── editor/
    │   ├── SongStudioScreen.kt           # Lyrics editing, rhyme helper & recording host
    │   ├── SongStudioViewModel.kt        # Editor state, undo/redo, autosave, audio state
    │   └── UndoRedoManager.kt            # Command-based text history buffer
    ├── albums/
    │   ├── AlbumsScreen.kt               # Album management and track counts
    │   └── AlbumDetailScreen.kt          # Album details and song reordering
    ├── rhyme/
    │   └── RhymeFinderSheet.kt           # Floating rhyme search bottom sheet
    ├── recorder/
    │   └── RecordingStudioSheet.kt       # Take recording, live meter, earpiece switch
    └── mixer/
        └── MultiTrackMixerSheet.kt       # Multitrack console, gain sliders, WAV export
```

---

## 3. Unidirectional Data Flow (UDF) & State Management

Each screen is governed by a dedicated `ViewModel` exposing an immutable `StateFlow<UiState>`:

1. **User Action**: The UI composable sends an event or user intent (e.g. `onLyricsChanged`, `onStartRecording`, `onSetTrackVolume`) to the ViewModel.
2. **Business Logic Execution**: The ViewModel invokes use cases or repositories using coroutine scopes (`viewModelScope.launch`).
3. **State Mutation**: Results update a private `MutableStateFlow<UiState>`.
4. **State Collection**: The Composable collects `uiState.collectAsStateWithLifecycle()` to render deterministic UI updates without unwanted recompositions.

---

## 4. Concurrency & Threading Model

- **UI Thread (Main)**: Rendering Composables, handling user gestures, navigation.
- **Data Thread (`Dispatchers.IO`)**: SQLite Room queries, JSON serialization, `.songproject` ZIP compression/extraction.
- **Audio Decoding & Mixing (`Dispatchers.Default` / Dedicated HandlerThread)**:
  - Real-time `AudioRecord` thread running at high thread priority (`THREAD_PRIORITY_URGENT_AUDIO`) to prevent audio buffer underruns.
  - Asynchronous background decoding via `MediaCodec` and multi-track PCM buffer alignment.
  - Waveform extraction running in cooperative background coroutines with downsampling.
