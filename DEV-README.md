# Unmute — Developer README

Documentation for developers working on **Unmute**, an offline bilingual (EN/ES) AAC app
for Android. For an end-user/caregiver overview, see `README.md`.

---

## Tech stack

| Layer      | Choice                                                          |
|------------|-----------------------------------------------------------------|
| Language   | Kotlin 2.1.x                                                    |
| UI         | Jetpack Compose (Material 3)                                    |
| Persistence| Room (boards / sections / cards / grid profiles)                |
| Settings   | Jetpack DataStore Preferences                                    |
| Images     | Coil + coil-svg (bundled SVGs + gallery photos), fully offline  |
| Speech     | Android `TextToSpeech` + custom `AudioTrack` for output routing |
| Architecture | MVVM, single `AppContainer` for DI (no Hilt)                    |
| Testing    | JUnit + kotlinx-coroutines-test                                 |

---

## Project structure

```
app/src/main/java/com/unmute/app/
├── data/            # Repository, seeding, DefaultSeed content
│   └── local/       # Room entities, DAOs, database + migrations
├── domain/model/    # ImageType, languages, EmojiLibrary, localization
├── tts/             # Text-to-speech + audio output routing
├── ui/              # Compose screens & components
│   ├── board/       # Board screen, card grid, reorderable tabs, dialogs
│   └── settings/    # Settings screen
└── util/            # PhotoStore, etc.
```

### Data model

- `Board (name, order)` → `Category (name_en/es, color, order, isPreset)` → `Card (label_en/es,
  phrase_en/es, symbol | emoji | photo, color, order)`
- `GridProfile (name, columns, isPreset)` — "Big", "Small", plus user "Custom" profiles
- `Settings` (DataStore): language, active grid profile, audio output, TTS rate/pitch, autospeak
- Default sections are marked `isPreset = true` and cannot be deleted.

---

## Building

```bash
./gradlew assembleDebug            # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest        # runs the unit tests
```

- Debug builds need no signing config — install the APK directly (minSdk 26, targetSdk 36).
- The project uses a version catalog (`gradle/libs.versions.toml`).
- Room schemas are exported to `app/schemas/` and committed; bump the database version and add a
  migration whenever the schema changes.

---

## Conventions

- Code should be idiomatic, readable, and follow KISS / DRY.
- Guard statements over nesting; small single-purpose functions.
- Prefer free/open dependencies over proprietary ones.
