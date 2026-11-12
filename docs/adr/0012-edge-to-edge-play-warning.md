# 0012. Edge-to-edge: keep androidx `enableEdgeToEdge()` despite Play's deprecated-API warning

**Status:** Accepted

## Context

Android vitals for 4.0.0 (167) lists two recommended actions:

- «Your app uses deprecated APIs or parameters for edge-to-edge»: `Window.setStatusBarColor`,
  `Window.setNavigationBarColor` and `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`, starting in `us0.b`, `ws0.b`,
  `ys0.b` and `s2.o`.
- «Edge-to-edge may not display for all users», which asks apps that target SDK 35 to handle insets or to call
  `enableEdgeToEdge()`.

The R8 mapping of the same code resolves those classes to `androidx.activity.EdgeToEdgeApi26`, `EdgeToEdgeApi29`,
`EdgeToEdgeApi35` and an R8 API outline of `EdgeToEdgeApi28`/`Api30`'s cutout mode. A scan of the release dex finds
no other caller. The app's own code (`MainActivity`) only calls `enableEdgeToEdge()`. activity 1.13.0 (latest
stable) and 1.14.0-alpha03 both still make these calls: on API 35+ they are no-ops, on older versions they are how
the bars become transparent.

Alternatives:

1. Replace `enableEdgeToEdge()` with our own setup: transparent bars and cutout mode from theme attributes,
   `WindowCompat.setDecorFitsSystemWindows(window, false)`, bar icon colours via `WindowInsetsControllerCompat`, and
   a Compose scrim behind the 3-button navigation bar on API 24–28.
2. Keep `enableEdgeToEdge()` and accept the warning.

## Decision

Keep `enableEdgeToEdge()` (option 2). The warning is a recommendation: it does not block releases, it does not
affect the core vitals that change discoverability, and the code it points at is the fix the second warning asks
for. Option 1 would re-implement a Google library that handles many API levels for us. That is new regression risk
on API 24–34, the versions we cannot see in Play's pre-launch report, and the theme attributes it needs
(`android:statusBarColor`, `android:navigationBarColor`) are deprecated too, so the warning might not even clear.

## Consequences

- The two recommended actions stay in Android vitals. They are known and need no action.
- Revisit when androidx.activity stops calling the deprecated setters, or if Play turns the warning into a
  requirement.
