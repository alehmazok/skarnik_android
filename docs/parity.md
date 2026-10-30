# Feature parity register

Every user-facing behaviour of skarnik-flutter 3.6.0, built from its Dart code, and its status in the native app.

**For Aleh:** strike out (`~~like this~~`) or delete any row we should not port, and add rows for things the
Flutter app doesn't have. Everything not struck out is ported. Product questions are marked ❓.

Status: ⬜ not started · 🟨 in progress · ✅ done (PR link) · ➖ dropped

Paths are relative to `skarnik-flutter/lib/features/`.

## 0. Foundation

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 0.1 | Same app id `by.mazokaleh.skarnik`; installs as an update | `android/app/build.gradle` | ✅ skeleton PR |
| 0.2 | Open existing ObjectBox stores (search, history, translation) at `app_flutter/objectbox_*`; seed the search index from the bundled asset on first launch | `app/data/repository/objectbox_database_repository.dart` | ✅ skeleton PR (emulator upgrade test pending) |
| 0.3 | Migrate Flutter `shared_preferences` keys into DataStore (each key with its feature, listed below) | various `shared_preferences_*_repository.dart` | ⬜ |
| 0.4 | Light/dark theme following the system | `app/presentation/skarnik_app.dart` | ✅ skeleton PR |
| 0.4a | New: System/Light/Dark setting; Material You dynamic colour with a switch to the brand palette (agreed 2026-10-30) | — | 🟨 theme done, settings toggles with the settings PR |
| 0.5 | Adaptive layout: list + translation side by side on wide screens | `widgets/master_detail_view.dart`, `widgets/breakpoints.dart` | ⬜ |
| 0.6 | Crash reporting (Crashlytics), release-only | `main.dart` | ⬜ |

## 1. Search

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 1.1 | Search screen opened from the History tab's search field; results across all three dictionaries, max 15 | `search/presentation/search_page.dart`, `app_config.dart` | ⬜ |
| 1.2 | Prefix search on `lword`, then on `lwordMask` (alternative forms), de-duplicated | `search/data/repository/query_repository_impl.dart` | ⬜ |
| 1.3 | Letter substitutions: и→і, е→ё, щ→ў, ъ and ' → ‘ (query runs with and without) | `search/data/repository/objectbox_search_repository.dart` | ⬜ |
| 1.4 | Fuzzy search (Damerau–Levenshtein, distance 1 for ≤5 letters, else 2) when nothing matched and query ≥3 letters | `search/data/util/` | ⬜ |
| 1.5 | Retry without a glued one-letter preposition/conjunction (e.g. «всмысле» → «смысле») | `objectbox_search_repository.dart` | ⬜ |
| 1.6 | Extra letter buttons (і, ў, ‘, ё …) above the keyboard | `search/presentation/widgets/letter_button.dart`, `search_extra_buttons.dart` | ⬜ |
| 1.7 | Hint «Пошук з аўтаматычнай падменай…» on empty query; «Па запыце нічога не знойдзена» on no results | `search/presentation/widgets/search_list_view.dart` | ⬜ |
| 1.8 | Result row: word + dictionary name; tap opens translation and saves to history | `search_list_view.dart` | ⬜ |
| 1.9 | Analytics: `search_performed`, `search_no_results`, `search_result_tapped` | `search/data/repository/firebase_analytics_search_repository.dart` | ⬜ |

## 2. Translation

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 2.1 | Source chain: local store → admin API → Supabase `main_word` → skarnik.by page scrape | `translation/domain/use_case/get_translation.dart` | ⬜ |
| 2.2 | ❓ Keep scraping skarnik.by as the last fallback? | `translation/data/repository/skarnik_translation_repository.dart` | ⬜ |
| 2.3 | Redirect words (`redirect_to`): open the target word with «Перанакіравана з «…»» | `translation_page.dart`, `core/exceptions.dart` | ⬜ |
| 2.4 | Cache fetched translations in the translation store; HTTP cache with lifetime from Remote Config | `objectbox_translation_repository.dart`, `data/http/skarnik_dio.dart` | ⬜ |
| 2.5 | Render translation HTML (colours, bold/italic, links to other words) | `translation/presentation/widgets/translation_html.dart` | ⬜ |
| 2.6 | Tap a linked word inside a translation to open it | `translation_html.dart` | ⬜ |
| 2.7 | Bookmark toggle with snackbar «Дададзена ў закладкі» / «Выдалена з закладак» | `widgets/action_favorites.dart` | ⬜ |
| 2.8 | Share: link `skarnik.app/r/<dict>/<id>` with the word as subject | `widgets/action_share.dart`, `translation_cubit.dart` | ⬜ |
| 2.9 | Error state «Прабачце, адбылася памылка перакладу слова.» | `translation_page.dart` | ⬜ |
| 2.10 | Analytics: `translation`, `add_to_favorites`, share event | `firebase_analytics_translation_repository.dart` | ⬜ |
| 2.11 | Open by id (deep link / widget) and open from a word (search, history, bookmarks) | `app/presentation/skarnik_router.dart` | ⬜ |

