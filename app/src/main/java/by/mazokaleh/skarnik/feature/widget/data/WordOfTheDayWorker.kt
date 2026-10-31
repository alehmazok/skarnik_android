package by.mazokaleh.skarnik.feature.widget.data

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.widget.domain.RefreshWordOfTheDayUseCase
import by.mazokaleh.skarnik.feature.widget.domain.WordOfTheDayStore
import by.mazokaleh.skarnik.feature.widget.ui.WordOfTheDayWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Picks a new word of the day and redraws the widgets. Runs hourly while a widget is on the home screen. */
@HiltWorker
class WordOfTheDayWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refresh: RefreshWordOfTheDayUseCase,
    private val store: WordOfTheDayStore
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val widgets = GlanceAppWidgetManager(applicationContext).getGlanceIds(WordOfTheDayWidget::class.java)
        if (widgets.isEmpty()) {
            // The last widget went away without onDisabled (e.g. a restored backup): stop refreshing.
            WordOfTheDayWork.cancel(applicationContext)
            return Result.success()
        }
        suspendRunCatching { refresh() }
            .onFailure { Log.e(TAG, "Cannot refresh the word of the day", it) }
            .getOrNull()
            ?.let { word ->
                store.save(word)
                WordOfTheDayWidget().updateAll(applicationContext)
            }
        // A failed refresh keeps the current word; the next hourly run tries again.
        return Result.success()
    }

    private companion object {
        const val TAG = "WordOfTheDay"
    }
}

/** The widget's hourly refresh, as one unique periodic job. */
object WordOfTheDayWork {
    private const val NAME = "word_of_the_day"

    /** Flutter's workmanager jobs. Their worker class is gone, so they would fail every hour forever. */
    private val FLUTTER_JOBS = listOf("word-of-day-refresh", "word-of-day-refresh-initial")

    /**
     * Starts the hourly refresh if it is not running. A new periodic job runs at once, so a freshly added widget
     * gets a word right away (Flutter needed a separate one-off job for that).
     */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WordOfTheDayWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }

    fun cancelFlutterJobs(context: Context) {
        val workManager = WorkManager.getInstance(context)
        FLUTTER_JOBS.forEach { workManager.cancelUniqueWork(it) }
    }
}
