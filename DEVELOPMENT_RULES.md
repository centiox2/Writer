# Songwriter - Development & Engineering Rules

This document defines mandatory protocols, coding standards, safety conventions, and quality gates for developing the Songwriter application.

---

## 1. Phased Development Protocol

1. **Strict Phased Delivery**: Features must be designed, implemented, and verified in distinct phases according to `PROJECT_PLAN.md`.
2. **Mandatory Post-Phase Quality Gate**:
   After completing every phase:
   - **Step 1: Compile**: Execute `compile_applet` to verify compilation and resource packaging.
   - **Step 2: Test**: Run unit/Robolectric tests or verify component behaviors.
   - **Step 3: Fix**: Immediately resolve any compiler warnings, syntax issues, or broken contracts before progressing.
   - **Step 4: Document**: Update `TODO.md` (check off completed items) and `CHANGELOG.md` (record the new version release notes).
   - **Step 5: Zero-Defect Rule**: Never leave the codebase in an uncompilable or knowingly broken state at the end of any turn.

---

## 2. Audio & Hardware Safety Rules

1. **Hardware Lifecycle Safety**:
   - `AudioRecord` and `AudioTrack` hardware handles must always be wrapped in try/finally blocks and released promptly when recording or playback stops.
   - Never leak native audio buffers or background threads.
2. **Permission Guardrails**:
   - `android.permission.RECORD_AUDIO` must be declared in `AndroidManifest.xml` and requested at runtime using modern Jetpack Compose permission handling before initializing `AudioRecord`.
   - Clear explanatory UI must be shown if microphone permission is denied.
3. **Audio Routing Restoration**:
   - When using `AudioManager.MODE_IN_COMMUNICATION` for earpiece routing, always restore `audioManager.mode = AudioManager.MODE_NORMAL` when recording stops or when the app is paused/destroyed.
4. **Volume & Gain Boundaries**:
   - Software mixer gains must always be clamped to prevent negative values or catastrophic digital wrapping.
   - Summing bus must pass through soft-clip limiting before writing to standard 16-bit PCM.

---

## 3. Data Integrity & Persistence Mandates

1. **Zero Data Loss Guarantee**:
   - User lyrics are sacred. Never discard uncommitted edits.
   - Debounce lyric edits (800ms) to SQLite and immediately flush to disk on activity lifecycle pauses (`onPause`, `onStop`).
2. **Never Silently Delete User Data**:
   - Destructive operations (deleting a song, purging a recorded take, clearing an album) must trigger an explicit confirmation dialog.
3. **Foreign Key Integrity**:
   - Always enforce foreign keys with appropriate cascade rules (deleting a song cleans up its track references; deleting an album sets song albumId to null without deleting the song).
4. **Atomic File Storage**:
   - File exports and project backup ZIP archives must be written to a temporary file first and atomically renamed upon successful completion to prevent corrupt, half-written files.

---

## 4. UI/UX & Code Quality Standards

1. **Material Design 3 Compliance**:
   - Use official Material 3 components (`Scaffold`, `ModalBottomSheet`, `FilledTonalButton`, `FilterChip`, `Slider`, `Card`).
   - All interactive elements must strictly satisfy the 48dp minimum touch target.
2. **Compose TestTags**:
   - Add `Modifier.testTag("...")` with descriptive snake_case identifiers to key actionable components (`record_button`, `play_pause_button`, `lyrics_text_editor`, `rhyme_search_input`, `mixer_export_button`).
3. **Clean Architecture Separation**:
   - Composables must never execute raw database or audio driver queries directly; they must interact solely through ViewModels and Domain Models.
   - Database entities must be mapped to clean Domain Models before reaching the UI layer.
