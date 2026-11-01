# 0008. Crash reporting: Sentry SDK without its Gradle plugin, not gated by analytics consent

**Status:** Accepted (2026-11-01)

## Context

Flutter reported to Firebase Crashlytics from release builds: uncaught errors as fatal, and every `SEVERE` log
record as a non-fatal (`logging.dart`). Aleh chose Sentry instead (2026-10-30); the org `skarnik` has a sponsored
open-source plan. The DSN is a repository secret (`SENTRY_DSN`).

Choices: the Sentry Gradle plugin (auto-installs the SDK, instruments OkHttp/Compose/logcat, uploads the R8
mapping) or the SDK alone with the mapping uploaded by `sentry-cli`; whether crash reports wait for the analytics
consent; which caught errors become non-fatals.

## Decision

- **SDK only** (`io.sentry:sentry-android`, with its NDK part for native crashes in ObjectBox). The Gradle plugin's
  AGP 9 support could not be confirmed, and its auto-instrumentation would add breadcrumbs with URLs and log lines
  that can carry the user's query. The release workflow generates a ProGuard UUID per build (passed to the SDK via
  `BuildConfig`) and uploads the R8 mapping under it with `sentry-cli` once `SENTRY_AUTH_TOKEN` is set.
- **Started by hand, release only.** `io.sentry.auto-init` is off; `CrashReporting.start` runs first in
  `SkarnikApplication.onCreate` and does nothing when the build has no DSN (debug builds, local release builds).
  App Distribution builds report as environment `internal`; the default is `production`.
- **Not gated by the analytics consent**, as Crashlytics wasn't in Flutter. The dialog asks about usage statistics;
  crash reports carry no user data: `sendDefaultPii` off (no IP, no user id), no screenshots, no view hierarchy, no
  tap breadcrumbs, no performance tracing.
- **Non-fatals through `logError(tag, message, error)`**, the replacement for `Log.e`. It sends the exception and the
  tag only: messages can name the query or word. `IOException` (offline, HTTP errors) is not reported; on a phone it
  is expected and would bury real bugs. Server health belongs to server monitoring.

## Consequences

- Stack traces of builds made before `SENTRY_AUTH_TOKEN` exists stay obfuscated in Sentry (the mapping is still in
  the workflow artifact for 30 days).
- A Play release workflow (10.1) must pass the DSN and `-Pskarnik.sentryEnvironment=production` the same way.
- Revisit the Gradle plugin when it documents AGP 9 support, if source context or automatic mapping upload is worth
  more than the manual step.
