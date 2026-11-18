<div align="center">
  <img src="docs/assets/logo.png" alt="Skarnik logo" width="120" />

  # Скарнік — Android

  Belarusian dictionary app: Бел-Рус, Рус-Бел and Тлумачальны dictionaries in one place.
  Native Android, written in Kotlin with Jetpack Compose.

  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

  <a href="https://play.google.com/store/apps/details?id=by.mazokaleh.skarnik">
    <img src="docs/assets/google-play-badge.svg" alt="Get it on Google Play" width="200" />
  </a>
</div>

## About

Skarnik is a Belarusian–Russian, Russian–Belarusian and Belarusian explanatory (ТСБМ) dictionary. Search works
offline. Translations come from the downloaded offline dictionary first, then Supabase, then the Skarnik admin API.

This app is the native rewrite of the earlier Flutter app and ships as an update of the same Play listing
(`by.mazokaleh.skarnik`). It opens the Flutter app's data in place, so users keep their history, bookmarks and
downloaded dictionaries.

## Features

- Search across all three dictionaries, offline, with letter substitution (и→і, е→ё, щ→ў, ъ→‘), a fuzzy fallback
  for misspelled words, and extra Belarusian letter buttons above the keyboard
- Offline dictionaries: download any of the three for reading translations without a connection
- Translations with tappable links: Belarusian words open their own entry, and abbreviations («разг.», «муж.»)
  show their full text in a tooltip
