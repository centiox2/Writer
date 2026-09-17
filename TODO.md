# Songwriter - Task Tracking (TODO)

## Phase 1: Environment Inspection & Specifications
- [x] Inspect existing Android project, Gradle dependencies, and SDK levels.
- [x] Configure unique `applicationId`, `rootProject.name`, `app_name`, and platform `metadata.json`.
- [x] Author comprehensive blueprint specifications:
  - [x] `PROJECT_PLAN.md`
  - [x] `ARCHITECTURE.md`
  - [x] `DATA_MODEL.md`
  - [x] `AUDIO_ARCHITECTURE.md`
  - [x] `UI_DESIGN.md`
  - [x] `DEVELOPMENT_RULES.md`
  - [x] `TODO.md`
  - [x] `CHANGELOG.md`
- [x] Run initial `compile_applet` verification to validate baseline build.

---

## Phase 2: Songs & Albums Implementation
- [x] Add Room and Navigation Compose dependencies to `app/build.gradle.kts`.
- [x] Create `SongEntity`, `AlbumEntity`, `AudioTrackEntity`, `RecordingEntity`.
- [x] Create `SongDao`, `AlbumDao`, `AudioTrackDao`, `RecordingDao`.
- [x] Create `SongDatabase` Room database with foreign keys and cascade rules.
- [x] Create domain models: `Song`, `Album`, `AudioTrack`, `Recording`, `TrackType`.
- [x] Create `SongRepository`, `AlbumRepository`, and `AudioRepository`.
- [x] Songs Management:
  - [x] Create song (title, album, initial lyrics).
  - [x] Rename song dialog and database update.
  - [x] Delete song with confirmation dialog.
  - [x] Duplicate song ("(Copy)" with fresh UUID and copied lyrics/metadata).
  - [x] Favorite / unfavorite toggle.
  - [x] Archive / unarchive toggle.
  - [x] Debounced search (300ms) over title and lyrics.
  - [x] Multi-criteria sort (Recently Modified, Title A-Z, Title Z-A, Date Created).
  - [x] Lyric preview snippet on song cards.
  - [x] Formatted last modified date display.
  - [x] Real-time audio track stem indicator badge.
  - [x] Real-time recording take count indicator badge.
  - [x] Song context menu (Rename, Duplicate, Assign to Album, Archive, Delete).
  - [x] Confirmation dialogs for destructive actions.
- [x] Albums Management:
  - [x] Create album (name, description, custom artwork preset).
  - [x] Rename & edit album details (title, description, artwork).
  - [x] Delete album with confirmation dialog.
  - [x] GUARANTEE: Deleting an album does NOT delete its songs (unlinks songs safely).
  - [x] Artwork style preset picker and sleeve artwork thumbnails.
  - [x] Album description display.
  - [x] Live song count indicator.
  - [x] Album detail view with tracklist.
  - [x] Add songs to album dialog (multi-select).
  - [x] Remove songs from album (with confirmation, keeping songs intact).
  - [x] Move song between albums.
- [x] Create `SongsViewModel` and `AlbumsViewModel` with Room Flow and StateFlow.
- [x] Update `DatabaseRepositoryTest` with tests for duplicate, rename, search, album song linking, and verifying album deletion preserves songs.
- [x] Verify `compile_applet` and Robolectric unit tests.

---

## Foundation UI & Responsive Navigation
- [x] Create custom launcher icon (`ic_launcher_background.xml` and `ic_launcher_foreground.xml`).
- [x] Implement Dark Studio Console theme with Dark, Light, and System modes in `Theme.kt`, `Color.kt`, and `Type.kt`.
- [x] Create responsive `MainScaffold` supporting phone bottom navigation and tablet navigation rail.
- [x] Create main `+` action button with `NewItemBottomSheet` (New Song, New Album, Quick Voice Idea).
- [x] Create placeholder screens with reactive data observation:
  - [x] `SongsScreen` (search, filter chips, active projects list, favorite toggle)
  - [x] `AlbumsScreen` (collections list, track counters, empty state)
  - [x] `RecordingsScreen` (takes list, duration formatting, take counters)
  - [x] `SettingsScreen` (theme toggle, offline-first specs, audio architecture info)
- [x] Verify compilation and all Robolectric unit tests.

---

## Phase 3: Complete Lyric Editor
- [x] Native multiline lyric editor using `BasicTextField` with full styling and custom brush cursor.
- [x] Cursor positioning and text/word selection.
- [x] Quick editing ribbon with Copy, Paste, Cut, Select All, and Clear Selection actions.
- [x] Full Undo and Redo history stacks (up to 50 states) with immediate state recovery.
- [x] In-lyric search engine with live query filtering, total match count, active match indicator, and Next / Previous match navigation.
- [x] Live songwriting metrics: real-time word count, character count, and line count.
- [x] Typography customization modal:
  - [x] Dynamic font size adjustment (12sp to 32sp slider).
  - [x] Dynamic line spacing multiplier (1.0x to 2.2x slider).
  - [x] Font family selector (Sans-serif, Serif, Monospace).
