# Songwriter - UI/UX Design System Specification

This document details the Material 3 Expressive visual design, color tokens, layout hierarchy, and component specifications for Songwriter.

---

## 1. Aesthetic Identity: "Dark Studio Console"

The interface adopts a professional, late-night music production aesthetic reminiscent of hardware audio workstations and modern DAWs:
- **Near-black background** minimizes eye strain during extended lyric-writing sessions.
- **Subtle gray elevated surfaces** clearly separate channel strips, meters, and text cards.
- **Expressive Blue & Mint Accents** guide attention to primary actions (record, play, solo, rhyme match).
- **Restrained Corner Radii (8dp - 16dp)** maintain a clean, precision instrument feel without bubbly or toy-like appearances.

---

## 2. Color System Tokens

```kotlin
// Primary Backgrounds & Canvas
val StudioBlack = Color(0xFF0F1013)          // Deep background
val StudioSurfaceDark = Color(0xFF16181D)    // Cards, panels, bottom sheets
val StudioSurfaceVariant = Color(0xFF20232A) // Inset wells, input bars, inactive chips
val StudioBorder = Color(0xFF2A2E38)         // Subtle hairline dividers

// Accent & Action Colors
val StudioBluePrimary = Color(0xFF4D88FF)   // Primary interactive elements, playhead
val StudioBlueGlow = Color(0x334D88FF)      // Waveform glow and active channel rings
val StudioMint = Color(0xFF38D9A9)          // Rhyme highlight, solo active state, success
val StudioRedRecord = Color(0xFFFF4D4D)     // Active recording indicator, record button
val StudioAmberMute = Color(0xFFFFB84D)     // Mute indicator, warnings

// Typography & Content
val StudioTextHigh = Color(0xFFF3F5F9)      // Headings, active lyric text
val StudioTextMedium = Color(0xFF9EA3B0)    // Secondary labels, timecodes, counts
val StudioTextLow = Color(0xFF5A606E)       // Placeholders, disabled icons
```

---

## 3. Typography & Hierarchy

- **Title Display**: Bold, sans-serif with slight tracking for song titles and album headers.
- **Lyric Body**: Crisp sans-serif (`LineHeight = 28.sp`, `FontSize = 18.sp`) with user-configurable scale (14sp to 24sp) and line spacing toggle to adapt to personal writing styles.
- **Technical & Timecodes**: Monospace (`FontFamily.Monospace`) for time counters (`02:45.12`), BPM counters, volume decibels, and syllable metrics.

---

## 4. Key Screen Layouts & Ergonomics

### 4.1 Library Screen (Projects & Albums)
- **Top Bar**: Search bar with debounced query filtering, Filter Chips (All, Favorites, Archived), Sort Menu (Recently Updated, Created Date, Title A–Z).
- **Tab Bar / View Switcher**: Songs view vs. Albums view.
- **Project Cards**: Displays song title, album badge, last modified date, lyric preview snippet, audio track count badge, and favorite star icon.
- **Floating Action Button (FAB)**: Primary "New Song" button with Material 3 Expressive motion.

### 4.2 Song Studio / Lyrics Editor Screen
- **Header Transport**: Compact playback bar containing title, play/pause, seek slider, and duration indicator.
- **Section Injector Bar**: Horizontally scrollable row of quick-insert chips: `[Intro]`, `[Verse]`, `[Chorus]`, `[Bridge]`, `[Hook]`, `[Outro]`.
- **Primary Writing Canvas**:
  - Genuine Android multiline text component (`BasicTextField` / `OutlinedTextField`) wrapped in `rememberScrollState`.
  - Supports system clipboard operations (cut, copy, paste), continuous undo/redo, text selection, and search-within-lyrics.
  - Floating footer pill displaying: Word Count | Character Count | Rhyme Lookup shortcut.
  - Screen wake-lock option (`FLAG_KEEP_SCREEN_ON`) toggled directly from editor settings.

### 4.3 Rhyme Finder Sheet
- Opens as an interactive Material 3 Modal Bottom Sheet or sidebar pane.
- Allows instant query lookup of the current word under cursor or user input.
- Displays results grouped by syllable count (1 Syllable, 2 Syllables, 3+ Syllables) with phonetic match type indicators (Perfect Rhyme, Slant Rhyme, Assonance).
- Tap any rhyme to copy or insert directly at the cursor position.

### 4.4 Recording Studio Sheet
- Live Audio Waveform visualizer displaying real-time audio amplitude pulses.
- Large circular Record / Pause / Stop button with pulse animation during recording.
- Earpiece Routing Toggle: Clearly states "Earpiece Mode Active (Backing audio played through top speaker to minimize mic bleed)".
- Takes List: Accordion list of all takes recorded for the current song. Each take has inline playback, waveform preview, favorite star, rename dialog, and "Add to Mixer" action.

### 4.5 Multitrack Mixer Console
- Horizontal stem channels displaying:
  - Track Title & Type Icon (Beat, Vocal Take, Guide).
  - Miniature full-track waveform.
  - Mute (`M`) and Solo (`S`) toggle chips with high-visibility color feedback.
  - Continuous logarithmic volume slider with percentage badge.
- Master Bus Strip: Master volume fader and one-tap "Export WAV Mixdown" button.

---

## 5. Responsive & Adaptive Design

- **Handheld Portrait (Compact)**: Single-column lyric editor with sticky bottom transport bar and slide-up modal sheets for recorder, rhyme finder, and mixer.
- **Tablets & Foldables (Expanded / Landscape)**:
  - Left pane (60% width): Lyrics editor with section tags and typography controls.
  - Right pane (40% width): Persistent multi-track mixer, live recording panel, or rhyme finder drawer side-by-side.
- **Touch Targets**: All buttons and sliders enforce a minimum touch target of 48dp × 48dp according to Material 3 accessibility guidelines.
