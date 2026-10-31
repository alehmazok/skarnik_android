package by.mazokaleh.skarnik.navigation

import android.content.Context
import android.content.Intent
import by.mazokaleh.skarnik.MainActivity
import by.mazokaleh.skarnik.core.model.WordLink

/** Intents that open a word in [MainActivity]: from the widget now, from app links later (parity 9.1). */
object WordLinkIntents {
    /** A word link (`https://skarnik.app/r/<dict>/<id>`) as a string extra. */
    const val EXTRA_WORD_LINK = "word_link"

    /** Opens [link] in the app, or just the app when [link] is null. */
    fun open(context: Context, link: String?): Intent = Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .apply { if (link != null) putExtra(EXTRA_WORD_LINK, link) }

    /** The word an intent asks to open. Ignores intents replayed from Recents, which were handled already. */
    fun wordLink(intent: Intent): WordLink? {
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        val link = intent.getStringExtra(EXTRA_WORD_LINK) ?: intent.dataString
        return link?.let(WordLink::parse)
    }
}
