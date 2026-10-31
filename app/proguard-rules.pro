# ObjectBox, Hilt, Compose and kotlinx.serialization ship their own consumer R8 rules.
# Add app-specific keep rules here only with a comment saying why.

# Navigation's type-safe routes look up an enum argument's class by its original name (Class.forName),
# so a renamed enum crashes the app on launch (StressTableRoute.source, 2026-10-31). Keeping the names of
# the app's few enums is cheap and covers future route arguments too.
-keepnames enum by.mazokaleh.skarnik.** { *; }
