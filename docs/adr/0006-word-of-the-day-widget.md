# 0006. Word-of-the-day widget: Glance, a periodic job while placed, state in its own DataStore

**Status:** Accepted (2026-10-31)

## Context

Flutter's «Слова дня» widget was a Glance widget fed by the `home_widget` plugin: the `workmanager` plugin ran
a Dart isolate hourly (and once at every app start) to pick a word, wrote it to `HomeWidgetPreferences` and
asked Glance to redraw. Its history lived in `SharedPreferencesAsync` under `widget_word_history_v1`. The
native app has to keep widgets that users already placed, and decide how to schedule and where to keep state.

Options for the state: Glance's per-widget `PreferencesGlanceStateDefinition`, the app's settings DataStore, or
a separate DataStore. Options for scheduling: always (as Flutter), or only while a widget exists.

## Decision

- Glance `GlanceAppWidget` with the Flutter provider's class name, `by.mazokaleh.skarnik.widget.WordOfDayWidgetReceiver`.
  The launcher binds placed widgets by that component name.
- One unique periodic WorkManager job (`word_of_the_day`, hourly, `KEEP`), enqueued from the receiver's
  `onEnabled`/`onUpdate` and cancelled in `onDisabled`. The worker also cancels itself when no widget is left.
  No separate one-off job: a newly enqueued periodic job runs at once.
- State in a separate DataStore, `word_of_the_day`: all widgets show the same word, so per-widget Glance state
  would only duplicate it, and the history grows with every refresh, so it stays out of the settings file.
  `widget_word_history_v1` is migrated from Flutter's DataStore and the shown word from `HomeWidgetPreferences`.
- Flutter's two workmanager unique jobs (`word-of-day-refresh`, `word-of-day-refresh-initial`) are cancelled at
  every app start: their worker class no longer exists, so WorkManager would fail them every hour.

## Consequences

- Users without a widget get no background work at all.
- A refresh without network keeps the current word (Flutter switched to the fallback word).
- The hourly cadence comes from Flutter; it can change in one place (`WordOfTheDayWork`) if product wants a
  daily word.
