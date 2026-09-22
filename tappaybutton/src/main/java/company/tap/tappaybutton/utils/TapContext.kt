package company.tap.tappaybutton.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log

/*
 * TapContext.kt
 *
 * Finding the activity a view is living in, and starting things from it.
 *
 * No iOS counterpart .. a UIView reaches its presenter through the responder chain, and this is
 * the Android equivalent of that walk.
 */

private const val TAG = "TapContext"

/**
 * Walks out through the theme wrappers a view's context is usually buried under, to the activity
 * underneath. Null when the view was built with an application context
 */
internal fun Context.tapHostActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Opens one of the sdk's own screens.
 *
 * Started from the activity the button is in, so it belongs to the app's task .. it opens over
 * the app and Back returns to it. `FLAG_ACTIVITY_NEW_TASK` would put it in a task of its own
 * instead, which the system shows as a second window in Recents, labelled with whatever the
 * app's launcher activity is called rather than with anything to do with paying.
 *
 * The flag is only used when there is no activity to start from, where it is the one way to
 * start anything at all
 * @param intent The screen to open
 */
internal fun Context.tapStartSdkActivity(intent: Intent) {
    val host: Activity? = tapHostActivity()

    if (host == null) {
        Log.i(TAG, "no activity behind the button, opening in a task of its own")
        startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        return
    }

    host.startActivity(intent)
}
