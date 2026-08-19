package company.tap.tappaybutton.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import company.tap.tappaybutton.PayButtonView
import company.tap.tappaybutton.utils.tapBase64Decoded
import company.tap.tappaybutton.utils.tapGetQueryItems
import company.tap.tappaybutton.utils.tapIsHttpUrl

/*
 * ThreeDSPasskeySession.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/views/ThreeDSPasskeySession.swift
 *
 * Runs the 3DS/ACS challenge in the system browser, which can serve a passkey. A WebView
 * can not .. it has no navigator.credentials, so an issuer that answers with a FIDO
 * challenge has to be handled out here instead of in the 3ds page.
 *
 * Where it is shown is ThreeDSBrowser: a Chrome Custom Tab over the app, which is as close
 * as Android gets to ASWebAuthenticationSession drawing safari over the app.
 *
 * The ending is what Android does not give away. ASWebAuthenticationSession claims the
 * callback scheme itself, closes the browser the moment the page reaches it and hands the
 * url over. Here that is assembled from two halves .. the tab runs in the app's own task,
 * and ThreeDSPasskeyCallbackActivity, the one component in the manifest claiming the
 * callback scheme, is launched over it and clears it away. Everything after that point is
 * the iOS logic, line for line.
 *
 * What it costs, same as on iOS, is visibility: nothing at all is reported between opening
 * the browser and the callback. A payer who authenticated and one who gave up look
 * identical from here, which is what `threeDSAssumesReturnOnDismiss` decides between.
 */

/** Runs the 3ds authentication in the system browser, ending on the callback scheme */
internal class ThreeDSPasskeySession {

    /** Notified as the process moves along. The owner clears it when the payment is over */
    internal var delegate: Delegate? = null

    /** The https return url, used to rebuild what the card form expects when the callback names none */
    private var redirectUrl: String? = null
    /** The query key the card form watches for, ex `auth_payer` */
    private var keyword: String? = null
    /** The identifier the acs page carries in its own path, ex `auth_payer_sSMda29...` */
    private var authenticationIdentifier: String? = null
    /** Set once an outcome has been reported, so only the first one counts */
    @Volatile
    private var hasReported: Boolean = false
    /** Set while the browser is up, so a return to the foreground can be read as a dismissal */
    @Volatile
    private var isBrowserOpen: Boolean = false
    /**
     * The screen the payer was on when the passkey started, so the sdk can put them back on it.
     *
     * A custom tab runs inside the app's own task, and nothing takes it down on its own. The
     * callback activity finishing only uncovers it again, which leaves the payer looking at a
     * finished acs page with no way back. Relaunching this with CLEAR_TOP is what ends the tab
     */
    private var hostActivity: Class<out android.app.Activity>? = null

    /**
     * Starts the authentication process
     * @param threeDsUrl The ACS page to load
     * @param redirectUrl The https return url, used when the callback names no url itself
     * @param callbackScheme The scheme the return page bounces to, ex `tapCardWebSDK`,
     * without the `://`. ThreeDSPasskeyCallbackActivity is what claims it
     * @param keyword The query key the card form watches for, ex `auth_payer`
     * @param context The context the browser is launched from
     */
    internal fun start(
        threeDsUrl: String?,
        redirectUrl: String?,
        callbackScheme: String,
        keyword: String?,
        context: Context
    ) {
        val acsUrl: Uri? = threeDsUrl
            ?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?.takeIf { !it.scheme.isNullOrEmpty() }

        if (acsUrl == null) {
            Log.e(TAG, "could not parse the three ds url ${threeDsUrl ?: "nil"}")
            report(Outcome.Failure(ThreeDSSessionError.InvalidThreeDSUrl))
            return
        }

        this.redirectUrl = redirectUrl
        // The acs names the authentication in the last part of its own path
        this.authenticationIdentifier = acsUrl.lastPathSegment
        this.keyword = keyword?.takeIf { it.isNotEmpty() }
            ?: keyword(this.authenticationIdentifier)

        Log.i(TAG, "starting")
        Log.i(TAG, "three ds url $acsUrl")
        Log.i(TAG, "waiting for $callbackScheme://")
        Log.i(TAG, "that scheme is claimed by ThreeDSPasskeyCallbackActivity, the host app declares nothing")

        current = this
        hostActivity = ThreeDSBrowser.hostActivity(context)?.javaClass
        lastHostActivity = hostActivity

        // Drawn over the app in a custom tab, the way iOS draws it over the app in
        // ASWebAuthenticationSession, and handed to the browser app only when the device
        // leaves no other way
        if (ThreeDSBrowser.open(acsUrl, context)) {
            isBrowserOpen = true
        } else {
            report(Outcome.Failure(ThreeDSSessionError.FailedToStart))
        }
    }

