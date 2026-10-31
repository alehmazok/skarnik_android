# Feature parity register

Every user-facing behaviour of skarnik-flutter 3.6.0, built from its Dart code, and its status in the native app.

**For Aleh:** strike out (`~~like this~~`) or delete any row we should not port, and add rows for things the
Flutter app doesn't have. Everything not struck out is ported. Product questions are marked ❓.

The Flutter app is the spec for behaviour and data, not for layout: screens follow Material 3, and a row notes
where the native UI deliberately differs ("M3: …").

Status: ⬜ not started · 🟨 in progress · ✅ done (PR link) · ➖ dropped

Paths are relative to `skarnik-flutter/lib/features/`.

## 0. Foundation

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 0.1 | Same app id `by.mazokaleh.skarnik`; installs as an update | `android/app/build.gradle` | ✅ skeleton PR |
| 0.2 | Open existing ObjectBox stores (search, history, translation) at `app_flutter/objectbox_*`; seed the search index from the bundled asset on first launch | `app/data/repository/objectbox_database_repository.dart` | ✅ skeleton PR; emulator upgrade 3.6.0 → 4.0.0 passed 2026-10-30 (all stores byte-identical, no re-seed) |
| 0.3 | Migrate Flutter settings into DataStore (each key with its feature, listed below). Source is `files/datastore/FlutterSharedPreferences.preferences_pb` (`SharedPreferencesAsync`), not the XML file. Each feature adds a one-time `FlutterPreferencesMigration` to the app's `settings` store; Flutter's file is left untouched | various `shared_preferences_*_repository.dart` | 🟨 mechanism + `favorites_sort_order` in history/bookmarks PR; download cursors + promo flag in offline PR; other keys with their features |
| 0.3a | Delete Flutter's HTTP cache `DioCache.db` once after the upgrade (cache only, not migrated; native uses OkHttp's cache in `cache/http`) | `translation/data/http/skarnik_dio.dart` | ✅ settings PR (`context.deleteDatabase("DioCache.db")` at every start, a no-op once gone) |
| 0.4 | Light/dark theme following the system | `app/presentation/skarnik_app.dart` | ✅ skeleton PR |
| 0.4a | New: System/Light/Dark setting; Material You dynamic colour with a switch to the brand palette (agreed 2026-10-30) | — | ✅ settings PR: «Выгляд» section with «Тэма» (single-choice dialog: Як у сістэме / Светлая / Цёмная) and «Колеры шпалер» (Android 12+ only); keys `theme_mode`, `dynamic_color` in the app's DataStore. System bar icons follow the chosen theme |
| 0.5 | Adaptive layout: list + translation side by side on wide screens | `widgets/master_detail_view.dart`, `widgets/breakpoints.dart` | ⬜ |
| 0.6 | Crash reporting, release-only. **Sentry** (org `skarnik`) instead of Crashlytics (Aleh, 2026-10-30) | `main.dart` | ⬜ |

