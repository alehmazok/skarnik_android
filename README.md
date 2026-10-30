# Скарнік — Android

Native Android app for the Skarnik Belarusian dictionary (Kotlin, Jetpack Compose). Replaces the
[Flutter app](https://github.com/alehmazok/skarnik-flutter) as an in-place update.

See [CLAUDE.md](CLAUDE.md) for the project rules and [docs/](docs/) for architecture, decisions and the
feature parity register.

## Build and run

Everything below works from a terminal (or the VS Code terminal); Android Studio is not required.

### Prerequisites

- **JDK 17 or newer** (CI uses Temurin 17; 21 works too). `java -version` should print it, or set `JAVA_HOME`.
- **Android SDK** with these packages, installed with `sdkmanager` from the
  [command-line tools](https://developer.android.com/studio#command-line-tools-only):

  ```bash
  sdkmanager "platform-tools" "emulator" "platforms;android-37" "build-tools;36.0.0" \
    "system-images;android-35;google_apis;arm64-v8a"   # use x86_64 on an Intel machine
  ```

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

If a Flutter debug build is on the device, the native build installs over it as an update and keeps its data.
To go back to Flutter, uninstall first (`adb uninstall by.mazokaleh.skarnik.debug`): the native versionCode is
higher, so Android refuses a downgrade.

### Checks and tests

These are the same commands CI runs:

```bash
./gradlew ktlintCheck detekt lintDebug   # static checks
./gradlew ktlintFormat                   # auto-fix ktlint findings
./gradlew testDebugUnitTest              # JVM unit tests, incl. ObjectBox against the real search seed
```

Reports land in `app/build/reports/` (lint, detekt, ktlint, tests). Unit tests need no emulator.

CI also fails if the build changes `app/objectbox-models/default.json`; check with
`git diff -- app/objectbox-models/` after building (see [ADR 0001](docs/adr/0001-objectbox-on-flutter-stores.md)).

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

### Release build

`./gradlew assembleRelease` builds a minified APK, but release signing is not set up in this repo yet,
so the output is unsigned and cannot be installed as is.