- A link between the same headword in Бел-Рус and ТСБМ
- «Націск»: stress tables for a word, from starnik.by with the Skarnik admin API as fallback
- History and Закладкі (bookmarks), sorted by date added or alphabetically
- Dictionary tab: browse each dictionary alphabetically
- «Слова дня» (Word of the Day) home screen widget
- Opens skarnik.app share links (App Links) and shares words as skarnik.app links
- Wide screens and foldables: the list and the word side by side
- Material You colours or the Skarnik brand palette; system, light or dark theme
- «Што новага» (What's new) after each update
- Firebase Analytics only after the user's consent; Sentry crash reporting in release builds

## Tech stack

- Kotlin, Jetpack Compose with Material 3, single Activity, Navigation Compose with type-safe routes
- Hilt (KSP) for dependency injection, Coroutines and Flow
- ObjectBox for the search index, history and offline dictionaries (the Flutter app's stores)
- OkHttp with a disk cache; Supabase over PostgREST without its SDK; kotlinx.serialization
- jsoup to turn translation HTML into Compose text
- DataStore for settings, WorkManager for downloads, Glance for the widget
- Firebase Analytics and Remote Config, Sentry
- Tests: JUnit, Turbine, [Maestro](https://maestro.mobile.dev) end-to-end flows

## Project layout

```
app/src/main/java/by/mazokaleh/skarnik/
├── core/                    shared code: database, network, analytics, models
├── feature/<name>/          one package per feature: data/, domain/, ui/
├── navigation/              NavHost, routes, list–detail layout
└── ui/theme/                Material 3 theme
baselineprofile/             Baseline Profile generator and startup benchmark
.maestro/                    end-to-end flows
docs/                        architecture, decisions, feature parity, releasing
```

- [docs/architecture.md](docs/architecture.md): how the app is built
- [docs/adr/](docs/adr/README.md): decisions and why
- [docs/parity.md](docs/parity.md): what was ported from the Flutter app and how it differs
- [docs/releasing.md](docs/releasing.md): release builds, Google Play, tester builds, secrets
- [CLAUDE.md](CLAUDE.md): project rules for contributors

## Build and run

Everything below works from a terminal (or the VS Code terminal); Android Studio is not required.

### Prerequisites

- **JDK 17 or newer** (CI uses Temurin 17; 21 works too). `java -version` should print it, or set `JAVA_HOME`.
- **Android SDK** with these packages, installed with `sdkmanager` from the
  [command-line tools](https://developer.android.com/studio#command-line-tools-only):

  ```bash
  sdkmanager "platform-tools" "emulator" "system-images;android-35;google_apis;arm64-v8a"   # x86_64 on Intel
  ```

  Gradle downloads the SDK platform and build tools the build needs on the first run.

- The SDK location and tools on `PATH`, for example in `~/.zshrc`:

  ```bash
  export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS; on Linux usually ~/Android/Sdk
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
  ```

  Gradle finds the SDK through `ANDROID_HOME`. Alternatively put `sdk.dir=/path/to/sdk` in `local.properties`
  (git-ignored).

- **An emulator (AVD) or a device.** Create an AVD once:

  ```bash
  avdmanager create avd -n Pixel_API_35 -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_8
  ```

  For a physical phone, turn on USB debugging and check that `adb devices` lists it.

Gradle itself comes with the wrapper (`./gradlew`); the first build downloads it and all dependencies.

### Build the debug APK

```bash
./gradlew assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. The debug build has the application id
`by.mazokaleh.skarnik.debug` and the name "Debug Skarnik", so it installs next to the store app.

### Install and launch

Start the emulator (skip this for a connected phone) and wait until it has booted:

```bash
emulator -avd Pixel_API_35 &
adb wait-for-device
```

Then build, install and start the app in one go:

```bash
./gradlew installDebug
adb shell am start -n by.mazokaleh.skarnik.debug/by.mazokaleh.skarnik.MainActivity
```

The first launch unpacks the search index (about 60 MB), so the home screen can take a few seconds.
App logs: `adb logcat --pid=$(adb shell pidof by.mazokaleh.skarnik.debug)`.

### Checks and tests

```bash
./gradlew ktlintCheck detekt lintDebug   # static checks
./gradlew ktlintFormat                   # auto-fix ktlint findings
./gradlew testDebugUnitTest              # JVM unit tests, incl. ObjectBox against the real search seed
```

Reports land in `app/build/reports/` (lint, detekt, ktlint, tests). Unit tests need no emulator.

The build must not change `app/objectbox-models/default.json`; check with `git diff -- app/objectbox-models/`
after building (see [ADR 0001](docs/adr/0001-objectbox-on-flutter-stores.md)).

### End-to-end flows (Maestro)

[Maestro](https://maestro.mobile.dev) drives the installed app on a running emulator or device.
Install it once:

```bash
curl -fsSL "https://get.maestro.mobile.dev" | bash   # adds ~/.maestro/bin to PATH
```

Then, with the emulator running:

```bash
./gradlew installDebug
maestro test .maestro/                  # all flows
maestro test .maestro/smoke.yaml        # one flow
```

Flows start from a cleared app state, so they wipe the debug app's history and bookmarks. Screenshots are
saved to `~/.maestro/tests/`. How to write flows: [.maestro/README.md](.maestro/README.md).

### CI

GitHub Actions ([.github/workflows/ci.yml](.github/workflows/ci.yml)) runs on every pull request and every push
to `main`: the static checks, the unit tests, a check that `app/objectbox-models/default.json` is unchanged, and
every Maestro flow on an Android emulator, the phone flows in `.maestro/` and the wide-screen flows in
`.maestro/wide/`. Release and tester builds are described in [docs/releasing.md](docs/releasing.md).

## Related projects

- [skarnik_ios](https://github.com/alehmazok/skarnik_ios): the native iOS app
- [skarnik.app](https://github.com/alehmazok/skarnik.app): the website, share links and the installable web app
- [skarnik_admin](https://github.com/alehmazok/skarnik_admin): the admin backend and API for the dictionary data

## Privacy and contact

The privacy policy is at [skarnik.app/privacy](https://skarnik.app/privacy). Questions and feedback go to the
Skarnik developer at [skarnikapp@gmail.com](mailto:skarnikapp@gmail.com), or through «Напісаць распрацоўшчыкам»
in the app's settings.

## License

[MIT](LICENSE)
