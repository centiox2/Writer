# Songwriter - Project Plan

A professional offline-first Android songwriting studio inspired by the functional paradigm of dedicated mobile songwriting and lyric-writing tools, engineered with modern Jetpack Compose, Material 3 Expressive design, Room persistence, and a native audio processing engine.

---

## 1. Project Overview & Core Goals

Songwriter empowers lyricists, rappers, vocalists, and producers to write lyrics, organize concepts by album, record vocal takes, import beats and instrumentals, analyze rhymes, and mix multitrack audio directly on-device without internet connectivity, external accounts, or cloud reliance.

### Primary Pillars:
1. **Precision Lyrics Studio**: Dedicated multiline writing canvas with undo/redo, section tags, typography controls, and real-time word/character counts.
2. **Offline Rhyme Finder**: Fast phonetic and syllable-ranked rhyme matching engine.
3. **Studio Audio Engine**: High-fidelity AudioRecord take recording with real-time waveform visualization, low-latency playback, multi-track PCM mixing, volume/mute/solo controls, and earpiece communication routing for zero-bleed tracking.
4. **Structured Project Persistence**: Song-as-a-project model backed by Room SQLite, crash-safe autosave with debouncing, and portable `.songproject` backup/restore.
5. **Dark Studio Aesthetic**: Compact, high-contrast Material 3 Expressive UI optimized for focused late-night writing and studio sessions.

---

## 2. Phased Implementation Roadmap

### Phase 1: Foundation, Environment & Technical Specifications (Current)
- [x] Inspect existing repository structure, Gradle configurations, and Android toolchains.
- [x] Configure unique `applicationId`, `rootProject.name`, `app_name`, and platform `metadata.json`.
- [x] Author comprehensive technical blueprints:
  - `PROJECT_PLAN.md`
  - `ARCHITECTURE.md`
  - `DATA_MODEL.md`
  - `AUDIO_ARCHITECTURE.md`
  - `UI_DESIGN.md`
  - `DEVELOPMENT_RULES.md`
  - `TODO.md`
  - `CHANGELOG.md`
- [x] Verify base compile status with `compile_applet`.

### Phase 2: Core Data Architecture & Room Persistence
- Enable Room and Navigation Compose dependencies.
- Implement database entities: `SongEntity`, `AlbumEntity`, `AudioTrackEntity`, `RecordingEntity`.
- Implement DAOs with coroutine Flows: `SongDao`, `AlbumDao`, `AudioTrackDao`, `RecordingDao`.
- Create `SongDatabase` with TypeConverters and schema versioning.
- Implement Repository interfaces and implementations: `SongRepository`, `AlbumRepository`, `AudioRepository`.
- Implement autosave manager with debounced coroutine dispatch.

### Phase 3: Audio Core Engine & Low-Level Processing
- Implement `AudioFileManager` for sandboxed file storage, scoped cache, and unique take path generation.
- Build `AudioRecorder` utilizing `android.media.AudioRecord` for raw PCM capture, 60fps amplitude streaming, and WAV packaging.
- Implement `CommunicationModeHelper` to route monitor/beat audio to the device earpiece (`MODE_IN_COMMUNICATION`) to minimize microphone bleed.
- Build `WaveformAnalyzer` with background RMS/peak downsampling and persistent waveform caching.
- Build `AudioPlayer` and multi-track `AudioMixer` with MediaCodec/MediaExtractor decoding, synchronized playback, volume scaling, mute/solo buses, and soft-limiting.

### Phase 4: Lyrics Engine & Phonetic Rhyme Finder
- Build algorithmic and dictionary-based `PhoneticDictionary` with offline syllable and soundex/metaphone phoneme indexing.
- Implement `RhymeEngine` supporting perfect rhymes, slant rhymes, and assonance.
- Implement `RhymeRanker` ordering results by syllable count and phonetic distance.
- Integrate section tag injectors (`[Intro]`, `[Verse]`, `[Chorus]`, `[Bridge]`, `[Hook]`, `[Outro]`).

### Phase 5: Material 3 Expressive Design System & Reusable Components
- Establish dark studio color scheme, typography hierarchy, and expressive shapes.
- Build reusable UI components:
  - Interactive static & live `WaveformVisualizer`
  - Real-time `DecibelMeter` / `AudioLevelMeter`
  - Precision `TrackFader` and mute/solo toggle buttons
  - `PlaybackControlBar` with seek bar, play/pause, loop, and timecode display
  - Section chip bar and quick styling sheets
  - Responsive adaptive layout wrappers (compact phone vs. expanded tablet/landscape)

### Phase 6: Core Application Screens
- **Library Screen**: Filterable grid/list of songs, album folders, search bar, sort options, favorites toggle, archive view, project creation modal.
- **Album Screen**: Album management, cover art display, track listing, add songs to album.
- **Song Studio / Lyrics Editor Screen**: Multi-line editor with `TextFieldValue` state management, undo/redo stack, word/character metrics, live section navigation, floating rhyme finder panel, sticky audio transport bar.
- **Recording & Takes Studio**: Take manager with record/pause/resume/stop controls, live waveform canvas, take renaming, take auditioning, and earpiece toggle.
- **Multi-Track Mixer Console**: Channel strip for beat, recordings, and guides; volume faders; mute/solo buttons; master output fader; mixdown preview.

### Phase 7: Export, Backup & Restore
- Build WAV Mixdown Exporter using background coroutines, progress indicators, and cancelable jobs.
- Build `.songproject` portable archive packager (ZIP container bundling `project.json`, `lyrics.txt`, `metadata.json`, and all audio tracks/takes).
- Implement archive importer with **Merge** (default) and **Replace** strategies.
- Integrate Android Storage Access Framework (`ActivityResultContracts.CreateDocument` & `OpenDocument`) for safe user-directed export and import.

### Phase 8: Verification, Polish & Hardening
- Implement Robolectric unit tests for Room DAOs, RhymeEngine, and AudioMixer algorithms.
- Verify lifecycle resilience (onPause, onStop, orientation changes, process recreation).
- Test crash safety, missing file detection, and edge-to-edge system insets.
- Run full compilation verification and document final changelog.
