# 0009. Wide screens: own list–detail layout with a nested NavHost per list

**Status:** Accepted (2026-11-01)

## Context

Flutter (`widgets/master_detail_view.dart`, `breakpoints.dart`) shows a navigation rail from 600 dp and, from
840 dp, the History, Bookmarks, Dictionary and Search lists at 360 dp with the tapped word beside them. Below
840 dp a tap pushes the translation full screen. In the native app a translation is a full-screen destination with
its own ViewModel, links to other words, redirects and the stress screens.

Choices: the Material 3 adaptive library (`NavigationSuiteScaffold`, `ListDetailPaneScaffold` with its navigator)
or the core M3 components with our own two-pane `Row`; how the word beside the list navigates and where its
ViewModels live.

## Decision

- **Core M3 components, no adaptive library.** `NavigationBar` below 600 dp, `NavigationRail` from 600 dp, two
  panes from 840 dp (`core/ui/WindowWidthClass`, measured with `BoxWithConstraints`, so split screen counts the
  app's own window). The adaptive library's navigator keeps only one detail value, not a back stack of
  translations with their ViewModels, so it would still need the parts below; its versions are not tied to the
  Compose BOM. The layout is small enough to own.
- **A nested `NavHost` per list** (`navigation/TranslationPane.kt`): placeholder → translation → links, redirects
  and stress screens, with the same destinations as full screen. Navigation scopes each translation's ViewModel to
  its entry and clears it on pop, handles back and predictive back inside the pane, and saves the stack. Each tab
  and the search screen have their own pane controller, kept with the tab's saved state.
- **Phones are unchanged.** Below 840 dp a tap still opens the word full screen. A word open beside a list when the
  window narrows (folding, rotating a tablet) takes the list's place until back, as M3's list–detail does,
  instead of being dropped as in Flutter.

## Consequences

- No new dependencies. A pane-specific feature (a draggable divider, a third pane) would mean revisiting the
  adaptive library.
- Translation, stress and their routes do not know about panes, except that the first word in a pane beside its
  list has no back arrow (`TranslationScreen(onBack = null)`).
- CI runs `.maestro/wide/` after resizing the emulator to 1280×800 dp.
