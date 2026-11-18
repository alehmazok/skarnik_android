package by.mazokaleh.skarnik.core.config

class FakeRemoteConfig(
    override var dictionaryLinkPlacement: DictionaryLinkPlacement = DictionaryLinkPlacement.DEFAULT
) : RemoteConfig {
    override suspend fun fetchAndActivate() = Unit
}
