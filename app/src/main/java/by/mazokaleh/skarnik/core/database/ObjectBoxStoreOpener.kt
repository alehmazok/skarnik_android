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
class ObjectBoxStoreOpener(private val documentsDir: File, private val openSeed: () -> InputStream) {
    fun open(): ObjectBoxStores {
        val searchDir = File(documentsDir, SEARCH_DIR)
        installSeedIfMissing(File(searchDir, DATA_FILE))
        return ObjectBoxStores(
            search = openStore(searchDir),
            history = openStore(File(documentsDir, HISTORY_DIR)),
            translation = openStore(File(documentsDir, TRANSLATION_DIR))
        )
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

    private fun openStore(dir: File): BoxStore = MyObjectBox.builder().directory(dir).build()

    companion object {
        const val SEARCH_DIR = "objectbox_search"
        const val HISTORY_DIR = "objectbox_history"
        const val TRANSLATION_DIR = "objectbox_translation"
        const val DATA_FILE = "data.mdb"
        const val SEED_ASSET = "objectbox/data.mdb.gz"
        private const val BUFFER_SIZE = 256 * 1024
    }
}
