package company.tap.tappaybutton.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService

/*
 * ThreeDSBrowser.kt
 *
 * Where a passkey authentication is actually shown.
 *
 * iOS shows it in ASWebAuthenticationSession, which is a real safari drawn over the app
 * rather than a switch to safari. The Android equivalent is a Chrome Custom Tab: it is the
 * browser's own engine, so navigator.credentials and the platform authenticator work, but it
 * opens over the app, inside the app's own task, with the app's colours and a close button
 * instead of a home screen bounce.
 *
 * A plain VIEW intent is kept as the fallback, for a device whose default browser does not
 * offer custom tabs at all. It hands the payer to the browser app, which is what the sdk did
 * before, and it still comes back the same way .. the return page bounces to the callback
 * scheme either way.
 */
internal object ThreeDSBrowser {

    private const val TAG = "ThreeDSBrowser"

    /**
     * Opens the acs page, in app when the device can and in the browser app when it can not
     * @param url The acs page to load
     * @param context The context the button lives in
     * @return True when something opened
     */
    internal fun open(url: Uri, context: Context): Boolean {
        val host: Activity? = context.findActivity()

        if (host == null) {
            // Without an activity there is no task to draw the tab over, so the browser app
            // is the only thing left. It is also the only path that may set NEW_TASK
            Log.i(TAG, "the button has no activity behind it, opening the browser app")
            return openInBrowserApp(url, context, needsNewTask = true)
        }

        if (customTabsPackage(context) == null) {
            Log.i(TAG, "no browser here offers custom tabs, opening the browser app")
            return openInBrowserApp(url, host, needsNewTask = false)
        }

        return try {
            // No NEW_TASK: the tab belongs to the app's task, which is what lets the callback
            // activity clear it away when the authentication ends, and what keeps the payer
            // inside the app the whole time
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .launchUrl(host, url)
            Log.i(TAG, "the passkey is running in a custom tab")
            true
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "the custom tab refused to open, falling back to the browser app", error)
            openInBrowserApp(url, host, needsNewTask = false)
        }
    }

    /** Hands the payer to whichever browser the device uses */
    private fun openInBrowserApp(url: Uri, context: Context, needsNewTask: Boolean): Boolean {
        val browser = Intent(Intent.ACTION_VIEW, url).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (needsNewTask) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(browser)
            true
        } catch (error: ActivityNotFoundException) {
            Log.e(TAG, "there is no browser on this device to run the passkey in", error)
            false
        }
    }

    /**
     * The package of a browser that speaks custom tabs, or null when none does.
     *
     * A browser is asked twice: whether it handles http at all, and whether it binds the
     * custom tabs service. Plenty answer the first and not the second
     */
    private fun customTabsPackage(context: Context): String? {
        val packageManager: PackageManager = context.packageManager
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))

        val browsers: List<ResolveInfo> = packageManager.queryIntentActivities(probe, 0)

        for (browser in browsers) {
            val serviceProbe = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION).apply {
                setPackage(browser.activityInfo.packageName)
            }
            if (packageManager.resolveService(serviceProbe, 0) != null) {
                return browser.activityInfo.packageName
            }
        }
        return null
    }

    /**
     * Walks out through the theme wrappers a view's context is usually buried under, to the
     * activity underneath. Null when the button was built with an application context
     */
    internal fun hostActivity(context: Context): Activity? = context.findActivity()

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
