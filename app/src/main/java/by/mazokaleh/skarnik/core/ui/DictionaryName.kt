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
