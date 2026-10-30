# CLAUDE.md — Skarnik Android

Native Android rewrite of [skarnik-flutter](https://github.com/alehmazok/skarnik-flutter): the Skarnik
Belarusian dictionary (Бел-Рус, Рус-Бел, Тлумачальны). It ships as an **update** of the existing Play app
`by.mazokaleh.skarnik`, so users must keep their history, bookmarks and downloaded dictionaries.

- Architecture (living reference): [docs/architecture.md](docs/architecture.md)
- Decisions and why: [docs/adr/](docs/adr/README.md)
- What is ported and what is left: [docs/parity.md](docs/parity.md)

## Roles

Aleh owns product: what to build, in which order, user-visible texts, release timing. Claude owns every
technical decision (architecture, libraries, code, tests, performance, CI) and states decisions instead of
asking. Product questions go to Aleh as a single multiple-choice question with a recommendation.

## How work is done

- One thread = one feature = one PR. Port behaviour from the Dart code, not from prose: the Flutter source
  (and `skarnik_ios` for shared specs) is the spec for behaviour and data logic, not for screens. Write a short spec in `docs/` only for non-obvious
  algorithms (search, translation fallback chain, dictionary download).
- Every PR: tests for the new logic, `docs/parity.md` updated, CI green. Self-review the diff before opening.
- A decision that had real alternatives gets an ADR in `docs/adr/` (`NNNN-title.md`). Plain rules go here.
- Emulator checks run on Aleh's machine via Remote Control at the end of each feature.

## Commands

```bash
./gradlew ktlintCheck detekt lintDebug   # static checks (what CI runs)
./gradlew ktlintFormat                   # auto-fix formatting
./gradlew testDebugUnitTest              # JVM unit tests, incl. ObjectBox against the real seed DB
./gradlew assembleDebug                  # debug APK (id by.mazokaleh.skarnik.debug)
maestro test .maestro/                   # end-to-end flows on a running emulator (after installDebug)
```

JDK 17. The cloud environment has no Android SDK or Maven access, so CI is the build of record there.

## Technical rules

**Stack.** Kotlin 2.x, Jetpack Compose + Material 3, single Activity, Navigation Compose with type-safe
`@Serializable` routes. Hilt (KSP) for DI. Coroutines + Flow. kotlinx.serialization for JSON. Retrofit or
Ktor on OkHttp with a disk cache (decided in the translation PR). DataStore instead of SharedPreferences.
WorkManager for background work, Glance for the widget. Versions live only in `gradle/libs.versions.toml`.

**Versions.** Use the latest stable SDK, AGP, Gradle, Kotlin and library versions. Check every version against
its official release page or Maven metadata when adding or bumping it, never from memory. No alphas/betas unless
an ADR says why. Dependabot opens grouped update PRs weekly (`.github/dependabot.yml`); merge them once CI is
green, and read the release notes for major bumps. AGP 9 has Kotlin built in: there is no
`org.jetbrains.kotlin.android` plugin, and kapt (only for ObjectBox) comes from `com.android.legacy-kapt`.

**Architecture.** UI (Compose) → ViewModel (one `StateFlow<UiState>` per screen) → use case (only when it
holds logic) → repository (interface in the feature, implementation in `data`) → data source. Feature-first
packages under `feature/<name>/{data,domain,ui}`; shared code under `core/`. No layer that only forwards calls.

**Storage compatibility (do not break).**
- ObjectBox entity class names, property names and types (`Long`, not `Int`) and index types must match
  skarnik-flutter's `lib/objectbox-model.json`. `app/objectbox-models/default.json` is that file; CI fails
  if the build changes it. Never delete or "clean up" retired UIDs in it.
- Stores live in `app_flutter/objectbox_{search,history,translation}` (`Context.getDir("flutter")`).
- `langId`: RusBel = 0, BelRus = 1, Tsbm = 2. Never renumber.
- Flutter `shared_preferences` data (in `FlutterSharedPreferences.xml`, keys prefixed `flutter.`) is
  migrated into DataStore once, by the feature that owns each key (see parity register).
- `applicationId` stays `by.mazokaleh.skarnik` (debug: `.debug` suffix, same as Flutter). `versionCode`
  must stay above Flutter's last (81).

**Threading and performance.** No disk or network on the main thread: inject `@IoDispatcher` /
`@DefaultDispatcher`, never call `Dispatchers.IO` directly in classes. Stores open lazily via
`SkarnikDatabase.stores()`. Compose: immutable/stable UI state, `key` in lazy lists, `collectAsStateWithLifecycle`.
R8 full mode with resource shrinking in release. Baseline Profile and startup macrobenchmark come with the
first real screens.

**Quality.** ktlint (android_studio style) + detekt + Android Lint must pass. Unit tests for logic and
ViewModels (JUnit4, kotlin.test, coroutines-test, Turbine, fakes over mocks). Compose UI tests for key screens.
Maestro flows in `.maestro/` for every user journey a feature adds ([ADR 0002](docs/adr/0002-maestro-end-to-end-tests.md));
CI runs them on an emulator. Give interactive and asserted elements a `Modifier.testTag`.
`CancellationException` is always rethrown.

**Theme (agreed with Aleh, 2026-10-30).** Material 3. Follows system light/dark by default, with a
System/Light/Dark setting in DataStore. Material You dynamic colour by default on Android 12+, with a setting
to switch to the brand palette (light: white + red `#E1251B`; dark: `#0F0F10` + `#FF4438`, same as skarnik.app).
No AMOLED theme. System font, sizes in `sp` via `MaterialTheme.typography`. Adaptive icon with a monochrome
layer. Colours in translation HTML are mapped to theme roles, never rendered as fixed hex. Never hard-code
colours in screens: use `MaterialTheme.colorScheme`.

**UI.** Material 3 guidelines come first (Aleh, 2026-10-30): do not copy Flutter screen layouts, navigation
or UX flows blindly. Where M3 has a better pattern (SearchBar, navigation bar/rail by window size, bottom
sheets, etc.), use it and note the deviation in the feature's row in `docs/parity.md`. All user-visible strings in Belarusian in `res/values/strings.xml` (use `plurals` for counts).
Resources are pinned to `be` (`withAppLocale()` in `attachBaseContext`) so plurals follow Belarusian rules
on any device. Dark theme, TalkBack labels, large fonts, edge-to-edge, predictive back. minSdk 24.

**Analytics, crashes and secrets.** Same Firebase Analytics event names as Flutter (listed in parity
register); nothing is sent before consent. Crash reporting is Sentry (org `skarnik`), not Crashlytics. Secrets
(incl. the Sentry DSN) only via `local.properties` / GitHub Secrets, never in code or git.
