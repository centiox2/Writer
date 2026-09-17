# Songwriter - Data Model & Persistence Specification

This document specifies the SQLite/Room database schema, relational integrity, data safety policies, and serialization formats for the Songwriter application.

---

## 1. Relational Entities (Room Schema)

### 1.1 `songs` Table (`SongEntity`)
Represents the central project unit.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY | UUID string identifier |
| `title` | `TEXT` | NOT NULL | Project title |
| `lyrics` | `TEXT` | NOT NULL, DEFAULT `""` | Complete lyric text content |
| `albumId` | `TEXT` | NULLABLE, FK (`albums.id` ON DELETE SET NULL) | Associated album |
| `artworkUri` | `TEXT` | NULLABLE | Path or URI to project cover art |
| `createdAt` | `INTEGER` | NOT NULL | Milliseconds since Unix epoch |
| `updatedAt` | `INTEGER` | NOT NULL | Milliseconds since Unix epoch |
| `favorite` | `INTEGER` | NOT NULL, DEFAULT `0` | Boolean (0 or 1) |
| `archived` | `INTEGER` | NOT NULL, DEFAULT `0` | Boolean (0 or 1) |
| `bpm` | `INTEGER` | NULLABLE | Beats per minute metadata |
| `keySignature` | `TEXT` | NULLABLE | Musical key (e.g., "C#m", "G Major") |

### 1.2 `albums` Table (`AlbumEntity`)
Represents collections/compilations of songs.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY | UUID string identifier |
| `name` | `TEXT` | NOT NULL | Album name |
| `description` | `TEXT` | NOT NULL, DEFAULT `""` | Description or liner notes |
| `artworkUri` | `TEXT` | NULLABLE | Path or URI to album cover art |
| `createdAt` | `INTEGER` | NOT NULL | Milliseconds since Unix epoch |
| `updatedAt` | `INTEGER` | NOT NULL | Milliseconds since Unix epoch |

### 1.3 `audio_tracks` Table (`AudioTrackEntity`)
Represents independent audio stems and tracks attached to a song project (e.g. beat, instrumental, guide vocal).

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY | UUID string identifier |
| `songId` | `TEXT` | NOT NULL, FK (`songs.id` ON DELETE CASCADE) | Parent song project |
| `name` | `TEXT` | NOT NULL | Track name (e.g. "Main Beat", "Guitar Guide") |
| `uri` | `TEXT` | NOT NULL | Relative internal file path or external URI |
| `type` | `TEXT` | NOT NULL | `BEAT`, `VOCAL`, `RECORDING`, `GUIDE`, `INSTRUMENTAL` |
| `durationMs` | `INTEGER` | NOT NULL, DEFAULT `0` | Track duration in milliseconds |
| `volume` | `REAL` | NOT NULL, DEFAULT `1.0` | Linear gain multiplier (0.0 to 1.5) |
| `muted` | `INTEGER` | NOT NULL, DEFAULT `0` | Boolean (0 = active, 1 = muted) |
| `solo` | `INTEGER` | NOT NULL, DEFAULT `0` | Boolean (0 = normal, 1 = solo) |
| `pan` | `REAL` | NOT NULL, DEFAULT `0.0` | Stereo pan (-1.0 left, 0.0 center, 1.0 right) |
| `offsetMs` | `INTEGER` | NOT NULL, DEFAULT `0` | Time shift on project timeline |
| `createdAt` | `INTEGER` | NOT NULL | Milliseconds since Unix epoch |

### 1.4 `recordings` Table (`RecordingEntity`)
Represents distinct vocal takes recorded within the app.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `TEXT` | PRIMARY KEY | UUID string identifier |
| `songId` | `TEXT` | NOT NULL, FK (`songs.id` ON DELETE CASCADE) | Parent song project |
| `name` | `TEXT` | NOT NULL | Take label (e.g., "Take 1", "Chorus Lead - Take 2") |
| `uri` | `TEXT` | NOT NULL | Relative internal file path to `.wav` file |
| `durationMs` | `INTEGER` | NOT NULL, DEFAULT `0` | Recording duration in milliseconds |
| `createdAt` | `INTEGER` | NOT NULL | Milliseconds since Unix epoch |
| `isFavorite` | `INTEGER` | NOT NULL, DEFAULT `0` | Highlighted take flag |
| `waveformPoints`| `TEXT` | NULLABLE | Comma-separated normalized peak values cache |

---

## 2. Relational Integrity & Cascade Rules

- **Foreign Keys**: Room SQLite enforces foreign keys (`foreignKeys = [...]`).
- **Cascade Deletion**:
  - Deleting a `Song` automatically purges associated `AudioTrack` and `Recording` database records.
  - The repository coordinates with `AudioFileManager` to asynchronously delete the physical audio files associated with purged records, preventing storage leaks.
- **Album De-association**:
  - Deleting an `Album` uses `onDelete = ForeignKey.SET_NULL`, meaning songs inside the album are preserved and reverted to unassigned status without losing lyrics or audio.

---

## 3. Data Safety & Autosave Policies

Songwriter implements military-grade data protection to safeguard lyrics and recordings:

1. **Debounced Text Autosave**:
   - As the user types in the lyrics editor, changes are held in memory.
   - An autosave job triggers after 800ms of user typing inactivity.
   - Saves are executed in an asynchronous coroutine write to SQLite.
2. **Lifecycle-Triggered Flush**:
   - Immediate synchronous/blocking write executed on `onPause()`, `onStop()`, or when navigating away from `SongStudioScreen`.
3. **Crash-Safe Persistence**:
   - SQLite WAL (Write-Ahead Logging) mode is activated.
   - Atomic file operations are used for file writes (write to `.tmp` file, then rename).
4. **Missing-File Detection**:
   - When loading audio tracks or takes, file existence is verified.
   - Missing or inaccessible files are flagged in the UI with a "Missing File" badge rather than causing a crash, allowing the user to re-link or delete the track entry.
5. **No Accidental Deletion**:
   - Destructive actions (deleting songs, deleting takes, resetting mixer) require explicit confirmation dialogs with clear explanations of consequences.

---

## 4. Project Archive Format (`.songproject`)

To allow exporting and sharing complete song projects without cloud reliance, the app defines a portable ZIP-based archive format:

```
[project-slug].songproject (ZIP Container)
├── project.json          # Complete JSON dump of song metadata and mixer settings
├── lyrics.txt            # Plaintext lyrics file for universal readability
├── metadata.json         # Container version, generator ID, export timestamp
├── audio/
│   ├── beat_01.wav       # Raw or imported audio stems
│   ├── take_01.wav       # Recorded vocal takes
│   └── take_02.wav
└── artwork/
    └── cover.jpg         # Cover artwork (if attached)
```

### Import Policies:
- **Merge Mode (Default)**: Creates a new song project with a fresh UUID, preserving existing local projects even if names collide.
- **Replace Mode**: Overwrites the existing project only if explicitly selected by the user and confirmed with a warning dialog.
