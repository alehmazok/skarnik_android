package by.mazokaleh.skarnik.core.analytics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Records what would go to Firebase. */
class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()
    val collectionEnabled = mutableListOf<Boolean>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled += enabled
    }
}

class FakeAnalyticsConsent(initial: ConsentState = ConsentState(answered = false, granted = false)) :
    AnalyticsConsent {
    val stored = MutableStateFlow(initial)
    override val state: Flow<ConsentState> = stored

    override suspend fun setGranted(granted: Boolean) {
        stored.value = ConsentState(answered = true, granted = granted)
    }

    override suspend fun applyStored() = Unit
}
