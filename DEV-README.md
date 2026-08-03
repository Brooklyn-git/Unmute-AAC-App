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
| Backup     | kotlinx-serialization (JSON manifest) + `java.util.zip` (`.unmute` files) |
| Architecture | MVVM, single `AppContainer` for DI (no Hilt)                    |
| Testing    | JUnit + kotlinx-coroutines-test                                 |

---

## Project structure

```
app/src/main/java/com/unmute/app/
├── data/            # Repository, seeding, DefaultSeed content
│   ├── backup/      # BackupManager + .unmute (zip) manifest models
│   └── local/       # Room entities, DAOs, database + migrations
├── domain/model/    # ImageType, languages, EmojiLibrary, localization
├── tts/             # Text-to-speech + audio output routing
├── ui/              # Compose screens & components
│   ├── board/       # Board screen, card grid, reorderable tabs, dialogs
│   ├── components/  # Reusable widgets (Stepper, etc.)
│   └── settings/    # Settings screen
└── util/            # PhotoStore, etc.
```

### Data model

- `Board (name, order)` → `Category (name_en/es, color, order, isPreset)` → `Card (label_en/es,
  phrase_en/es, symbol | emoji | photo, color, order)`
- `GridProfile (name, columns, isPreset)` — "Big", "Small", plus user "Custom" profiles
- `Settings` (DataStore): language, active grid profile, audio output, TTS engine/rate/pitch,
  autospeak, speak-on-add, card font size, secure mode (tap count + reset window)
- Default sections are marked `isPreset = true` and cannot be deleted.
- The app currently operates on a **single board**; the `boards` table is schema-ready for future
  multi-board support.

### Backup & restore

- From Settings → **Data**, the user can export a full backup or import one (system document picker).
- A backup is a ZIP (default name `unmute-backup-YYYYMMDD-HHMMSS.unmute`) containing:
  - `backup.json` — versioned manifest with board, categories, cards, grid profiles, and settings.
  - `photos/` — the card photo files (for `ImageType.PHOTO` cards).
- Import validates the manifest (version + referential integrity) and applies everything in a single
  Room transaction via `BoardRepository.replaceAll()`; settings are restored afterwards.
- Bump `BACKUP_VERSION` in `BackupManager` whenever the backup format changes.

---

## Building

```bash
./gradlew assembleDebug            # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest        # runs the unit tests
```

- **JDK:** AGP 8.x cannot run on JDK 26. Use a JDK 21 for the build and point Gradle at it
  with `JAVA_HOME` (e.g. `export JAVA_HOME=/path/to/jdk-21`); do **not** hardcode a machine path
  in `gradle.properties` (F-Droid build servers set their own `JAVA_HOME`).
- Debug builds need no signing config — install the APK directly (minSdk 26, targetSdk 36).
- The project uses a version catalog (`gradle/libs.versions.toml`).
- Room schemas are exported to `app/schemas/` and committed; bump the database version and add a
  migration whenever the schema changes.

Current unit tests (`app/src/test`): TTS `WavParser`, localization/fallback logic, and
backup serialization round-trips.

---

## Conventions

- Code should be idiomatic, readable, and follow KISS / DRY.
- Guard statements over nesting; small single-purpose functions.
- Prefer free/open dependencies over proprietary ones.

---

## License

- **Source code:** GNU General Public License v3 (`LICENSE`).
- **Bundled symbols:** Mulberry Symbols, CC BY-SA 4.0 (see `NOTICE`).
  The symbols live in `app/src/main/assets/symbols/` and must stay under
  CC BY-SA with attribution.
