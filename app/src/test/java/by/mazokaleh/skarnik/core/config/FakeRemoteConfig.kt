package by.mazokaleh.skarnik.core.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class FakeRemoteConfig(
    override var dictionaryLinkPlacement: DictionaryLinkPlacement = DictionaryLinkPlacement.DEFAULT,
    override var httpCacheLifetime: Duration = 24.hours
) : RemoteConfig {
    override suspend fun fetchAndActivate() = Unit
}
