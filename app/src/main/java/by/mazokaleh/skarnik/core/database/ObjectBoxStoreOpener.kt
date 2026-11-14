package by.mazokaleh.skarnik.core.database

import by.mazokaleh.skarnik.core.database.entity.MyObjectBox
import io.objectbox.BoxStore
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Opens the stores at the paths the Flutter app used, so an upgrade keeps history, bookmarks and
 * downloaded dictionaries.
 *
 * Flutter's `getApplicationDocumentsDirectory()` is `Context.getDir("flutter")`, i.e.
 * `/data/data/<package>/app_flutter`; pass that as [documentsDir].
 *
 * Blocking: call off the main thread.
 */
class ObjectBoxStoreOpener(
    private val documentsDir: File,
    private val openStore: (File) -> BoxStore = { dir -> MyObjectBox.builder().directory(dir).build() },
    private val openSeed: () -> InputStream
) {
    /**
     * Opens all three stores or none: if one fails, the ones already opened are closed before the error is
     * rethrown. A leaked store would otherwise block the next attempt on the same directory: ObjectBox then
     * forces finalization while holding its open-files lock, the leaked store's `finalize()` waits for that
     * lock, and Android kills the app after 60 s (Sentry SKARNIK_ANDROID-4).
     */
    fun open(): ObjectBoxStores {
        val searchDir = File(documentsDir, SEARCH_DIR)
        installSeedIfMissing(File(searchDir, DATA_FILE))
        val search = openStore(searchDir)
        return search.closeIfThrows {
            val history = openStore(File(documentsDir, HISTORY_DIR))
            history.closeIfThrows {
                ObjectBoxStores(search, history, translation = openStore(File(documentsDir, TRANSLATION_DIR)))
            }
        }
    }

    private inline fun <T> BoxStore.closeIfThrows(block: () -> T): T {
        var succeeded = false
        try {
            return block().also { succeeded = true }
        } finally {
            if (!succeeded) close()
        }
    }

    /**
     * Copies the bundled search index on first launch. Unlike Flutter, writes to a temp file and renames
     * it, so a process killed mid-copy cannot leave a truncated store behind.
     */
    private fun installSeedIfMissing(target: File) {
        if (target.exists()) return
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "$DATA_FILE.tmp")
        GZIPInputStream(openSeed()).use { input ->
            tmp.outputStream().use { output -> input.copyTo(output, BUFFER_SIZE) }
        }
        check(tmp.renameTo(target)) { "Cannot move seed database to $target" }
    }

    companion object {
        const val SEARCH_DIR = "objectbox_search"
        const val HISTORY_DIR = "objectbox_history"
        const val TRANSLATION_DIR = "objectbox_translation"
        const val DATA_FILE = "data.mdb"

        /**
         * Gzip-compressed search index. Deliberately not `.gz`: AAPT silently un-gzips `*.gz` assets and drops
         * the extension, so the file would be missing under its name at runtime.
         */
        const val SEED_ASSET = "objectbox/search_seed.mdb.gzip"

        private const val BUFFER_SIZE = 256 * 1024
    }
}
