# 0002. Maestro for end-to-end tests

**Status:** Accepted (2026-10-30)

## Context

Unit and ViewModel tests cover logic, and Compose UI tests cover single screens. Neither checks whole user
journeys on a real device: install, first launch, search → translation → bookmark, deep links, the widget, and
above all the upgrade over the Flutter app, where user data must survive. Today those are checked by hand.

Options considered:

1. **Compose UI tests / Espresso only.** In-process, fast and precise, but tied to one app's code: they can't
   drive the Flutter build, cross app boundaries (share sheet, deep link from a browser, home-screen widget) or
   test an install-over-install upgrade.
2. **UI Automator tests in Kotlin.** Can do all of the above, but flows are verbose, slow to write and flaky
   without a lot of custom waiting code.
3. **Maestro.** Black-box YAML flows run against any installed APK through the accessibility tree, with
   built-in waiting and retries. Works the same on the Flutter app (Flutter exposes semantics), so one flow
   can prepare data in Flutter 3.6.0 and assert it in the native build.

## Decision

Use Maestro for end-to-end journeys, alongside (not instead of) unit, ViewModel and Compose UI tests.

- Flows live in `.maestro/`, one journey per file; see `.maestro/README.md` for the rules.
- Elements are found by Compose `testTag` (exposed as resource ids with `testTagsAsResourceId`) or visible
  text, never by coordinates.
- CI runs all flows on an Android emulator in a separate `e2e` job, against the debug APK built by the
  `build` job, and uploads screenshots. Locally, flows run on Aleh's emulator via Remote Control.
- Each feature PR adds or updates the flow for its journey. The Flutter → native upgrade check becomes a
  Maestro flow pair (seed data in the Flutter app, then assert after installing the native build).

## Consequences

- Journeys are checked on every PR without manual tapping; screenshots in CI give Aleh something to look at
  without installing the APK.
- The `e2e` job adds a few minutes of emulator time per PR and depends on the Maestro CLI and the emulator
  action; if it gets flaky, the fix is in the flow (waits, selectors), never retry-until-green.
- Compose UI tests stay the place for precise per-screen assertions (states, accessibility semantics).
