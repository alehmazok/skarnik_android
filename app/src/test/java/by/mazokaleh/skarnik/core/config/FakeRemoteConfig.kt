package by.mazokaleh.skarnik.core.config

class FakeRemoteConfig(
    override var dictionaryLinkPlacement: DictionaryLinkPlacement = DictionaryLinkPlacement.DEFAULT,
    override var minSupportedVersionCode: Long = 0L
) : RemoteConfig {
    override suspend fun fetchAndActivate() = Unit
}
