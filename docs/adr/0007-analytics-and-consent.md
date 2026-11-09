# 0007. Analytics: Firebase without the google-services plugin, one typed event logger, off until consent

**Status:** Accepted (2026-10-31)

## Context

Flutter logs usage events to Firebase Analytics after a first-launch consent dialog. Every feature had its own
analytics repository interface with a `DevAnalytics*` (logs only) and a `FirebaseAnalytics*` implementation.
Firebase was configured by the google-services plugin from a `google-services.json` that CI restored from a
secret. The native app must keep the event names (the reports span both apps), must not send anything before the
user answers, and must not ask people again who already answered in the Flutter app.

Options for configuration: the google-services plugin with the JSON from a secret, or the same values as string
resources from `gradle.properties`. Options for the logger: per-feature repositories as in Flutter, or one
interface with typed events. Options for debug builds: a separate logging implementation, or the same one with
the SDK deactivated.

## Decision

- **Configuration without the plugin.** The five values `FirebaseInitProvider` reads (`google_app_id`,
  `google_api_key`, `gcm_defaultSenderId`, `project_id`, `google_storage_bucket`) are `resValue`s built from
  `gradle.properties`. They are the public client config of the Android app Flutter registered (Flutter commits
  them in `lib/firebase_options.dart`), so there is no secret and no extra plugin. `res/raw/keep_firebase.xml`
  keeps them from resource shrinking, since Firebase reads them by name.
- **One logger.** `Analytics.log(AnalyticsEvent)` with a sealed `AnalyticsEvent` per Flutter event. A layer of
  per-feature interfaces would only forward calls; typed events keep the names and parameters in one file, where
  `AnalyticsEventTest` pins them to Flutter's.
- **Consent.** The manifest sets `firebase_analytics_collection_enabled=false`. `AnalyticsConsent` stores the
  answer under Flutter's keys in the app's DataStore (a `FlutterPreferencesMigration` copies Flutter's), calls
  `setAnalyticsCollectionEnabled` when it changes and again at every app start. The dialog cannot be dismissed
  with back or a tap outside; declining is as easy as allowing. The Settings switch is the same answer.
- **Anonymous.** Advertising-id collection and automatic screen views are off in the manifest: the dialog
  promises anonymous statistics, and Flutter already turned screen views off.
- **No advertising-id permission** (issue #47, the counterpart of skarnik_ios#65). Firebase Analytics merges
  `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID` and `ACCESS_ADSERVICES_ATTRIBUTION` into the
  app; the manifest removes all three with `tools:node="remove"`, Google's documented way (Android has no
  Firebase artifact without the ad id, unlike iOS). On Android 13+ the system hands an app without `AD_ID` a zeroed id anyway,
  and collection is off. The `play-services-ads-identifier` library itself stays: the measurement code links
  against it, and dropping it risks R8 "missing class" errors or a crash in release that only a store build
  would show, for a few KB. `checkReleaseManifestPermissions` (in `app/build.gradle.kts`, run by CI) fails when
  the merged release manifest declares any of them, so a Firebase BoM bump can't bring one back unnoticed.
- **Debug builds** have `firebase_analytics_collection_deactivated=true` (a manifest placeholder): nothing they do
  reaches the production reports, whatever the consent. The tracker prints every event to Logcat in debug.

## Consequences

- No Firebase secret in CI; a new Firebase app would mean editing `gradle.properties`.
- Release builds from App Distribution report like store builds once a tester allows analytics, as Flutter's did.
- Every Maestro flow that starts from a clean install declines the dialog first (`subflows/decline-analytics.yaml`).
- Play Console: the Advertising ID declaration is answered "no"; Data safety still lists device or other ids
  (the Firebase app instance id) for analytics, not shared, not for ads or tracking.
- Remote Config (8.8) reuses the same Firebase configuration. Its fetch is not analytics and runs before consent,
  as in Flutter: it sends a Firebase installation id and no events.
