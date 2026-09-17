# Changelog

All notable changes to the Songwriter application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.6.0-alpha] - 2026-09-17
### Phase 5: Offline Rhyme Engine & Songwriting Assistant
- **Phonetic Rhyme Engine**:
  - Implemented `RhymeEngine` with offline phonetic analysis coordinating `PhoneticDictionary`, `RhymeRanker`, and `WordNormalizer`.
  - Embedded curated ARPAbet phonetic sound dictionary with 1,200+ core lyric words indexed by rhyme key (nucleus + coda) and vowel phone.
  - Natural consonant class acoustic similarity (plosives, fricatives, nasals, liquids) for expressive near / slant rhyme scoring.
  - Suffix and spelling fallback heuristics when a word or compound is missing from the dictionary, providing full offline rhyme suggestions.
- **Rhyme Assistant Bottom Sheet & UI Integration**:
  - Direct search bar with real-time rhyme lookup.
  - Integrated with lyric editor: extracts selected text or word under cursor with one tap on the top bar or editing ribbon bar.
  - Syllable count filter chips and category tabs: All Ranked, Perfect Rhymes, Near Rhymes.
  - One-tap "Insert at Cursor" and "Copy to Clipboard" actions.
- **Unit Testing**:
  - Built comprehensive test suite in `RhymeEngineTest.kt` validating normalization, contractions, phonetic indexing, multi-syllable rhyme keys, coda slant similarity, and fallback logic.

---

## [0.5.0-alpha] - 2026-09-16
### Phase 4: Local Audio Import & Real Waveform Engine
- **Storage Access Framework (SAF) Audio Import**:
  - Implemented secure SAF document picker supporting MP3, WAV, M4A/AAC, OGG, and FLAC audio files.
  - Internal sandbox file copying and orphan file cleanup via `AudioFileManager`.
  - Beat track database persistence and linking to songs via `AudioRepository`.
- **Playback & Interruption Engine**:
  - Built `BeatPlayer` handling play/pause, seek, volume, loop, and mute.
  - Audio focus management (`AudioManager.OnAudioFocusChangeListener`) with ducking and transient loss handling.
  - Headphone unplugged (`ACTION_AUDIO_BECOMING_NOISY`) safety pause.
  - Real-time output routing detection (Earpiece, Speaker, Bluetooth, Wired Headphones).
  - Background playback: Beat continues playing uninterrupted while songwriting in the lyric editor.
- **Real Waveform Generation**:
  - Implemented `WaveformAnalyzer` utilizing `MediaExtractor` and `MediaCodec` for true PCM amplitude extraction.
  - Peak amplitude downsampling to 100 points with two-tier caching (in-memory LruCache and JSON disk cache).
  - Interactive scrubbing canvas with tap-to-seek support.
- **Testing**:
  - Comprehensive unit test suite in `BeatAudioImportTest.kt`.

---

## [0.4.0-alpha] - 2026-09-15
### Phase 3: Complete Lyric Editor
- **Native Multiline Lyric Editor**:
  - Implemented studio-grade multiline text editing powered by `BasicTextField` with custom cursor brush and reactive `TextFieldValue` state.
  - Seamless cursor positioning, drag selection, and word selection.
  - Prevented keyboard obstruction with `Modifier.imePadding()` and responsive scroll-to-cursor handling.
- **Editing Actions & History**:
  - Full bidirectional Undo and Redo stacks with up to 50 historical states and automatic snapshot throttling.
  - Quick action editing ribbon with dedicated Copy, Cut, Paste, Select All, and Clear Selection triggers.
  - Non-destructive "Clear Document" workflow guarded by a confirmation dialog (`ClearLyricsDialog`) to prevent accidental deletion.
- **Search & Outline Navigation**:
  - In-lyric search engine with live query matching, total occurrences count, current match counter, and Next/Previous match cycling with cursor selection.
  - Song structure outline sheet with automated detection of standard section headers and one-tap jump-to-section cursor repositioning.