## 1. Search

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 1.1 | Search screen opened from the History tab's search field; results across all three dictionaries. Each stage (word, mask, fuzzy) returns up to 15, so up to 30 rows can show, as in Flutter. M3: collapsed search bar on the start screen opens a full-screen expanded `SearchBar`; back closes it. Whitespace-only query shows the hint (Flutter searched for `""` and listed 15 random words) | `search/presentation/search_page.dart`, `app_config.dart` | ✅ search PR |
| 1.2 | Prefix search on `lword`, then on `lwordMask` (alternative forms), de-duplicated | `search/data/repository/query_repository_impl.dart` | ✅ search PR |
| 1.3 | Letter substitutions: и→і, е→ё, щ→ў, ъ and ' → ‘ (query runs with and without) | `search/data/repository/objectbox_search_repository.dart` | ✅ search PR |
| 1.4 | Fuzzy search (Damerau–Levenshtein, distance 1 for ≤5 letters, else 2) when nothing matched and query ≥3 letters. Native skips words whose length differs by more than the distance (same results, less work) | `search/data/util/` | ✅ search PR |
| 1.5 | Retry without a glued one-letter preposition/conjunction (e.g. «всмысле» → «смысле») | `objectbox_search_repository.dart` | ✅ search PR |
| 1.6 | Extra letter buttons (и щ ъ · і ў ') while the keyboard is up; insert at the cursor. M3: a row of tonal buttons docked above the keyboard instead of floating action buttons | `search/presentation/widgets/letter_button.dart`, `search_extra_buttons.dart` | ✅ search PR |
| 1.7 | Hint «Пошук з аўтаматычнай падменай…» on empty query; «Па запыце нічога не знойдзена» on no results. New: «Не атрымалася выканаць пошук.» on a store error (Flutter showed nothing) | `search/presentation/widgets/search_list_view.dart` | ✅ search PR |
| 1.8 | Result row: word + dictionary name; tap opens translation and saves to history. Native: names in sentence case («Беларуска-рускі»). Saving to history is done by the translation screen once the translation loads, as in Flutter (`save_to_history`) | `search_list_view.dart` | ✅ search PR, translation PR |
| 1.9 | Analytics: `search_performed` / `search_no_results` once results stayed 500 ms (`query`, `result_count`, `used_preposition_fallback`), `search_result_tapped` (word params: `word_id`, `lang_id`, `word`, `dict_name` = Flutter's enum name `belRus`/`rusBel`/`tsbm`, `dict_path`; `position` from 0, `query`) | `search/data/repository/firebase_analytics_search_repository.dart` | ✅ analytics PR |

## 2. Translation

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 2.1 | Source chain: local store (offline dictionaries) → admin API → Supabase `main_word`. A malformed API entry also falls through to Supabase. The word is looked up in the search index first, as in Flutter | `translation/domain/use_case/get_translation.dart` | ✅ translation PR |
| ~~2.2~~ | ~~skarnik.by page scrape as the last fallback~~ (dropped by Aleh, 2026-10-30) | `translation/data/repository/skarnik_translation_repository.dart` | ➖ |
| 2.3 | Redirect words (`redirect_to`): open the target word with «Перанакіравана з «…»». M3: the target replaces the alias screen (back skips the empty alias) and the notice is a snackbar on the target; a word redirecting to itself shows the error instead of looping | `translation_page.dart`, `core/exceptions.dart` | ✅ translation PR |
| 2.4 | HTTP cache for fetched translations, 24 h (Flutter's Remote Config default; Remote Config itself comes with 8.8). Native caches the Supabase fallback too. The translation store holds only offline downloads | `objectbox_translation_repository.dart`, `data/http/skarnik_dio.dart` | ✅ translation PR (lifetime from Remote Config with 8.8) |
| 2.5 | Render translation HTML (colours, bold/italic, sizes, links to other words), selectable. Colours map to M3 roles instead of Flutter's fixed iOS colours; unknown colours render as plain text ([ADR 0003](adr/0003-translation-network-and-html.md)) | `translation/presentation/widgets/translation_html.dart` | ✅ translation PR |
| 2.6 | Tap a linked word inside a translation to open it. New: links to other sites open in the browser (Flutter ignored them) | `translation_html.dart` | ✅ translation PR |
| 2.7 | Bookmark toggle with snackbar «Дададзена ў закладкі.» / «Выдалена з закладак.» | `widgets/action_favorites.dart` | ✅ translation PR |
| 2.8 | Share: link `skarnik.app/r/<dict>/<id>` with the word as subject (Android share sheet) | `widgets/action_share.dart`, `translation_cubit.dart` | ✅ translation PR |
| 2.9 | Error state «Прабачце, адбылася памылка перакладу слова.» M3: shown in the screen with a «Паўтарыць» button instead of a 15 s snackbar over an endless spinner | `translation_page.dart` | ✅ translation PR |
| 2.10 | Analytics: `translation` (`uri` = admin API address, word params, `source` local/api/cloud), `add_to_favorites` (adding only), `share` (`content_type` text/plain, `item_id` = link, `method` system) | `firebase_analytics_translation_repository.dart` | ✅ analytics PR |
| 2.11 | Open by id (deep link / widget) and open from a word (search, history, bookmarks). The route takes `langId` + `wordId` (+ the word to show while loading, and `saveToHistory`, false from history/bookmarks as in Flutter) | `app/presentation/skarnik_router.dart` | 🟨 route done in translation PR, used by history/bookmarks and the widget (`WordLinkIntents`); deep links with 9.1 |

## 3. History and bookmarks

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 3.1 | History tab (start screen): words opened, newest first, paged by 25. Native: the list observes the history store, so a word opened from search shows on return (Flutter reloaded on an app event). Tap opens the word without re-saving it | `history/`, `home/domain/use_case/load_history.dart` | ✅ history/bookmarks PR |
| 3.2 | Bookmarks tab: paged list, tap to open. Native: the list observes the history store (ObjectBox query as a Flow), so bookmarks added or removed elsewhere show at once. Flutter bug, not ported: the kept-alive tab never reloads after a bookmark changes on the translation screen | `favorites/` | ✅ history/bookmarks PR |
| 3.3 | Bookmarks sort: by date added / alphabetical, remembered (`favorites_sort_order`, migrated from Flutter with the same values). M3: sort icon opens a dropdown menu with a check on the current order | `favorites/data/repository/shared_preferences_favorites_sort_repository.dart` | ✅ history/bookmarks PR |
| 3.4 | Remove a bookmark with «Вярнуць» undo in the snackbar. Native: swipe from the end (as in Flutter) or the TalkBack action «Выдаліць з закладак»; the bookmark is removed at once and undo puts it back with its old id, so it keeps its place (Flutter delayed the delete by 4 s and lost it if the app closed) | `favorites/presentation/favorites_page.dart` | ✅ history/bookmarks PR |
| 3.5 | Bottom navigation: History («Пошук»), Bookmarks, Dictionary; each tab keeps its own state. Native: tabs keep scroll position and data while hidden; back on Bookmarks or Dictionary returns to History first (M3). The navigation rail on wide screens comes with 0.5 | `home/presentation/home_shell.dart` | ✅ history/bookmarks PR, Dictionary in dictionary PR (rail with 0.5) |

## 4. Dictionary (vocabulary)

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 4.1 | Tabs: Тлумачальны (first), Бел-Рус, Рус-Бел, each the whole dictionary from the search store; a dictionary loads when its tab is first shown and is kept while the home screen lives. M3: top app bar «Слоўнік» over a primary tab row; each dictionary keeps its own scroll position. New: «Паўтарыць» when the store cannot be read (Flutter showed an endless spinner) | `vocabulary/presentation/vocabulary_page.dart`, `vocabulary_cubit.dart` | ✅ dictionary PR |
| 4.2 | Alphabetical list with a letter index bar to jump; tap opens the word and saves it to history. Native: words are sorted in Belarusian/Russian alphabet order and grouped by the stored `letter` under sticky letter headers, as in the iOS app (Flutter listed them in store order). The index shows the letters that have words (Flutter showed a fixed list, and its first letter never jumped); dragging shows the letter in a bubble with a light haptic tick (Flutter had none on Android); TalkBack gets a «Перайсці да літары» action per letter | `vocabulary/presentation/widgets/alphabet_list_view.dart` | ✅ dictionary PR |
| 4.3 | Analytics: `vocabulary` (word params) when a word is opened from the Dictionary tab | `vocabulary/data/repository/firebase_analytics_vocabulary_repository.dart` | ✅ analytics PR |

## 5. Offline dictionaries

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 5.1 | Settings section «Афлайн доступ»: per dictionary download / progress «N / M слоў» / downloaded count. M3: Settings open from a settings button in the trailing slot of the start screen's search bar; rows are `ListItem`s under list subheaders («Афлайн доступ», «Выгляд», «Іншае»), counts use Belarusian plurals | `settings/presentation/widgets/offline_dictionaries_section.dart` | ✅ offline PR |
| 5.2 | Paged download from Supabase, resumable via cursor (`download_cursor_<dict>`, migrated from Flutter), stale-cursor reset, 5 attempts per request. Native: the download is a WorkManager job ([ADR 0004](adr/0004-offline-downloads-in-workmanager.md)), so it survives leaving Settings and the process being killed, and resumes from the cursor (Flutter's download died with the app). An unfinished download shows the error row and resumes on tap | `translation/domain/use_case/download_dictionary.dart` | ✅ offline PR |
| 5.3 | Rate limit: max 4 download starts per 5 minutes (`download_recent_attempts_at_ms`; not migrated, it only matters for 5 minutes) | `check_download_rate_limit.dart` | ✅ offline PR |
| 5.4 | Delete an offline copy with confirmation. M3: the dialog's buttons name the action («Скасаваць» / «Выдаліць» instead of «Не» / «Так»); «Афлайн-копія слоўніка выдалена.» snackbar after it | `clear_downloaded_dictionary.dart` | ✅ offline PR |
| 5.5 | Promo badge on the settings button until settings are opened (`offline_dictionaries_promo_seen`, migrated from Flutter) | `settings/presentation/offline_dictionaries_promo_cubit.dart` | ✅ offline PR |
| 5.6 | Analytics: `offline_dictionary_download_click` (`dict_name`, `dict_path`) on a tap that may start a download | `firebase_analytics_settings_repository.dart` | ✅ analytics PR |

## 6. Stress (націск)

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 6.1 | «На́ціск» button on a translation; picker when the entry has several words. Candidates: the ТСБМ headword unless it is a phrase, else the single words of the entry's `#831b03` runs, sorted. M3: an assist chip under the headword (a dropdown menu for several candidates) instead of a text button in the top app bar | `translation/presentation/widgets/action_stress.dart`, `translation/domain/entity/translation.dart` | ✅ stress PR |
| 6.2 | Resolve word list and stress table: starnik.by (`/api/wordList`, `/pravapis/<id>` page) first, skarnik_admin `/api/stress_words/` when starnik.by fails or has no exact match; never merged, a table only from the entry's own source (`shared/stress_spec.md`). Responses go through the shared 24 h HTTP cache. (The parity row said Supabase; Flutter moved the fallback to skarnik_admin) | `stress/` | ✅ stress PR |
| 6.3 | Stress table screen with HTML cells and part-of-speech names; one match opens its table directly (the lookup screen is replaced, so back returns to the translation), homographs are listed first. M3: two columns with dividers instead of a bordered table; `.pytanne` muted and `.skarot` in the label colour role instead of fixed red; the stressed word and part of speech head the table. Not found and errors stay on the screen (errors with «Паўтарыць»); Flutter closed the screen with a snackbar when nothing was found and had no retry | `stress/presentation/stress_page.dart`, `stress_table_page.dart`, `strings.dart` | ✅ stress PR |
| 6.4 | Analytics: `stress_clicked` (`word`) when the «На́ціск» screen opens; not again on retry | `stress/data/repository/firebase_analytics_stress_repository.dart` | ✅ analytics PR |

## 7. Word-of-the-day widget

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 7.1 | Glance widget «Слова дня»: random Бел-Рус word with its translation as plain text. Same provider class (`by.mazokaleh.skarnik.widget.WordOfDayWidgetReceiver`), so widgets placed by the Flutter app stay on the home screen; the word they showed (home_widget's `HomeWidgetPreferences`) is kept. M3: Material You colours on Android 12+ (brand palette below), light/dark with the system, system corner radius, a «Слова дня» label above the word, and the word in its own case instead of upper case; the translation gets as many lines as the widget's height allows | `widget/`, `android/app/src/main/kotlin/.../widget/` | ✅ widget PR |
| 7.2 | Refresh hourly and once when added; skip words shown in the last 30 days (`widget_word_history_v1`, migrated); reject near-identical word/translation (same normalisation, Levenshtein and 1.8 threshold); 30 attempts. Native: the hourly WorkManager job runs only while a widget is on the home screen (Flutter scheduled it for everyone at every app start and refreshed on every launch); a refresh that finds no word (no network) keeps the current word instead of switching to the fallback «халэмус»; history entries that no longer affect a pick are pruned. Flutter's leftover workmanager jobs are cancelled | `widget/domain/use_case/refresh_widget_data.dart`, `main.dart` | ✅ widget PR |
| 7.3 | Tap opens the word in the app (over the start screen, saved to history); the fallback word just opens the app | `widget/` | ✅ widget PR |

## 8. Settings, consent, review

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 8.1 | Settings: clear search history with confirmation; bookmarks stay. M3: the dialog title asks the question and the buttons name the action («Скасаваць» / «Ачысціць» instead of «Увага» + «Не» / «Так»); the row is disabled while history is empty; «Гісторыя паспяхова ачышчана.» snackbar after it. The History tab follows the store, so it empties at once | `settings/presentation/settings_page.dart` | ✅ settings PR |
| 8.2 | Settings: «Аналітыка выкарыстання» switch in «Іншае», the same answer as the dialog (switching counts as an answer). Subtitle typo «зборы» fixed to «збор» | `settings_page.dart` | ✅ analytics PR |
| 8.3 | Settings: «Напісаць распрацоўшчыкам»: email with «Skarnik 4.0.0 (123), Android» as subject (Flutter's format). Native: the body starts with the device and Android version. Addresses as in the iOS app (to belanghelp@gmail.com, bcc skarnikapp@gmail.com; Flutter's came from a build secret), in `gradle.properties`. A snackbar says so when there is no email app | `settings_cubit.dart` | ✅ settings PR |
| 8.4 | Settings: «Ацаніць праграму» opens the store listing (`market://`, so Play or AppGallery), else the Play web page | `settings_cubit.dart` | ✅ settings PR |
| 8.5 | «Пра Скарнік» bottom sheet (opens fully expanded): about text, links to starnik.by and drukarnik.app, email button, version. Native: «Працуе ў рэжыме анлайн.» is left out, since dictionaries can be downloaded for offline use | `settings/presentation/widgets/about_bottom_sheet.dart` | ✅ settings PR |
| 8.6 | Analytics consent dialog on first launch, over any screen, not dismissible by back or outside tap; nothing sent before an answer (collection off in the manifest). Keys `analytics_consent_answered`, `analytics_consent_granted` migrated from Flutter, so people who answered are not asked again. Native: no advertising id; debug builds never send ([ADR 0007](adr/0007-analytics-and-consent.md)) | `analytics_consent/` | ✅ analytics PR |
| 8.7 | In-app review after 10 translation views, asked once; AppGallery fallback dialog on Huawei (`review_translation_view_count`, `review_already_requested`) | `review/` | ⬜ |
| 8.8 | Remote Config (HTTP cache lifetime) | `app/domain/use_case/init_remote_config.dart` | ⬜ |
| 8.9 | Analytics: `app_open` when the activity starts fresh (not on rotation) | `app/data/repository/firebase_analytics_app_repository.dart` | ✅ analytics PR |

## 9. Deep links

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 9.1 | Open `skarnik.by/<dict>/<id>` and `skarnik.app/r/<dict>/<id>` (http/https, optional www) in the app | `app/domain/use_case/handle_app_link.dart`, `AndroidManifest.xml` | ⬜ |
| 9.2 | Verified App Links (`autoVerify`) | `AndroidManifest.xml` | ⬜ |

## 10. Release

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 10.1 | Signed release build with the existing upload key; Play, plus Huawei AppGallery | `android/fastlane/` | ⬜ |
| 10.1a | New: every merge to `main` goes to Firebase App Distribution (group `internal`), signed with the upload key ([ADR 0005](adr/0005-firebase-app-distribution.md)) | `android/fastlane/Fastfile` (`firebase` lane) | 🟨 workflow PR; needs repository secrets |
| 10.2 | Upgrade test: Flutter 3.6.0 with history, bookmarks and an offline dictionary → native build keeps all of it | — | ⬜ |
