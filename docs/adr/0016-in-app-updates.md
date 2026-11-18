# 0016. In-app updates: Play's flexible flow once per version, immediate below a Remote Config minimum

**Status:** Accepted (2026-11-18)

## Context

In November 2026 the old Flutter build 3.5.2 still had about 1,900 active users on Android, more than twice the
native 4.0.x (#84). Old builds can't be prompted after the fact, so the goal is that every native build can prompt
for the next one. Google Play In-App Updates offers two flows: **flexible** (Play's prompt, the download runs in the
background while the app is used, then the app restarts to install) and **immediate** (a full-screen Play flow that
blocks the app until the update is installed). Questions: which flow, how often to ask, how to force an update
when a build must go (a broken release, an old API), and where to show "ready to restart".

## Decision

- **Library:** `com.google.android.play:app-update` 2.1.0 (the latest; required for apps targeting Android 14+),
  not `-ktx`: the plain artifact plus `core/common/TaskAwait.kt` covers it, as for in-app review. The flow is
  started with `startUpdateFlowForResult` and an Activity Result launcher.
- **Flexible by default, once per available version.** The `versionCode` Play offers is stored
  (`app_update_prompted_version_code`) before the prompt shows, as the review request and "what's new" do, so a
  user who says no is not asked again until a newer version is out. Daily nagging was the alternative; one ask per
  version respects the choice and still reaches everyone at each release.
- **Immediate below a minimum.** Remote Config `min_supported_version_code` (Android only, default 0 = never):
  a build below it starts the immediate flow at every check, and resumes one the user left. Closing it is
  respected until the next start of the app, so the app does not loop on its own prompt; meanwhile the flexible
  prompt can still show. A Remote Config number was chosen over Play's update priority (`inAppUpdatePriority`),
  which can only be set through the Play Developer API at upload time and can't be changed later.
- **When:** each time the app comes to the front (`ON_RESUME`), after the analytics consent dialog has been
  answered, so Play's prompt never covers it. Play is only asked about status; it decides availability itself.
- **"Ready to restart":** an M3 snackbar «Абнаўленне гатова» with «Перазапусціць» and a close button, in an
  app-wide `SnackbarHostState` that the home screen's Scaffold shows above the navigation bar. Shown when the
  download finishes and again at each return to the app until the update is installed (Play's recommendation). A
  dialog was the alternative; a snackbar doesn't interrupt a lookup.
- **Analytics** (through `Analytics`, so nothing before consent): `app_update_prompt_shown`,
  `app_update_accepted`, `app_update_dismissed` with `update_type` (`flexible`/`immediate`) and
  `available_version_code`; `app_update_restart_clicked` when the snackbar's action installs a flexible update.

## Consequences

- Only Play installs see anything. AppGallery, Firebase App Distribution, debug and sideloaded builds get an error
  or "no update" from Play, which is logged to Logcat only (not Sentry), as for in-app review.
- An immediate update usually restarts the app before its result arrives, so `app_update_accepted` undercounts the
  immediate flow; `app_update_prompt_shown` minus `app_update_dismissed` is the better measure there.
- The flow can't run on CI's emulator (no Play-installed build), so there is no Maestro flow; the decision logic and
  the view model are unit-tested with a fake. Checking it for real needs an internal testing track
  ([Play's internal app sharing](https://developer.android.com/guide/playcore/in-app-updates/test)).
- Setting `min_supported_version_code` above a build that is still the newest on someone's track blocks nobody:
  Play reports no update, so nothing starts.
