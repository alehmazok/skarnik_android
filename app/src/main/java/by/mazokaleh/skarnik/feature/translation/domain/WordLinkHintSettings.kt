package by.mazokaleh.skarnik.feature.translation.domain

/** Whether the one-time hint about tappable words in translations has been shown. */
interface WordLinkHintSettings {
    suspend fun isHintShown(): Boolean

    suspend fun markHintShown()
}
