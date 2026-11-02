# 0010. Startup performance: a committed Baseline Profile generated on CI's emulator

**Status:** Accepted (2026-11-02)

## Context

Without ahead-of-time compiled code, the first launches after an install or update run the app interpreted and
JIT-compiled: Compose, Hilt, ObjectBox and navigation all start slowly until ART has profiled them. A Baseline
Profile lists the classes and methods to compile at install time; a startup profile lets R8 put the startup
classes into the first dex file. Play applies the profile at install, and also builds cloud profiles from users'
devices, but only for Play installs. Firebase App Distribution and Huawei AppGallery install plain APKs.

Choices: where the profile comes from (generated during every release build, or generated on demand and
committed), which devices generate it (Gradle managed devices or the emulator CI already starts), and how the
gain is checked.

## Decision

- **A `:baselineprofile` test module** with the `androidx.baselineprofile` plugin (1.5, the first that works with
  AGP 9) generates both profiles from the journeys in `Journeys.kt`: cold start to History (answering the consent
  dialog with «Не дазваляць»), search with a letter substitution and opening a word, the Dictionary tab with
  flings, Bookmarks. Elements are found by the test tags the Maestro flows use.
- **Committed, regenerated on demand.** The profile lives in `app/src/release/generated/baselineProfiles/`; release
  builds only pack it (`automaticGenerationDuringBuild = false`), so a release never depends on an emulator.
  `.github/workflows/baseline-profile.yml` (manual, and on pull requests that change the generator) regenerates it
  on the same API 35 emulator as the Maestro job, pushes it to the branch (from main: to `baseline-profile/update`
  with a pull request) and starts CI for that commit.
- **`profileinstaller` in the app**, so APK installs (App Distribution, AppGallery) compile the profile on the
  first launches too, not only Play installs.
- **`StartupBenchmark`** measures cold start to History with no compilation and with the profile
  (`BaselineProfileMode.Require`, so a missing profile fails instead of measuring nothing). The workflow writes the
  medians to its summary. Emulator numbers are for comparing the two modes, not absolute times.
- The plugin's `benchmarkRelease` and `nonMinifiedRelease` build types are signed with the debug key: they copy
  release, which has no key on pull requests, and they never leave test devices.

## Consequences

- Rerun the workflow after changes to startup or the main screens; a stale profile still helps but covers less.
- Gradle managed devices would download a system image on every run and duplicate the emulator setup CI already
  has, so they are not used.
- Journeys that open a translation depend on the network; they wait for it but do not fail without it.