- **Songwriting Metrics & Sections**:
  - Real-time live metrics displaying total word count, character count, and line count.
  - One-tap section chip injector for standard songwriting markers: `[Intro]`, `[Verse]`, `[Pre-Chorus]`, `[Chorus]`, `[Post-Chorus]`, `[Bridge]`, `[Hook]`, and `[Outro]`.
- **Typography & Display Preferences**:
  - In-editor formatting bottom sheet with slider controls for font size (12sp to 32sp) and line spacing multiplier (1.0x to 2.2x).
  - Font family selector supporting Sans-serif, Serif, and Monospace typefaces.
  - "Keep Screen Awake" mode toggling Android window `FLAG_KEEP_SCREEN_ON` to prevent device screen timeout during live performance and studio writing sessions.
- **Persistence & State Safety**:
  - 600ms debounced autosave engine targeting Room database with visual state chips (`Saved`, `Saving...`, `Unsaved`).
  - Guaranteed lifecycle-aware saving on backgrounding/leaving via Android `LifecycleEventObserver` on `ON_PAUSE` / `ON_STOP` and Compose `BackHandler`.
  - Process recreation and app restart resilience tested and verified.
- **Testing & Verification**:
  - Implemented `LyricEditorTest` in Robolectric test suite validating metrics calculation, undo/redo, text selection/cut/paste, section outlines, in-lyric search cycling, autosave, instant save, typography updates, and process restart recreation.
  - All unit tests passing (`gradle testDebugUnitTest`).
  - Full app build passing (`compile_applet`).

---

## [0.3.0-alpha] - 2026-09-14
### Phase 2: Full Songs & Albums Implementation
- **Songs Feature Suite**:
  - **Creation**: Support creating songs with title, optional album assignment, and initial lyrics via the main action sheet or dialogs.
  - **Renaming**: Interactive rename dialog (`RenameSongDialog`) with instant Room database updates.
  - **Duplication**: Complete song cloning (`duplicateSong`) generating "[Title] (Copy)" with a fresh UUID, duplicating lyrics, key, and tempo settings.
  - **Deletion**: Safe deletion workflow with `DeleteConfirmDialog` warning of associated audio stems and take removals.
  - **Favorites & Archive**: Fast one-tap toggling with reactive Flow updates and designated filter chips (All, Favorites, Archive).
  - **Debounced Search**: 300ms debounced search query over song titles and lyrics backed by Room Flow queries.
  - **Sorting**: Multi-mode sorting support: Recently Modified, Title A-Z, Title Z-A, and Date Created.
  - **Visual Cards**: Designed `SongItemCard` featuring dynamic lyric previews, formatted last modified dates, and real-time audio stem / recording take badges.
  - **Context Menus**: Comprehensive 3-dots actions menu for every song project with destructive actions in high-contrast studio red.
- **Albums Feature Suite**:
  - **Creation & Editing**: Create and edit albums with titles, notes/descriptions, and customizable sleeve artwork style presets.
  - **Artwork Presets**: Implemented `AlbumArtworkThumbnail` and `AlbumCoverPresetPicker` with high-contrast gradient sleeves and vinyl groove motifs.
  - **Deletion Safety Guarantee**: Strictly verified that deleting an album **never** deletes its songs; songs are safely unlinked and remain standalone in the user's project library.
  - **Album Detail View**: Interactive album screen featuring header banners, track counts, tracklists, and direct song removal actions.
  - **Song Assignment**: Built `MoveSongToAlbumDialog` and `AddSongsToAlbumDialog` for adding, moving, and removing songs to/from collections.
- **Verification & Testing**:
  - Enhanced `DatabaseRepositoryTest` with Robolectric unit tests validating duplicate, rename, search, album song linking, and unlinking on album deletion.
  - All unit tests passing (`gradle :app:testDebugUnitTest`).
  - Full app compilation verified with `compile_applet`.

---

