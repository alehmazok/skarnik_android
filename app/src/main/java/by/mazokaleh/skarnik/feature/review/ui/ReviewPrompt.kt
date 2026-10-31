package by.mazokaleh.skarnik.feature.review.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import by.mazokaleh.skarnik.R
import by.mazokaleh.skarnik.core.common.suspendRunCatching
import by.mazokaleh.skarnik.feature.settings.ui.ExternalActions
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

object ReviewPromptTestTags {
    const val APP_GALLERY_DIALOG = "review_app_gallery_dialog"
}

/**
 * Asks for a review once the translation screen says so: Google Play's in-app review sheet, or, where Play cannot
 * show it (Huawei devices without Play), a dialog that opens the app's page in AppGallery. With neither store,
 * nothing is shown. Flutter's `in_app_review` + `_showReviewFallbackDialog`.
 */
@Stable
class ReviewPromptState internal constructor(
    private val activity: Activity?,
    private val scope: CoroutineScope,
    private val appGalleryDialog: MutableState<Boolean>
) {
    val showAppGalleryDialog: Boolean get() = appGalleryDialog.value

    fun ask() {
        val activity = activity ?: return
        scope.launch {
            if (!launchPlayReview(activity) && appGalleryIntent().resolveActivity(activity.packageManager) != null) {
                appGalleryDialog.value = true
            }
        }
    }

    fun openAppGallery(context: Context) {
        appGalleryDialog.value = false
        try {
            context.startActivity(appGalleryIntent())
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "AppGallery is gone", e)
        }
    }

    fun dismiss() {
        appGalleryDialog.value = false
    }

    /**
     * True when Play accepted the request. Play decides itself whether the sheet appears (quota), and reports
     * nothing back; it fails without the Play Store.
     */
    private suspend fun launchPlayReview(activity: Activity): Boolean = suspendRunCatching {
        val manager = ReviewManagerFactory.create(activity)
        val info = manager.requestReviewFlow().await()
        manager.launchReviewFlow(activity, info).await()
    }.onFailure { Log.i(TAG, "Play in-app review is unavailable", it) }.isSuccess

    private companion object {
        const val TAG = "Review"

        /** Only AppGallery handles this scheme, so resolving it also tells whether AppGallery is installed. */
        fun appGalleryIntent() = Intent(
            Intent.ACTION_VIEW,
            "appmarket://details?id=${ExternalActions.STORE_PACKAGE}".toUri()
        )
    }
}

/** Play's tasks complete on the main thread; a failed or cancelled task (e.g. no Play Store) throws. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Play task was cancelled"))
        }
    }
}

@Composable
fun rememberReviewPromptState(): ReviewPromptState {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    // Saved, so the dialog survives rotation: it is shown once per install.
    val appGalleryDialog = rememberSaveable { mutableStateOf(false) }
    return remember(activity, scope) { ReviewPromptState(activity, scope, appGalleryDialog) }
}

/** The AppGallery fallback. M3: the buttons name the action («Пазней» / «Адкрыць AppGallery»), as in Flutter. */
@Composable
fun ReviewPromptDialog(state: ReviewPromptState) {
    if (!state.showAppGalleryDialog) return
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = state::dismiss,
        title = { Text(stringResource(R.string.review_title)) },
        text = { Text(stringResource(R.string.review_app_gallery_message)) },
        confirmButton = {
            TextButton(onClick = { state.openAppGallery(context) }) {
                Text(stringResource(R.string.review_app_gallery_open))
            }
        },
        dismissButton = {
            TextButton(onClick = state::dismiss) { Text(stringResource(R.string.review_later)) }
        },
        modifier = Modifier.testTag(ReviewPromptTestTags.APP_GALLERY_DIALOG)
    )
}
