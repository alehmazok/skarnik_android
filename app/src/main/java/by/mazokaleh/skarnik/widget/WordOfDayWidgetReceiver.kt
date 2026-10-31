package by.mazokaleh.skarnik.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import by.mazokaleh.skarnik.feature.widget.data.WordOfTheDayWork
import by.mazokaleh.skarnik.feature.widget.ui.WordOfTheDayWidget

/**
 * The «Слова дня» widget provider. Package and class name are the Flutter app's
 * (`by.mazokaleh.skarnik.widget.WordOfDayWidgetReceiver`): the launcher binds placed widgets by this name, so
 * renaming it would remove every widget users already have on their home screen.
 */
class WordOfDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordOfTheDayWidget()

    /** Also sent after an app update and a reboot, so widgets placed by the Flutter app start refreshing. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WordOfTheDayWork.schedule(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WordOfTheDayWork.schedule(context)
    }

    /** The last widget was removed. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WordOfTheDayWork.cancel(context)
    }
}
