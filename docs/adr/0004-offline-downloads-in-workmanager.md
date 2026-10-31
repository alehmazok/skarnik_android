# 0004. Offline dictionary downloads run as a WorkManager job

**Status:** Accepted (2026-10-31)

## Context

Downloading a dictionary means a few hundred Supabase requests and a few minutes of work. Flutter ran it in an
app-scoped cubit: it survived leaving Settings but died with the process, and resumed only when the user tapped
again. Options for the native app:

1. An app-scoped coroutine, as in Flutter.
2. A WorkManager job (`CoroutineWorker`), unique per dictionary.
3. A foreground service (directly or through `setForeground`) with a progress notification.

## Decision

Option 2, without a foreground service. The use case saves the cursor after every page and rewriting a page is
harmless, so the job can be stopped at any point: when the system stops it (10-minute limit, process death,
Doze), WorkManager runs it again later and it resumes from the cursor. Unique work (`KEEP`) replaces Flutter's
"one cubit" guard against parallel downloads, and `WorkInfo` progress drives the Settings row. The job has no
network constraint: without network it fails after 5 attempts per request and the row shows the error, as in
Flutter, instead of waiting silently.

A foreground service would need `FOREGROUND_SERVICE_DATA_SYNC`, a notification channel and, on Android 13+,
the notification permission, for little gain once the job resumes on its own.

## Consequences

- WorkManager's default initializer is removed from the manifest; `SkarnikApplication` provides the
  configuration with `HiltWorkerFactory`.
- A download started in the background may pause under Doze and finish later; the row keeps showing progress.
- If the job record is pruned, a leftover cursor with stored words still marks the dictionary as unfinished.
