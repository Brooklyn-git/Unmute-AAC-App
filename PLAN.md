# Unmute — AAC App Plan

## Vision

A fully **offline**, general-purpose AAC (Augmentative and Alternative Communication) app for Android.
Symbol-based communication boards with category navigation, a sentence-building bar that speaks via
text-to-speech, a spelling mode with word prediction, and caregiver customization (edit boards, add
own photos). Targets both children and adults, with configurable layouts.

## App name

**Unmute**

## Target users

- Both / general purpose (children with speech delay, adults with aphasia/stroke/ALS).
- Configurable layouts adapt to the user (see Grid profiles).

## Core features (v1)

- Symbol grid boards with category navigation
- Sentence building bar (tap cards → sentence → speak)
- Text input + word prediction (offline, language-aware)
- Customizable boards (caregiver can add/edit/reorder cards, use symbols or own photos)
- Text-to-speech via Android `TextToSpeech` API (offline)
- Audio output routing: **speaker by default**, user-selectable output

## Tech stack

- Kotlin 2.1.10, Jetpack Compose (Material 3)
- AGP 8.12.2, Gradle wrapper 9.5.1 (proven toolchain on this machine, works with JDK 26)
- Room — persistence (boards/categories/cards)
- DataStore — settings
- Coil + coil-svg — image loading (bundled Mulberry SVGs + custom photos), offline
- Android TextToSpeech API — on-device voices, offline
- Custom `AudioTrack` playback for forced output routing
- Photo Picker (`GetContent`) — custom pictures with no storage permissions
- Tests: JUnit + kotlinx-coroutines-test
- MVVM + single `AppContainer` for DI (no Hilt — keep it simple)

## Data model

- `Board (name, order)` → `Category (name_en/es, color, order)` → `Card (label_en/es, phrase_en/es, symbolPath | photoUri, color, order)`
- `GridProfile (name, columns, isPreset)` — "Big", "Small", plus user "Custom" profiles
- `Settings` (DataStore): language, active grid profile, audio output, TTS voice/rate/pitch, autospeak
- First launch seeds a default board with core categories (Greetings, People, Food & Drink, Feelings, Actions, Places, Things) using Mulberry symbols with EN + ES labels.

## Screens / UX

1. **Board screen** — sentence bar on top, category tabs, card grid. Tap card → appended to sentence (+ optional per-word speech); Speak / Clear / Backspace buttons.
2. **Text mode** — keyboard + prediction strip + Speak.
3. **Edit mode (caregiver)** — add/edit/delete/reorder cards & categories; pick a Mulberry symbol or own photo; edit bilingual labels; optional PIN gate.
4. **Settings** — language, grid profiles, TTS voice/rate/pitch, audio output (+ test sound), autospeak.

## Grid profiles

- **Global**: one active profile applies to all boards (muscle-memory friendly for AAC users).
- **Two locked presets** (name and columns cannot be edited):
  - **Big** (default) — few columns, large buttons
  - **Small** — dense grid
- **Custom profiles:** pressing "Add custom" creates a **copy of the currently active profile's layout**, which the user can then modify (change columns, rename; default name "Custom"). The original profile is never modified. Same behavior regardless of which profile was active (Big, Small, or another custom).
- Custom profiles can be edited, renamed, and deleted. Presets cannot be modified or deleted.

## Multi-language (English + Spanish)

- App UI strings via resource qualifiers (`values/` + `values-es/`).
- Card data is language-aware: `label_en / label_es`, `phrase_en / phrase_es`; displays the active language, falls back to English.
- Seed board ships with Spanish + English labels for core symbols (translation map built during implementation).
- Language selectable in Settings (default: follow system).

## Audio output routing

Android's system APIs that force routing (`setCommunicationDevice`, `setPreferredDeviceForStrategy`)
require the signature-level `MODIFY_AUDIO_ROUTING` permission — not available to normal apps.
The built-in TTS also plays through its own AudioTrack that the app cannot control.

**Implementation:**
1. `TextToSpeech.synthesizeToFile()` → render speech to a PCM file (built-in Google TTS supports this).
2. Play it through our own `AudioTrack` with `setPreferredDevice()` to force the output.
3. Output options in Settings: **Speaker / Wired headset / Bluetooth / Automatic** (system default).
   Devices enumerated via `AudioManager.getDevices()`.

**Goal scenario:** AAC user keeps headphones on (e.g., listening to music) while their speech goes
through the speaker so they can communicate without disconnecting headphones.

**Caveats:**
- Some OEMs (known: some Xiaomi/Redmi) ignore `setPreferredDevice` → add a **"Test sound"** button in Settings.
- Slightly higher latency from synthesize-to-file (~100–300ms) — acceptable; can pre-synthesize next utterance in background.
- Fallback: if a TTS engine doesn't support file synthesis, fall back to normal `speak()` (system routing applies).

## Word prediction (offline)

Prefix match over app vocabulary (card labels + phrases + common words), ranked by recency/frequency.
Language-aware (EN/ES).

## Mulberry symbols

- One-time setup: download the [Mulberry release](https://github.com/mulberrysymbols/mulberry-symbols/releases/latest)
  (~3,100 SVGs + `symbol-info.csv`), bundle a curated core subset in `assets/symbols/`.
- CC-BY-SA attribution in an About screen.
- Fallback if coil-svg misrenders any symbol: convert those to PNG via a script.

## Milestones

1. Project scaffold "Unmute" → build passing (build after every change, per AGENTS.md)
2. Data layer: Room schema (bilingual cards, grid profiles) + seeded default board
3. Board UI + grid profiles (Big/Small/Custom)
4. TTS + custom audio routing (speaker-by-default, output selection, test sound)
5. Sentence bar
6. Text input + prediction (language-aware)
7. Customization (edit mode, photo picker, bilingual label editing)
8. Settings + accessibility polish
9. Unit tests + final `assembleDebug`

## Known risks / notes

- JDK 26: proven OK with AGP 8.12.2 + Gradle 9.5.1 on this machine.
- Needs internet once to download Compose/Room/Coil deps and the Mulberry zip.
- No emulator/device in PATH currently — verification via builds + unit tests.
- TTS voices depend on device engines; expose voice selection.
