package by.mazokaleh.skarnik.feature.offline.domain

import by.mazokaleh.skarnik.core.model.Dictionary
import javax.inject.Inject

/** Removes a downloaded dictionary and its resume point (Flutter's `ClearDownloadedDictionaryUseCase`). */
class DeleteOfflineDictionaryUseCase @Inject constructor(
    private val store: OfflineDictionaryStore,
    private val settings: OfflineDictionarySettings
) {
    suspend operator fun invoke(dictionary: Dictionary) {
        store.clear(dictionary)
        settings.clearCursor(dictionary)
    }
}
