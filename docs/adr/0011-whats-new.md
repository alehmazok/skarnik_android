# 0011. What's new: release notes bundled in the app, shown once after an update

**Status:** Accepted (2026-11-06)

## Context

Aleh wants people to see what changed after an update, in the iOS and Android apps alike. Questions: where the
notes come from (bundled with the release, or Firebase Remote Config so the text can change without a release),
when they show, and how to tell an update from a fresh install, including the update from the Flutter app, which
never stored a "last seen" version.

## Decision

- **Bundled notes** (Aleh, 2026-11-06). `WhatsNewCatalog` lists releases newest first; texts are string
  resources. A notes entry goes into the same PR as the user-visible change, so it is reviewed with it and works
  offline. Remote Config was rejected: the text could change without a release, but the sheet would not show
  offline and the notes would live away from the code they describe.
- **A modal bottom sheet** (M3), opened over whatever screen is shown at start, once per version that has notes,
  after the consent dialog has been answered. Settings → «Што новага» opens a sheet with every release.
- **Fresh install or update** comes from `PackageInfo`: an update keeps `firstInstallTime` and moves
  `lastUpdateTime`. This covers people updating from Flutter or from a native build without the key, with no
  heuristics over other stored keys. The version is stored as seen before the sheet shows (as the review request),
  so it shows at most once even if the app is closed.

## Consequences

- A version without notes shows nothing; versions skipped by the user show only the newest release's notes.
- Someone who installs, never opens the app and then gets an update sees that update's notes on first start.
- The iOS app has the same behaviour; iOS has no install times, so it tells a fresh install by the absence of
  any stored user data at launch.
