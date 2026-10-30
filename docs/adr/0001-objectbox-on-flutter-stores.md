# 0001. Keep ObjectBox and open the Flutter stores in place

**Status:** Accepted (2026-10-30)

## Context

The native app replaces the Flutter app as an update of `by.mazokaleh.skarnik`. The Flutter app keeps all
local data in three ObjectBox (Dart) stores under the app's `app_flutter` directory:

- `objectbox_search`: the ~316k-word search index, copied from a bundled asset on first launch;
- `objectbox_history`: search history and bookmarks (user data);
- `objectbox_translation`: cached translations and user-downloaded offline dictionaries (user data,
  up to tens of MB per dictionary, slow and rate-limited to re-download).

Losing history and bookmarks on update would be the most visible regression of the rewrite.

Options considered:

1. **ObjectBox Kotlin on the same files.** ObjectBox Java/Kotlin and Dart share the same native core and file
   format; a store is compatible when the model UIDs match.
2. **Room (SQLite) plus a one-time migration** that reads the ObjectBox stores and copies them into SQLite.
   Needs ObjectBox in the app anyway for the migration, so two databases ship for months, plus migration
   code that must handle partial runs. The search index would be rebuilt as an SQLite/FTS asset.
3. **Room and drop local data.** Simplest code, but users lose history, bookmarks and offline dictionaries.

## Decision

Use ObjectBox Kotlin and open the existing stores in place:

- Entities `ObjectboxSearchWord`, `ObjectboxHistoryWord`, `ObjectboxFavoriteWord`, `ObjectboxTranslationWord`
  keep the Flutter class names, property names, types (`Long` for Dart `int`) and index types, and carry the
  same `@Uid` values.
- `app/objectbox-models/default.json` is a copy of skarnik-flutter's `lib/objectbox-model.json` (including
  retired UIDs). CI fails if the build changes it, so model drift is reviewed, not accidental.
- Stores open at `Context.getDir("flutter")/objectbox_{search,history,translation}`, the directory Flutter's
  `path_provider` used as documents directory.
- The search seed is bundled gzip-compressed (`assets/objectbox/search_seed.mdb.gzip`, ~9 MB instead of 60 MB in git)
  and installed via a temp file + rename, so an interrupted first launch can't leave a broken store.

## Consequences

- Users keep all data on update with no migration step and no second database.
- Search keeps ObjectBox's indexed prefix queries, which the Flutter search algorithm is written against.
- We are tied to ObjectBox's Kotlin API and its kapt-based code generator (Hilt uses KSP; both run).
- `ObjectBoxStoreOpenerTest` opens the real Dart-written seed with the Kotlin model on every CI run. The
  history and translation stores are covered by an upgrade test on the emulator (install Flutter 3.6.0 debug,
  add history/bookmarks/an offline dictionary, install the native debug build over it), done once Remote
  Control on Aleh's machine is set up.
- Moving to Room later stays possible as a one-time migration (option 2), and would get its own ADR.
