package by.mazokaleh.skarnik.core.analytics

/**
 * A screen in `screen_view` reports. [screenName] is the stable name the reports group by (shared with skarnik_ios);
 * [screenClass] is the route class name, written out because R8 renames classes in release builds.
 */
enum class Screen(val screenName: String, val screenClass: String) {
    History("history", "HomeRoute"),
    Favorites("favorites", "HomeRoute"),
    Vocabulary("vocabulary", "HomeRoute"),
    Search("search", "SearchRoute"),
    Translation("translation", "TranslationRoute"),
    Stress("stress", "StressRoute"),
    StressTable("stress_table", "StressTableRoute"),
    Settings("settings", "SettingsRoute")
}
