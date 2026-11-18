package by.mazokaleh.skarnik.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue

/** A Remote Config value as Firebase returns it, from [source] (remote by default). */
class FakeRemoteConfigValue(
    private val raw: String,
    private val source: Int = FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
) : FirebaseRemoteConfigValue {
    override fun asLong(): Long = raw.toLongOrNull() ?: throw IllegalArgumentException("not a long: $raw")

    override fun asDouble(): Double = raw.toDoubleOrNull() ?: throw IllegalArgumentException("not a double: $raw")

    override fun asString(): String = raw

    override fun asByteArray(): ByteArray = raw.toByteArray()

    override fun asBoolean(): Boolean = raw.toBooleanStrict()

    override fun getSource(): Int = source
}
