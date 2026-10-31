package by.mazokaleh.skarnik

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import by.mazokaleh.skarnik.core.network.FlutterHttpCacheCleaner
import by.mazokaleh.skarnik.feature.widget.data.WordOfTheDayWork
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class SkarnikApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var flutterHttpCacheCleaner: FlutterHttpCacheCleaner

    /** WorkManager starts on first use with this configuration, so workers get their dependencies from Hilt. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Enqueues a database write on WorkManager's own thread; nothing waits for it.
        WordOfTheDayWork.cancelFlutterJobs(this)
        // The cleaner switches to the IO dispatcher; the app scope lives as long as the process.
        MainScope().launch { flutterHttpCacheCleaner() }
    }
}
