# Store screenshots

Flows that take the Google Play screenshots of the native app (issue #58). They are not tests: CI does not run
this folder, and `maestro test .maestro/` does not pick it up.

```bash
./gradlew installDebug
scripts/store-screenshots.sh            # → build/store-screenshots/<device>-<size>/*.png
```

The script needs exactly one booted emulator or device (Google APIs image with Gboard) and runs:

1. `setup/belarusian-keyboard.yaml`: makes Gboard's Belarusian layout its only language, so the search shot shows
   «і ў» keys and no language key. It removes English (US) from Gboard on that emulator. Gboard adds languages only
   from its own settings, so the flow taps through them by their English texts; an emulator in another system
   language needs this done by hand.
2. Demo mode for the status bar: 10:00, full battery and Wi-Fi, no notification icons.
3. `fill.yaml`: a clean install; opens the words of the App Store shots (skarnik_ios
   `AppStoreScreenshotsUITests.swift`, word ids in issue #58) by their share links, oldest first, and bookmarks
   eleven of them.
4. `shots.yaml` four times. The app stays on «Як у сістэме» and the script switches the system night mode, so
   the keyboard and system bars match the app:

| Prefix | Look |
|---|---|
| `you-light`, `you-dark` | Material You (colours from the wallpaper) |
| `brand-light`, `brand-dark` | Brand palette (white or `#0F0F10` with the Skarnik red), after `brand-palette.yaml` turns «Колеры шпалер» off |

Screens: `1-history`, `2-search` («мов» with the keyboard and the letter row), `3-translation` («удзячнасць»,
Тлумачальны), `4-translation-stress-button` («пара»: «па́ра | пара́» and «На́ціск»), `5-stress` («стракаты»),
`6-bookmarks`, `7-dictionary` (Тлумачальны in «К», reached with the letter index), `8-settings`.

Later shots open words from History, which does not reorder it, so every pass shows the same lists. A full run
takes about ten minutes; translations need the network once, then come from the HTTP cache.

Devices:
- Phone: the `Pixel_6_API_36` AVD (1080×2400).
- Tablet: the `Pixel_Tablet` AVD in landscape (2560×1600, its natural orientation). From 840 dp words open
  beside the list, so the translation and stress shots show the list–detail layout with the row highlighted; the
  other screens show «Выберыце слова са спіса» in the empty pane.

An emulator that had an older build installed shows «Што новага» after `installDebug`; `fill.yaml` closes it.

Not taken by the flow: the home screen widget («Слова дня» is random, so take it by hand when a good word comes
up).
