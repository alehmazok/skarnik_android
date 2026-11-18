# Releasing

Maintainer guide: release builds, the Play bundle, tester builds through Firebase App Distribution, and the
repository secrets the workflows need. How to build and test locally is in the [README](../README.md).

## Release build

`./gradlew assembleRelease` builds a minified APK (R8 full mode with resource shrinking). It is signed with the
Play upload key when `key.properties` exists in the repository root (gitignored; keys `storePassword`,
`keyPassword`, `keyAlias`, `storeFile` relative to `app/`); otherwise it is unsigned.

Crash reporting is on only in release builds that got a `SENTRY_DSN` ([ADR 0008](adr/0008-sentry-crash-reporting.md)),
so local release builds send nothing unless you pass one.

## Google Play bundle

`./gradlew bundleRelease -Pskarnik.versionCode=<code>` builds the AAB for Google Play
(`app/build/outputs/bundle/release/app-release.aab`). The version code must be higher than every build already on
Play (and above the Flutter app's last, 81).

In CI: Actions → Play release bundle → Run workflow
([.github/workflows/play-bundle.yml](../.github/workflows/play-bundle.yml)). Leave the version code empty to use one
above the newest App Distribution build (read from Firebase), or enter one. It builds the signed AAB, uploads its
R8 mapping to Sentry and keeps the AAB and `mapping.txt` as the `skarnik-play-<code>` artifact for 90 days.

## Tester builds (Firebase App Distribution)

Every merge to `main` builds a signed release APK and uploads it to the Firebase App Distribution group
`internal` of the `skarnik-app` project ([.github/workflows/distribute.yml](../.github/workflows/distribute.yml),
[ADR 0005](adr/0005-firebase-app-distribution.md)). Run it by hand from Actions → Firebase App Distribution →
Run workflow. These builds report to Sentry as environment `internal`.

Without the secrets below the workflow still builds, and warns that nothing was uploaded. To upload from a local
machine:

```bash
./gradlew assembleRelease appDistributionUploadRelease --serviceCredentialsFile=/path/to/key.json
```

## Baseline Profile

After changing startup or the main screens, run Actions → Baseline Profile → Run workflow. It regenerates the
profile on CI's emulator and commits it ([ADR 0010](adr/0010-baseline-profile.md)).

## Repository secrets

Settings → Secrets and variables → Actions.

| Secret | Value | Used by |
|---|---|---|
| `ANDROID_KEYSTORE_BASE64` | The upload keystore, `base64 -i upload.keystore` | Play bundle, App Distribution |
| `ANDROID_STORE_PASSWORD` | Keystore password | Play bundle, App Distribution |
| `ANDROID_KEY_ALIAS` | Key alias | Play bundle, App Distribution |
| `ANDROID_KEY_PASSWORD` | Key password | Play bundle, App Distribution |
| `SENTRY_DSN` | DSN of the Android project in the Sentry org `skarnik`; turns crash reporting on in the release build | Play bundle, App Distribution |
| `SENTRY_AUTH_TOKEN` | Sentry auth token with `project:releases` scope; uploads the R8 mapping so stack traces are readable | Play bundle, App Distribution |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | JSON key of a `skarnik-app` service account with the *Firebase App Distribution Admin* role | App Distribution; Play bundle (to pick the version code) |

The Sentry project slug for the mapping upload is the repository variable `SENTRY_PROJECT` (default
`skarnik_android`).
