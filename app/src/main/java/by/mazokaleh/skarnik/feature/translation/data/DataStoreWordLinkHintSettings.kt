package by.mazokaleh.skarnik.feature.translation.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import by.mazokaleh.skarnik.feature.translation.domain.WordLinkHintSettings
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** New in the native app, so nothing to migrate from Flutter. */
class DataStoreWordLinkHintSettings @Inject constructor(
    private val settings: DataStore<Preferences>
) : WordLinkHintSettings {
    override suspend fun isHintShown(): Boolean = settings.data.first()[SHOWN] == true

    override suspend fun markHintShown() {
        settings.edit { it[SHOWN] = true }
    }

    companion object {
        val SHOWN = booleanPreferencesKey("word_link_hint_shown")
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WordLinkHintModule {
    @Binds
    fun bindWordLinkHintSettings(impl: DataStoreWordLinkHintSettings): WordLinkHintSettings
}
