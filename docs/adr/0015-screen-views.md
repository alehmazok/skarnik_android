# 0015. Screen views: logged by hand from the navigation destinations

**Status:** Accepted (2026-11-18)

## Context

GA4 showed no `screen_view` from Android (#83), while iOS sends them through UIKit's automatic tracking, so
"Pages and screens", engagement per screen and path exploration were iOS-only. Firebase's automatic screen
reporting on Android sees activities, and the app has one, so it would only ever report `MainActivity`; it stays
off in the manifest. Screens are Navigation Compose destinations in two hosts (the main one and the
`ListDetailLayout` pane), and the three start-screen tabs are not destinations at all.

## Decision

- `AnalyticsEvent.ScreenView` logs Firebase's `screen_view` with `screen_name` (a stable name from the `Screen`
  enum: `history`, `favorites`, `vocabulary`, `search`, `translation`, `stress`, `stress_table`, `settings`) and
  `screen_class` (the route class name, written out since R8 renames classes). It goes through `Analytics`, so
  nothing is sent before consent. The same names are proposed for skarnik_ios.
- Each destination calls `TrackScreenView(screen)`; the start screen calls it with the selected tab. It works
  from the destination's lifecycle instead of an `OnDestinationChangedListener`, so the pane's words, the tabs and
  both hosts use one mechanism, and on a wide screen a word opened beside the list is logged while the list stays
  shown.
- `ScreenViewTracker` keeps the screens in view by key (back stack entry id + screen). A screen is logged when it
  resumes and is not in view yet; it leaves the view when it pauses while the activity stays resumed (covered by
  another destination) or leaves the composition without a configuration change (tab switch, back). So rotation,
  a theme change and a return from the background log nothing, and coming back to a screen logs it again.
- Dialogs and bottom sheets (consent, «Пра Скарнік», «Што новага») are not screens.

## Consequences

- A user's first screen is usually logged before they answer the consent dialog, so it is dropped for new users.
- The placeholder pane («Выберыце слова са спіса») is not a screen; backing out of a pane word does not log the
  list again, since it never left the view.
