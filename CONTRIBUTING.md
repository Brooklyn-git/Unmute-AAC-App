# Contributing to Unmute

Thanks for wanting to help! Unmute is a fully offline AAC (Augmentative and
Alternative Communication) app. People who rely on AAC devices can be sensitive
to regressions, so we take care with every change. Please read this before
opening a PR or an issue.

---

## Code of conduct

Be kind and patient. Many contributors (and users) are here because AAC matters
to them or someone they care about. Harassment and demeaning comments have no
place here.

---

## Getting started

1. **Fork** the repository and clone your fork.
2. Open it in Android Studio (latest stable) or your preferred editor.
3. Build and run the app once so you're sure your setup works:

   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ```

   The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

4. Find something to work on. Open issues and bugs are a good place to start;
   tell us you're on it before starting large work so we don't collide.

More details about the codebase are in `DEV-README.md`.

---

## Making changes

- Keep commits **small, focused, and atomic** — each one should be easy to
  review and revert on its own.
- Write a clear commit message describing the *why* of the change, not just the
  *what*.
- Rebase your branch onto the latest `main` before opening a PR.
- Open a **draft PR** early for big changes, so reviewers can follow along.

## Style

Follow the project conventions in `AGENTS.md` and `DEV-README.md`:

- Idiomatic, readable code. **KISS** over cleverness, **DRY** over copy-paste.
- Guard statements over deep nesting; small functions that do one thing.
- Prefer free/open dependencies over proprietary ones.
- Reuse existing helpers instead of re-implementing them.
- No hardcoding values that might be needed elsewhere.
- Don't add code comments unless they genuinely clarify intent.

## Testing

- Run the app and verify your change works on a device or emulator.
- Write tests for as much of your change as possible.
- Follow **TDD**: write a failing test first, then make it pass.
- **Never delete existing tests.** A failing test means something is wrong with
  your change, not the test.
- Run the full unit test suite before submitting:

  ```bash
  ./gradlew testDebugUnitTest
  ```

## Building

- Run `./gradlew assembleDebug` after your changes to make sure everything
  compiles.
- Room: if you change the schema, bump the database version and add a
  migration. Exported schemas live in `app/schemas/` and are committed.
- Keep the app **fully offline** and free of tracking/analytics. That's a core
  promise of Unmute.

---

## Translating the app

Unmute is used by people who rely on AAC devices, so a good translation matters
as much as a good feature. A language lives in four places — you can do the
translation work alone and open a draft PR, and a maintainer (or you, if you
feel confident) can wire up the language-selection code.

### 1. UI strings

- Source (English) strings live in `app/src/main/res/values/strings.xml`
  (107 strings). Create a new folder `app/src/main/res/values-<code>/` with a
  `strings.xml` using the **same keys**, e.g. `values-fr/strings.xml`,
  `values-de/strings.xml`.
- Keep the key names identical to the English file and never remove a key —
  Android falls back to the English string for any missing one, but that's a
  regression for that user, and lint reports it as `MissingTranslation`.
- Preserve format placeholders exactly (`%s`, `%d`, `%1$s`), and keep
  apostrophes escaped as `\'`.

### 2. Default board content

The board that ships with the app is defined in
`app/src/main/java/com/unmute/app/data/DefaultSeed.kt` (9 sections, 89 cards).
Every board, category and card stores **both** languages:

- `boardNameEn` / `boardNameEs`, `nameEn` / `nameEs`
- per card: `labelEn` / `labelEs` (short label shown on the tile) and
  `phraseEn` / `phraseEs` (full sentence spoken when tapped)

Each card also has `imageType`, `imageValue`, `symbolValue` and `color` —
**do not translate those**; the symbol/emoji is the same in every language.
For a new language you (or the wiring PR) add a `*Fr` / `*De` field next to the
`*En` / `*Es` ones. A section or card can ship with an empty new-language field:
it will fall back to English, which is acceptable for a first pass.

Phrases should read like something the user would actually say out loud
("I am hungry", not "hunger"), while labels stay short enough for a tile.

### 3. Prediction word list

Autocomplete candidates come from `app/src/main/java/com/unmute/app/domain/model/CommonWords.kt`
(`ENGLISH`, ~700 words, and `SPANISH`, ~486 words). Add one list per language:

- lowercase only (matching is case-insensitive), no duplicates
- no words already covered by the default seed cards
- focus on high-frequency core words an AAC user needs quickly
- keep apostrophes inside words (`don't`), and keep accents as the language
  requires (Spanish keeps `él`, `más`, `café` — matching is case-insensitive,
  not accent-insensitive)

### 4. Wiring up the language

A translation is only *selectable* once the code knows about it. Keep
English as the fallback language. To add a language you need to touch:

- `AppLanguage` enum — `app/src/main/java/com/unmute/app/domain/model/Models.kt`
- `resolveLanguage()` mapping to a two-letter code —
  `app/src/main/java/com/unmute/app/domain/model/Localization.kt`
- the `label(lang)` / `phrase(lang)` / `withEditedLabels()` helpers in the same
  file, so they read the new fields with `ifBlank { ...En }` fallback
- the `Locale` mapping in `MainActivity.withAppLanguage()` —
  `app/src/main/java/com/unmute/app/MainActivity.kt`
- the language radios — `app/src/main/java/com/unmute/app/ui/settings/SettingsScreen.kt`

Existing backups load via `AppLanguage.valueOf()` and default to `SYSTEM` on
unknown values, so old backups stay safe.

### Verifying your translation

- `./gradlew assembleDebug` and install on a device or emulator.
- Switch to your language in Settings and check: app chrome, section names,
  card labels, the sentence bar, and word predictions.
- Confirm the fallback behaves: a card left untranslated shows English, not
  garbage.
- Add/extend unit tests in `app/src/test/java/com/unmute/app/domain/model/LocalizationTest.kt`
  mirroring the Spanish cases, and run `./gradlew testDebugUnitTest`.

---

## Opening a PR

1. Describe what you changed and why.
2. Reference any related issue (e.g. `Fixes #12`).
3. Mention how you tested the change.
4. If your PR touches UI, screenshots or a short screen recording are very
   welcome.

## Reviewing

All AI-assisted code must be reviewed by a human before merging. This also
applies to code written with AI tools: treat it as a draft and review it like
any other submission.

---

## Licensing

Unmute is licensed under the **GPL-3.0** (see `LICENSE`). By contributing, you
agree that your contributions are licensed under the same terms. Code that can't
be GPL-3.0-compatible won't be merged.

Some assets have different licenses — see `NOTICE`:

- Bundled symbols (Mulberry Symbols) are **CC BY-SA 4.0**. They must keep their
  attribution and cannot be relicensed.

---

## Reporting issues

Before opening an issue:

- Search open and closed issues to see if it's already known.
- For bugs, include: app version, Android version, device, and clear steps to
  reproduce.

---

## Need help?

Ask in an issue or PR — someone will get back to you. First-timers are very
welcome.
