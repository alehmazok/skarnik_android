# 0004. Firebase App Distribution for tester builds

**Status:** Accepted (2026-10-31)

## Context

Testers need every change on a real phone without building it, as a release build (R8, the upload key), so
they see what Play users will get. The Flutter app already has a Firebase project (`skarnik-app`) with an
Android app registered for `by.mazokaleh.skarnik` and a tester group `internal`, and distributed from a local
fastlane lane with a `FIREBASE_CLI_TOKEN` (deprecated by Firebase).

Options considered:

1. **fastlane + `firebase_app_distribution` plugin**, as in Flutter. Adds Ruby, Bundler and a Gemfile to a
   Gradle-only repo for one upload.
2. **firebase-tools CLI (`npx firebase-tools appdistribution:distribute`)** or a third-party GitHub Action
   wrapping it. Adds Node; the CLI version lives in a workflow file where Dependabot doesn't see it.
3. **Firebase App Distribution Gradle plugin** (`appDistributionUploadRelease`). One Gradle task, its version
   in `libs.versions.toml` next to everything else and bumped by Dependabot, same command locally and in CI.

## Decision

Use the Gradle plugin (option 3), driven by `.github/workflows/distribute.yml`:

- Every push to `main` builds a signed, minified release APK and uploads it to the `internal` group. Manual
  runs are possible from the Actions tab. PRs that touch the release build config build it without uploading.
- Same Firebase app as Flutter (same application id), so testers who have a Flutter build from App
  Distribution get the native build as an update, which also exercises the data migration.
- Signed with the existing Play upload key from `key.properties` (Flutter's format); CI writes it from the
  same secrets the Flutter repo uses. Without it the release build stays unsigned and nothing is uploaded.
- `versionCode` is `100 + run number` of the workflow, so every build installs over the previous one.
- Authentication with a service account key (role *Firebase App Distribution Admin*) in a repository secret.
  Workload Identity Federation (keyless) is supported by the plugin and can replace the key later.

## Consequences

- A merge reaches testers in about ten minutes with no manual step; the R8 mapping file is kept as a
  workflow artifact for 30 days.
- The release build is now built in CI, so R8 keep-rule problems show up on merge, not at Play release time.
- If Play App Signing is on, a tester build can't install over the Play version (different signature): testers
  uninstall the Play app first or use a separate device.
