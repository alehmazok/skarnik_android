package by.mazokaleh.skarnik.core.ui

import androidx.annotation.StringRes
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.model.Dictionary

/** The dictionary's name as shown under a word in lists. */
@get:StringRes
val Dictionary.nameRes: Int
    get() = when (this) {
        Dictionary.BelRus -> R.string.dictionary_bel_rus
        Dictionary.RusBel -> R.string.dictionary_rus_bel
        Dictionary.Tsbm -> R.string.dictionary_tsbm
    }

/** The dictionary's short name, as on the Dictionary tab and in Settings. */
@get:StringRes
val Dictionary.shortNameRes: Int
    get() = when (this) {
        Dictionary.Tsbm -> R.string.vocabulary_tab_tsbm
        Dictionary.BelRus -> R.string.vocabulary_tab_bel_rus
        Dictionary.RusBel -> R.string.vocabulary_tab_rus_bel
    }
