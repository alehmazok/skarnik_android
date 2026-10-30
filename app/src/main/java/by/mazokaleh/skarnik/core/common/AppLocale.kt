package by.mazokaleh.skarnik.core.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The app's only UI language. Every string is Belarusian, whatever the device locale is. */
val APP_LOCALE: Locale = Locale.forLanguageTag("be")

/**
 * Pins resources to [APP_LOCALE]. Without it, plurals follow the device's plural rules: on an English device
 * 316235 picks `other` ("слова ў") instead of Belarusian `many` ("слоў у").
 */
fun Context.withAppLocale(): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(APP_LOCALE)
    return createConfigurationContext(config)
}