## 3. History and bookmarks

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 3.1 | History tab (start screen): words opened, newest first, paged by 25 | `history/`, `home/domain/use_case/load_history.dart` | ⬜ |
| 3.2 | Bookmarks tab: paged list, tap to open | `favorites/` | ⬜ |
| 3.3 | Bookmarks sort: by date added / alphabetical, remembered (`favorites_sort_order`) | `favorites/data/repository/shared_preferences_favorites_sort_repository.dart` | ⬜ |
| 3.4 | Remove a bookmark with «Вярнуць» undo in the snackbar | `favorites/presentation/favorites_page.dart` | ⬜ |
| 3.5 | Bottom navigation: History, Bookmarks, Dictionary; each tab keeps its own stack | `home/presentation/home_shell.dart` | ⬜ |

## 4. Dictionary (vocabulary)

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 4.1 | Tabs: Тлумачальны, Бел-Рус, Рус-Бел | `vocabulary/presentation/vocabulary_page.dart` | ⬜ |
| 4.2 | Alphabetical list with a letter index bar to jump | `vocabulary/presentation/widgets/alphabet_list_view.dart` | ⬜ |
| 4.3 | Analytics: `vocabulary` | `vocabulary/data/repository/firebase_analytics_vocabulary_repository.dart` | ⬜ |

## 5. Offline dictionaries

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 5.1 | Settings section «Афлайн доступ»: per dictionary download / progress «N / M слоў» / downloaded count | `settings/presentation/widgets/offline_dictionaries_section.dart` | ⬜ |
| 5.2 | Paged download from Supabase, resumable via cursor (`download_cursor_<dict>`), stale-cursor reset | `translation/domain/use_case/download_dictionary.dart` | ⬜ |
| 5.3 | Rate limit: max 4 download starts per 5 minutes (`download_recent_attempts_at_ms`) | `check_download_rate_limit.dart` | ⬜ |
| 5.4 | Delete an offline copy with confirmation | `clear_downloaded_dictionary.dart` | ⬜ |
| 5.5 | Promo badge on the settings button until settings are opened (`offline_dictionaries_promo_seen`) | `settings/presentation/offline_dictionaries_promo_cubit.dart` | ⬜ |
| 5.6 | Analytics: `offline_dictionary_download_click` | `firebase_analytics_settings_repository.dart` | ⬜ |

## 6. Stress (націск)

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 6.1 | «На́ціск» button on a translation; picker when the entry has several words | `translation/presentation/widgets/action_stress.dart` | ⬜ |
| 6.2 | Resolve word list and stress table (admin API / Supabase; starnik.by) | `stress/` | ⬜ |
| 6.3 | Stress table screen with HTML cells and part-of-speech names | `stress/presentation/stress_table_page.dart`, `strings.dart` | ⬜ |
| 6.4 | Analytics: `stress_clicked` | `stress/data/repository/firebase_analytics_stress_repository.dart` | ⬜ |

## 7. Word-of-the-day widget

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 7.1 | Glance widget «Слова дня»: random Бел-Рус word with translation | `widget/`, `android/app/src/main/kotlin/.../widget/` | ⬜ |
| 7.2 | Refresh hourly and once when added; skip words shown in the last 30 days (`widget_word_history_v1`); reject near-identical word/translation; fallback word | `widget/domain/use_case/refresh_widget_data.dart` | ⬜ |
| 7.3 | Tap opens the word in the app | `widget/` | ⬜ |

## 8. Settings, consent, review

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 8.1 | Settings: clear search history with confirmation | `settings/presentation/settings_page.dart` | ⬜ |
| 8.2 | Settings: analytics on/off switch | `settings_page.dart` | ⬜ |
| 8.3 | Settings: «Напісаць распрацоўшчыкам» (email with device info) | `settings_cubit.dart` | ⬜ |
| 8.4 | Settings: «Ацаніць праграму» | `settings_cubit.dart` | ⬜ |
| 8.5 | «Пра Скарнік» bottom sheet: about text, links to starnik.by and drukarnik.app, version | `settings/presentation/widgets/about_bottom_sheet.dart` | ⬜ |
| 8.6 | Analytics consent dialog on first launch; nothing sent before an answer (`analytics_consent_answered`, `analytics_consent_granted`) | `analytics_consent/` | ⬜ |
| 8.7 | In-app review after 10 translation views, asked once; AppGallery fallback dialog on Huawei (`review_translation_view_count`, `review_already_requested`) | `review/` | ⬜ |
| 8.8 | Remote Config (HTTP cache lifetime) | `app/domain/use_case/init_remote_config.dart` | ⬜ |
| 8.9 | Analytics: app open | `app/data/repository/firebase_analytics_app_repository.dart` | ⬜ |

## 9. Deep links

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 9.1 | Open `skarnik.by/<dict>/<id>` and `skarnik.app/r/<dict>/<id>` (http/https, optional www) in the app | `app/domain/use_case/handle_app_link.dart`, `AndroidManifest.xml` | ⬜ |
| 9.2 | Verified App Links (`autoVerify`) | `AndroidManifest.xml` | ⬜ |

## 10. Release

| # | Behaviour | Flutter source | Status |
|---|---|---|---|
| 10.1 | Signed release build with the existing upload key; Play, plus Huawei AppGallery | `android/fastlane/` | ⬜ |
| 10.2 | Upgrade test: Flutter 3.6.0 with history, bookmarks and an offline dictionary → native build keeps all of it | — | ⬜ |
