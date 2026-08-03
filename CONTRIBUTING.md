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
