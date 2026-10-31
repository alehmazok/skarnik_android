package by.mazokaleh.skarnik.feature.settings.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import by.mazokaleh.skarnik.BuildConfig
import by.mazokaleh.skarnik.R

/** Intents Settings sends to other apps: email to the developers and the store listing. */
object ExternalActions {
    private const val TAG = "ExternalActions"

    /** The Play listing. Not [BuildConfig.APPLICATION_ID]: the debug build's id has no listing. */
    private const val STORE_PACKAGE = "by.mazokaleh.skarnik"

    /** «Skarnik 4.0.0 (123)», as Flutter's `getAppNameAndVersion`. */
    fun appNameAndVersion(context: Context): String =
        "${context.getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    /**
     * Opens an email app with the developers' address and the app version in the subject (Flutter's
     * `mailToDevs`); the body starts with the device and Android version under the cursor. Returns false when
     * there is no email app.
     */
    fun mailToDevelopers(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.DEV_EMAIL))
            if (BuildConfig.DEV_EMAIL_BCC.isNotEmpty()) putExtra(Intent.EXTRA_BCC, arrayOf(BuildConfig.DEV_EMAIL_BCC))
            putExtra(Intent.EXTRA_SUBJECT, "${appNameAndVersion(context)}, Android")
            putExtra(Intent.EXTRA_TEXT, "\n\n—\n${deviceInfo()}")
        }
        return context.tryStart(intent)
    }

    /** The app's store page: the store app (Play, or AppGallery on Huawei), else the Play web page. */
    fun openStoreListing(context: Context): Boolean =
        context.tryStart(Intent(Intent.ACTION_VIEW, "market://details?id=$STORE_PACKAGE".toUri())) ||
            context.tryStart(
                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$STORE_PACKAGE".toUri())
            )

    private fun deviceInfo(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    private fun Context.tryStart(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "No app for ${intent.data}", e)
        false
    }
}
