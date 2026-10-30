package by.mazokaleh.skarnik.core.database

import android.content.Context
import by.mazokaleh.skarnik.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Entry point to local storage. Stores are opened lazily, once, off the main thread. */
interface SkarnikDatabase {
    suspend fun stores(): ObjectBoxStores
}

@Singleton
class ObjectBoxSkarnikDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SkarnikDatabase {
    private val mutex = Mutex()

    @Volatile
    private var stores: ObjectBoxStores? = null

    override suspend fun stores(): ObjectBoxStores = stores ?: mutex.withLock {
        stores ?: withContext(ioDispatcher) {
            ObjectBoxStoreOpener(
                documentsDir = context.getDir(FLUTTER_DOCUMENTS_DIR, Context.MODE_PRIVATE),
                openSeed = { context.assets.open(ObjectBoxStoreOpener.SEED_ASSET) }
            ).open()
        }.also { stores = it }
    }

    private companion object {
        /** Becomes `app_flutter`: where Flutter's path_provider put the documents directory. */
        const val FLUTTER_DOCUMENTS_DIR = "flutter"
    }
}