## [0.2.0-alpha] - 2026-09-14
### Foundation, Database Layer & Responsive UI
- **Database Architecture (Room)**:
  - Created `SongEntity`, `AlbumEntity`, `AudioTrackEntity`, and `RecordingEntity` with foreign keys, cascading deletes, and database indexes.
  - Implemented `SongDao`, `AlbumDao`, `AudioTrackDao`, and `RecordingDao` with reactive `Flow` queries and suspend write operations.
  - Built `SongDatabase` Room database with singleton instance provider.
  - Created clean domain models (`Song`, `Album`, `AudioTrack`, `Recording`, `TrackType`) and bidirectional entity-to-domain mappers.
  - Built `SongRepository`, `AlbumRepository`, and `AudioRepository`.
  - Added `DefaultAppContainer` for dependency provision.
  - Implemented comprehensive `DatabaseRepositoryTest` with Robolectric in-memory SQLite verifying all CRUD operations, foreign key cascades, and reactive flows.
- **Theme & Design System**:
  - Implemented Dark Studio Console aesthetic with custom dark, light, and system themes (`Theme.kt`, `Color.kt`, `Type.kt`).
  - Created custom adaptive vector launcher icon (`ic_launcher_background.xml` and `ic_launcher_foreground.xml`) featuring studio waveform and quill pen motif.
- **Responsive Navigation & Screens**:
  - Built responsive `MainScaffold` with adaptive breakpoint: bottom navigation bar for phones, navigation rail for tablets/landscape.
  - Integrated `Navigation Compose` with four primary tabs: **Songs**, **Albums**, **Recordings**, and **Settings**.
  - Built the main `+` action button with `NewItemBottomSheet` allowing instant creation of Songs, Albums, or Quick Voice Ideas directly in Room.
  - Implemented all 4 placeholder screens with reactive Flow collection from Room.
- **Verification**:
  - Compilation verified with `compile_applet`.
  - All JVM unit tests passing cleanly with Robolectric (`gradle :app:testDebugUnitTest`).

---

## [0.1.0-alpha] - 2026-09-14

### Initial Architecture & Environment Specifications (Phase 1)
- **Environment Inspection**:
  - Validated Android toolchains: Android Gradle Plugin 9.1.1, Kotlin 2.2.10, Compose Compiler 2.2.10, compileSdk 36, minSdk 24, targetSdk 36.
  - Inspected existing dependencies including Room 2.7.0, Coroutines 1.10.2, Compose BOM 2024.09.00, Roborazzi 1.59.0, and Robolectric 4.16.1.
- **Platform & Metadata Configuration**:
  - Configured platform `metadata.json` with app title "Songwriter" and feature description.
  - Set unique project `applicationId = "com.aistudio.songwriter.kxrqlv"` in `app/build.gradle.kts`.
  - Set `rootProject.name = "Songwriter"` in `settings.gradle.kts`.
  - Updated `res/values/strings.xml` with `app_name` = "Songwriter".
- **Architecture Blueprints Created**:
  - `PROJECT_PLAN.md`: Complete 8-phase implementation roadmap and functional scope.
  - `ARCHITECTURE.md`: Clean Architecture + MVVM + UDF structure, directory organization, and threading model.
  - `DATA_MODEL.md`: Room schema for `songs`, `albums`, `audio_tracks`, `recordings`, data safety rules, and `.songproject` specification.
  - `AUDIO_ARCHITECTURE.md`: Low-latency AudioRecord pipeline, earpiece communication routing, multi-stem PCM summing bus, soft limiter, and waveform caching.
  - `UI_DESIGN.md`: Dark Studio Console visual identity, color tokens, typography scales, and responsive layout specifications.
  - `DEVELOPMENT_RULES.md`: Mandatory development protocols, post-phase compile/test gates, and safety constraints.
  - `TODO.md`: Comprehensive phase-by-phase task tracking.
  - `CHANGELOG.md`: Established semantic version history.
- **Build Verification**:
  - Successfully verified baseline project compilation using `compile_applet`.