- [x] "Keep Screen Awake" mode toggle (`FLAG_KEEP_SCREEN_ON` on window) to prevent dimming during performances and studio sessions.
- [x] Non-destructive autosave engine with 600ms debounce and visual `SaveStatus` indicator (Saved, Saving..., Unsaved changes).
- [x] Guaranteed save on leaving or backgrounding via `DisposableEffect` with `Lifecycle.Event.ON_PAUSE` / `ON_STOP` and Compose `BackHandler`.
- [x] Standard songwriting section tags insertion chips: `[Intro]`, `[Verse]`, `[Pre-Chorus]`, `[Chorus]`, `[Post-Chorus]`, `[Bridge]`, `[Hook]`, `[Outro]`.
- [x] Song sections outline sheet with one-tap jump-to-section navigation.
- [x] Keyboard handling with `imePadding()` to prevent keyboard obstruction.
- [x] Protection against accidental document clearing via confirmation dialog (`ClearLyricsDialog`).
- [x] Integration with `MainScaffold` hiding bottom navigation and FAB in editor mode.
- [x] Automated unit and integration test suite (`LyricEditorTest`) covering editing, metrics, undo/redo, selection, clipboard, section jumps, search, autosave, app restart, and process death recreation.

---

## Phase 4: Local Audio Import & Waveform Engine
- [x] Storage Access Framework (SAF) audio file picker launcher.
- [x] Support Android-compatible formats: MP3, WAV, M4A/AAC, OGG, FLAC.
- [x] Beat track management: Import, Replace, Remove beat.
- [x] Metadata extraction: filename, duration, mime type.
- [x] Playback controls: Play/pause, seeking, volume slider, loop toggle, mute toggle.
- [x] Audio focus management (`AudioManager.OnAudioFocusChangeListener`) with ducking and transient loss handling.
- [x] Audio routing: Dynamic output device detection (Earpiece, Speaker, Bluetooth, Wired Headphones).
- [x] Interruption handling: Audio focus loss, noisy broadcast (headphone unplugged).
- [x] Missing-file handling: Graceful detection and UI warning when audio file is deleted from storage.
- [x] Real waveform generation from audio:
  - [x] Asynchronous extraction using `MediaExtractor` and `MediaCodec`.
  - [x] Peak amplitude downsampling to 100 normalized points.
  - [x] Two-tier waveform caching (in-memory LruCache + persistent JSON disk cache).
  - [x] Non-blocking background coroutine processing.
  - [x] Interactive scrubbing and tap-to-seek waveform canvas.
- [x] Background playback: Instrumental beat plays concurrently while writing lyrics.
- [x] Comprehensive Robolectric unit test suite in `BeatAudioImportTest.kt`.
- [x] Compile and test verification.

---

## Phase 5: Offline Rhyme Engine & Assistant
- [x] Create `RhymeEngine` coordinating offline rhyme retrieval and scoring.
- [x] Create `PhoneticDictionary` with 1200+ embedded ARPAbet phonetic sound mappings and rhyme keys.
- [x] Create `RhymeRanker` ranking candidates by rhyme type, consonant class similarity, syllable count, and frequency.
- [x] Create `WordNormalizer` for punctuation stripping, contractions resolution, and cursor/selection word extraction.
- [x] Support manual entry and query from text input field.
- [x] Support automatic extraction of word under cursor or selected text in lyric editor.
- [x] Support Perfect rhymes, Near / Slant rhymes (assonance), and Ranked results.
- [x] One-tap "Insert at Cursor" and "Copy to Clipboard" actions.
- [x] Support punctuation, capitalization, contractions, and multi-syllable words.
- [x] Orthographic suffix and spelling fallback heuristics when word is missing from phonetic dictionary.
- [x] Fully offline operation (zero network dependencies).
- [x] Comprehensive unit test suite in `RhymeEngineTest.kt` (all passing).
- [x] Compile and verify with `compile_applet`.

---

## Phase 5: Material 3 Expressive Design System & Components
- [ ] Update `Theme.kt`, `Color.kt`, and `Type.kt` with Dark Studio Console palette.
- [ ] Build `WaveformVisualizer` (static cached waveform & live recording pulse).
- [ ] Build `AudioLevelMeter` (VU / peak decibel meter).
- [ ] Build `TrackFader` with volume slider, numeric indicator, and Mute/Solo chips.
- [ ] Build `PlaybackControlBar` with seek bar, play/pause, timecodes, and loop switch.
- [ ] Build `SectionChipRow` for fast lyric structure markup.
- [ ] Compile and verify.

---

## Phase 6: Core UI Screens
- [ ] Build `LibraryScreen` & `LibraryViewModel` (songs grid/list, album cards, search, favorites, archive).
- [ ] Build `AlbumDetailScreen` (album header, artwork, song listing, reordering).
- [ ] Build `SongStudioScreen` & `SongStudioViewModel`:
  - Multiline lyrics text editor with selection, undo/redo, word/character count.
  - Typography settings (font size, line height, keep screen awake).
  - Floating rhyme finder trigger.
  - Sticky bottom transport control bar.
- [ ] Build `RhymeFinderSheet` (syllable sections, one-tap copy/insert).
- [ ] Build `RecordingStudioSheet` (take manager, live waveform, earpiece mode switch, take auditioning).
- [ ] Build `MultiTrackMixerSheet` (channel strips, master fader, stem management).
- [ ] Compile and verify.

---

## Phase 7: Export & Backup/Restore
- [ ] Build `WavMixdownExporter` for rendering mixed audio projects to standard WAV format.
- [ ] Build `.songproject` ZIP packager (`project.json`, `lyrics.txt`, `metadata.json`, `audio/`, `artwork/`).
- [ ] Implement project importer with Merge (default) and Replace options.
- [ ] Hook up Android Storage Access Framework (SAF) document launchers.
- [ ] Compile and verify.

---

## Phase 8: Final Quality Assurance & Hardening
- [ ] Test lifecycle behavior: rotation, backgrounding, audio focus interruptions.
- [ ] Test missing file safety checks and dialog confirmations for deletes.
- [ ] Verify Robolectric / unit test suite for DAOs and audio algorithms.
- [ ] Final `compile_applet` build pass and release notes update in `CHANGELOG.md`.
