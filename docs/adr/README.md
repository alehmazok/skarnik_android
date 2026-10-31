# Architecture Decision Records

One file per decision that had real alternatives: `NNNN-short-title.md`, numbered in order, never renumbered.
Rules without alternatives go straight into [CLAUDE.md](../CLAUDE.md). A superseded ADR stays, with its
status changed to `Superseded by NNNN`.

Template: **Status** (Proposed / Accepted / Superseded by NNNN), **Context**, **Decision**, **Consequences**.

| # | Decision | Status |
|---|---|---|
| [0001](0001-objectbox-on-flutter-stores.md) | Keep ObjectBox and open the Flutter stores in place | Accepted |
| [0002](0002-maestro-end-to-end-tests.md) | Maestro for end-to-end tests | Accepted |
| [0003](0003-translation-network-and-html.md) | Translation: plain OkHttp, PostgREST without an SDK, jsoup HTML rendering | Accepted |
| [0004](0004-offline-downloads-in-workmanager.md) | Offline dictionary downloads run as a WorkManager job | Accepted |
| [0005](0005-firebase-app-distribution.md) | Firebase App Distribution via its Gradle plugin, on every merge to main | Accepted |