    /**
     * Hands the session a callback, whether it arrived from the browser through
     * ThreeDSPasskeyCallbackActivity or from the card form navigating to it in the web view
     * @param url The callback url
     * @return True when the session took it
     */
    internal fun handleCallback(url: Uri?): Boolean {
        if (hasReported) return false
        if (url == null) {
            report(Outcome.Failure(ThreeDSSessionError.FailedToStart))
            return true
        }
        Log.i(TAG, "the passkey came back on $url")
        isBrowserOpen = false
        report(Outcome.Success(url))
        return true
    }

    /**
     * Puts the payer back on the screen they started from, ending the custom tab with it.
     *
     * CLEAR_TOP finishes everything above that screen in the task, and the tab is what is
     * above it. `singleTask` on the callback activity does not do this on its own .. it only
     * clears the top when it is reusing an instance already in the task, and the one the
     * browser launches is always new
     * @param context The callback activity, which sits in the task the tab is in
     */
    internal fun returnToHost(context: Context) {
        val host: Class<out android.app.Activity> = hostActivity ?: return
        Log.i(TAG, "bringing ${host.simpleName} back, which ends the custom tab")
        context.startActivity(
            Intent(context, host).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    /** Closes the session without telling the delegate, ex the payment it belonged to is over */
    internal fun cancel() {
        Log.i(TAG, "closed from the sdk side, the delegate is not told")
        hasReported = true
        isBrowserOpen = false
        clearCurrent()
    }

    /**
     * The app came back to the foreground while the browser was up and nothing came back
     * on the callback.
     *
     * The browser reports nothing, so this is the only sign the payer left it, and it can
     * not tell apart one who authenticated from one who gave up. What that means is
     * `PayButtonView.threeDSAssumesReturnOnDismiss`: on, the return url is rebuilt from what
     * the acs was given and the backend decides whether the authentication passed; off,
     * every dismissal is a cancel, which is stricter but leaves a completed passkey with no
     * way home unless the return page bounced to the callback first
     */
    internal fun onHostResumed() {
        if (hasReported || !isBrowserOpen) return
        isBrowserOpen = false

        if (!PayButtonView.threeDSAssumesReturnOnDismiss) {
            Log.i(TAG, "came back with no callback, taking it as a cancel")
            report(Outcome.Failure(ThreeDSSessionError.CanceledByUser))
            return
        }

        val assumed: String? = assumedReturnUrl()
        if (assumed == null) {
            Log.i(TAG, "came back with no callback and nothing to rebuild a return url out of")
            report(Outcome.Failure(ThreeDSSessionError.CanceledByUser))
            return
        }

        Log.i(TAG, "came back with no callback, assuming the return url $assumed")
        report(Outcome.Success(Uri.parse(assumed)))
    }

    //MARK: - Private methods

    /**
     * Works out what to hand the card form out of the callback the browser came back on.
     *
     * The return page bounces to `tapCardWebSDK://onPasskeyRedirect?data=...`, and what sits
     * in `data` decides. A url is handed over as it is, a base64 wrapper is unwrapped first,
     * and anything else falls back to the return url rebuilt from the id the acs was given
     */
    private fun redirectionUrl(callbackUrl: Uri): String {
        printCallback(callbackUrl)

        val data: String? = tapGetQueryItems(callbackUrl)["data"]

        if (!data.isNullOrEmpty()) {
            // The card form sends its data base64 encoded elsewhere, so try that first
            val decoded: String = tapBase64Decoded(data) ?: data
            if (decoded != data) Log.i(TAG, "data decoded to $decoded")
            // What comes out is sometimes a json string rather than a bare one, ex
            // "https://sdk.dev.tap.company/?auth_payer=XXXX" with the quotes part of the
            // value. Left in place the quote makes it not look like a url at all, and the
            // authentication falls back to a return url it had to guess
            val unwrapped: String = unquoted(decoded)
            if (unwrapped != decoded) Log.i(TAG, "data was quoted, unwrapped to $unwrapped")
            if (tapIsHttpUrl(unwrapped)) {
                Log.i(TAG, "data names the url to finish on")
                return unwrapped
            }
            Log.i(TAG, "data is not a url, falling back to the return url we can build")
        } else {
            Log.i(TAG, "the callback carries no data")
        }

        val assumed: String? = assumedReturnUrl()
        if (assumed != null) {
            Log.i(TAG, "rebuilt $assumed out of the keyword and the identifier")
            return assumed
        }

        Log.i(TAG, "nothing to build a return url out of, handing the callback over as it is")
        return callbackUrl.toString()
    }

    /**
     * Strips the quotes off a value that arrived as a json string rather than a bare one
     * @param value The decoded callback data
     * @return The value with its surrounding quotes removed, or unchanged when it had none
     */
    private fun unquoted(value: String): String {
        if (value.length < 2) return value
        if (!value.startsWith("\"") || !value.endsWith("\"")) return value
        return try {
            JSONObject("{\"url\":$value}").getString("url")
        } catch (error: JSONException) {
            value.substring(1, value.length - 1)
        }
    }

    /**
     * Rebuilds the url the acs would have landed on, out of the return url, the keyword the
     * card form watches for and the identifier the acs carries in its path
     */
    private fun assumedReturnUrl(): String? {
        val base: String = redirectUrl?.takeIf { it.isNotEmpty() } ?: return null
        val key: String = keyword?.takeIf { it.isNotEmpty() } ?: return null
        val identifier: String = authenticationIdentifier?.takeIf { it.isNotEmpty() } ?: return null

        return try {
            Uri.parse(base)
                .buildUpon()
                .clearQuery()
                .appendQueryParameter(key, identifier)
                .build()
                .toString()
        } catch (error: Exception) {
            Log.e(TAG, "unable to build the assumed return url", error)
            null
        }
    }

    /** Prints the callback taken apart, so what the return page sent back is readable */
    private fun printCallback(url: Uri) {
        Log.i(TAG, "callback $url")
        Log.i(TAG, "callback names ${url.host ?: "nothing"}")
        val items: Map<String, String> = tapGetQueryItems(url)
        if (items.isEmpty()) {
            Log.i(TAG, "the callback carries no query")
        } else {
            for ((name, value) in items) Log.i(TAG, "    $name = $value")
        }
    }

    /** Fans the outcome out to the delegate, always on the main thread and only once */
    private fun report(outcome: Outcome) {
        synchronized(this) {
            if (hasReported) {
                Log.i(TAG, "an outcome was already reported, ignoring this one")
                return
            }
            hasReported = true
        }
        clearCurrent()

        val deliver = Runnable {
            val listener: Delegate? = delegate
            if (listener == null) {
                Log.i(TAG, "nobody is listening, the delegate is null")
                return@Runnable
            }
            when (outcome) {
                is Outcome.Success -> {
                    listener.onReachedCallback(this, outcome.callbackUrl)
                    val redirection: String = redirectionUrl(outcome.callbackUrl)
                    Log.i(TAG, "handing the card form $redirection")
                    listener.onSucceeded(this, redirection)
                }
                is Outcome.Failure -> {
                    if (outcome.error === ThreeDSSessionError.CanceledByUser) {
                        listener.onCanceled(this)
                    } else {
                        listener.onFailed(this, outcome.error)
                    }
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) deliver.run()
        else Handler(Looper.getMainLooper()).post(deliver)
    }

    /** Lets go of the shared reference, but only when it is still pointing at this session */
    private fun clearCurrent() {
        synchronized(ThreeDSPasskeySession::class.java) {
            if (current === this) current = null
        }
    }

    /** What the browser came back with */
    private sealed class Outcome {
        class Success(val callbackUrl: Uri) : Outcome()
        class Failure(val error: Throwable) : Outcome()
    }

    /** Reports the progress of a 3ds process running in the system browser */
    internal interface Delegate {
        /** The callback url exactly as it arrived, before anything is read out of it */
        fun onReachedCallback(session: ThreeDSPasskeySession, callbackUrl: Uri) {}
        /** The process completed, carrying the url the card web sdk expects */
        fun onSucceeded(session: ThreeDSPasskeySession, redirectionUrl: String)
        /** The payer closed the browser before finishing */
        fun onCanceled(session: ThreeDSPasskeySession)
        /** The process could not be completed */
        fun onFailed(session: ThreeDSPasskeySession, error: Throwable)
    }

    internal companion object {

        private const val TAG = "ThreeDSPasskeySession"

        /**
         * The session the browser is currently running for.
         *
         * The callback arrives in its own activity, launched by the system out of a browser
         * that knows nothing about this sdk, so there is no reference to pass along .. the
         * running session has to be findable from a static. One authentication, one browser,
         * so there is only ever one
         */
        @Volatile
        internal var current: ThreeDSPasskeySession? = null
            private set

        /**
         * The last screen a passkey was started from.
         *
         * Kept past the end of the session, because the outcome is reported and the session
         * let go of before the tab has been taken down
         */
        @Volatile
        internal var lastHostActivity: Class<out android.app.Activity>? = null

        /**
         * Puts the payer back on the screen the passkey started from.
         *
         * Has to work whether or not a session is still running .. by the time the callback
         * arrives the outcome may already have been reported and the session cleared, and the
         * tab still has to come down
         */
        internal fun returnToHost(context: Context) {
            val running: ThreeDSPasskeySession? = current
            if (running != null) {
                running.returnToHost(context)
                return
            }

            val host: Class<out android.app.Activity> = lastHostActivity ?: run {
                Log.i(TAG, "there is no screen recorded to go back to")
                return
            }
            Log.i(TAG, "the session is gone, bringing ${host.simpleName} back anyway")
            context.startActivity(
                Intent(context, host).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }

        /**
         * Called by ThreeDSPasskeyCallbackActivity when the browser bounces back to us.
         *
         * Not @JvmStatic .. that would put a static `handleCallback(Uri)` on the class next
         * to the instance one of the same shape, which the jvm has no way to tell apart
         */
        internal fun handleCallback(callback: Uri?): Boolean {
            val session: ThreeDSPasskeySession? = current
            if (session == null) {
                Log.e(TAG, "a passkey callback arrived but no session is running")
                return false
            }
            return session.handleCallback(callback)
        }

        /** Called when the app comes back to the foreground, see `onHostResumed` */
        internal fun hostResumed() {
            current?.onHostResumed()
        }

        /**
         * Reads the query key back out of an acs identifier, ex `auth_payer_sneBZ46...`
         * gives `auth_payer`
         * @param identifier The identifier the acs carries in its path
         * @return The keyword, or null when the identifier has no underscore to split on
         */
        internal fun keyword(identifier: String?): String? {
            if (identifier.isNullOrEmpty()) return null
            val parts: List<String> = identifier.split("_")
            if (parts.size <= 1) return null
            return parts.dropLast(1).joinToString("_")
        }
    }
}
