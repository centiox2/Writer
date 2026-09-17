# Songwriter - Audio Engine Architecture

This document details the low-level audio engineering specifications, real-time recording pipeline, multitrack software mixing engine, earpiece communication routing, and waveform processing.

---

## 1. Core Audio Subsystems

Songwriter uses direct Android native audio APIs (`AudioRecord`, `AudioTrack`, `MediaCodec`, `MediaExtractor`, `AudioManager`) to maintain sub-frame accuracy, low overhead, and complete offline capability.

```
┌────────────────────────────────────────────────────────────────────────┐
│                              AudioEngine                               │
│  - Playback coordinator                                                │
│  - Recording lifecycle manager                                         │
│  - Timeline clock & synchronized transport (play/pause/seek/loop)      │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
         ┌─────────▼──────────┐            ┌─────────▼──────────┐
         │   AudioRecorder    │            │     AudioMixer     │
         │ - AudioRecord PCM  │            │ - Multi-stem sync  │
         │ - Live VU metering │            │ - Volume/mute/solo │
         │ - WAV file writer  │            │ - Soft-clip limiter│
         └─────────┬──────────┘            │ - PCM summing bus  │
                   │                       └─────────┬──────────┘
                   │                                 │
         ┌─────────▼──────────┐            ┌─────────▼──────────┐
         │CommunicationRouting│            │   AudioTrack Out   │
         │ - Earpiece toggle  │            │   or WAV Export    │
         │ - Bluetooth/Headset│            └────────────────────┘
         └────────────────────┘
```

---

## 2. Low-Latency Recording Architecture (`AudioRecorder`)

### 2.1 Hardware Configuration
- **API**: `android.media.AudioRecord`
- **Audio Source**: `MediaRecorder.AudioSource.MIC` (or `VOICE_COMMUNICATION` when earpiece mode is enabled)
- **Sample Rate**: 44,100 Hz (standard studio sample rate with universal Android hardware support)
- **Channel Config**: `AudioFormat.CHANNEL_IN_MONO` (standard for vocal tracking)
- **Audio Format**: `AudioFormat.ENCODING_PCM_16BIT`
- **Buffer Size**: Computed dynamically via `AudioRecord.getMinBufferSize()`, multiplied by 2 to prevent hardware underflow during garbage collection or background activities.

### 2.2 Threading & File Writing
- Recording executes in a dedicated high-priority coroutine thread (`Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)`).
- As bytes are read from `AudioRecord`, they are:
  1. Appended to a streaming `FileOutputStream` (raw PCM).
  2. Analyzed in 1024-sample blocks to compute Root-Mean-Square (RMS) amplitude and peak decibels.
  3. Dispatched to `_recordingState` via a high-frequency Flow to drive the live UI visualizer at 60fps.
- Upon calling `stop()`, a canonical 44-byte RIFF/WAV header is prepended and updated with exact data chunk sizes.

---

## 3. Communication Mode & Bleed Reduction (`CommunicationModeHelper`)

### 3.1 Problem Statement
When recording vocals over a backing beat on a phone without headphones, speaker output feeds directly into the microphone, severely degrading the vocal track.

### 3.2 Engineered Solution
By engaging Android's Voice Communication audio routing, the backing beat is directed through the device's **built-in earpiece** (the top speaker used for phone calls) while the user holds the phone like a telephone or microphone:
1. When **Earpiece Mode** is toggled on:
   - `audioManager.mode = AudioManager.MODE_IN_COMMUNICATION`
   - For Android 12+ (API 31+): Inspect `audioManager.availableCommunicationDevices` for `AudioDeviceInfo.TYPE_BUILTIN_EARPIECE`, and invoke `audioManager.setCommunicationDevice(earpieceDevice)`.
   - For Android 7.0–11 (API 24–30): Invoke `audioManager.isSpeakerphoneOn = false`.
2. When a wired headset or Bluetooth headset (A2DP/SCO) is detected, the app automatically respects the connected accessory, displaying an indicator: "Headphones Connected — Earpiece routing bypassed".
3. When recording completes, audio mode is cleanly restored to `AudioManager.MODE_NORMAL`.

---

## 4. Multi-Track Mixer & PCM Summing Engine (`AudioMixer`)

The app avoids multiple disconnected `MediaPlayer` instances, which suffer from independent clock drift, seek jitter, and volume phase issues.

### 4.1 Synchronized Pipeline
```
[Stem 1: Beat]      --> MediaExtractor + MediaCodec --> Resample 44.1kHz Float PCM --┐
                                                                                     │
[Stem 2: Vocal]     --> Decode to 44.1kHz Float PCM ---------------------------------┼--> Sync by OffsetMs
                                                                                     │
[Stem 3: Harmonies] --> Decode to 44.1kHz Float PCM ---------------------------------┘
                                                                                     │
                                                      ┌──────────────────────────────┘
                                                      │
                                           ┌──────────▼──────────┐
                                           │  Solo / Mute Matrix │
                                           └──────────┬──────────┘
                                                      │
                                           ┌──────────▼──────────┐
                                           │  Gain & Pan Scaler  │
                                           └──────────┬──────────┘
                                                      │
                                           ┌──────────▼──────────┐
                                           │  Summing Accumulator│
                                           └──────────┬──────────┘
                                                      │
                                           ┌──────────▼──────────┐
                                           │ Soft-Clip Limiter   │
                                           └──────────┬──────────┘
                                                      │
                                    ┌─────────────────┴─────────────────┐
                                    ▼                                   ▼
                         Real-Time AudioTrack Output              WAV File Mixdown
```

### 4.2 Gain and Solo/Mute Rules
Let $N$ be the number of active tracks:
- If $\exists \text{ track } i \text{ with } \text{solo}_i = \text{true}$:
  - Any track with $\text{solo} = \text{false}$ is silenced ($G_k = 0$).
  - Soloed tracks are scaled by their volume: $G_i = \text{volume}_i$ (unless $\text{muted}_i = \text{true}$, in which case $G_i = 0$).
- If no track is soloed:
  - For each track $k$: $G_k = 0$ if $\text{muted}_k = \text{true}$, else $G_k = \text{volume}_k$.

### 4.3 Clipping Prevention (Soft Limiter)
Summing multiple digital tracks can exceed normalized $[-1.0, 1.0]$ bounds. To prevent harsh digital square-wave distortion, the summing bus applies an analytical soft-clipper before 16-bit PCM quantization:
$$f(x) = \begin{cases} 
x & \text{for } |x| \le 0.8 \\
\text{sgn}(x) \cdot \left(0.8 + 0.2 \cdot \tanh\left(\frac{|x| - 0.8}{0.2}\right)\right) & \text{for } |x| > 0.8 
\end{cases}$$
This provides warm, analog-style saturation on loud peaks while preserving pristine dynamics on normal levels.

---

## 5. Waveform Analyzer & Caching (`WaveformAnalyzer`)

1. **Extraction**: Audio files are read via `MediaExtractor` / `MediaCodec` in background coroutines.
2. **Downsampling**: Amplitude samples are partitioned into 100–200 discrete time buckets. For each bucket, peak and RMS values are calculated.
3. **Normalization**: Peak values are normalized to a $[0.0, 1.0]$ floating-point range.
4. **Two-Level Caching**:
   - **Level 1 (Memory)**: Fast in-memory `LruCache<String, FloatArray>` keyed by file path and last-modified timestamp.
   - **Level 2 (Persistent Room/File)**: Serialized waveform string stored directly in `recordings` and `audio_tracks` tables. Re-analyzing on subsequent app sessions is instantaneous (0ms).
