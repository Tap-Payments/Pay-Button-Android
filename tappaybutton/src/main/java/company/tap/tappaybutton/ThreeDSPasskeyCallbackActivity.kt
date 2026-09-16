package company.tap.tappaybutton

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import company.tap.tappaybutton.views.ThreeDSPasskeySession

/*
 * ThreeDSPasskeyCallbackActivity.kt
 *
 * The Android half of what ASWebAuthenticationSession does for free on iOS.
 *
 * The browser running the passkey knows nothing about this sdk, so the return page bounces
 * to `tapCardWebSDK://onPasskeyRedirect?data=...` and the system hands that url to whoever
 * claims the scheme. This is that one component. It reads nothing out of the callback
 * itself .. everything after this point is ThreeDSPasskeySession, which is the same code
 * the iOS session runs.
 *
 * This replaces PasskeyWebViewActivity, ThreeDSPasskeyRedirectActivity and
 * PasskeyCallbackActivity, which were three activities claiming the same scheme with three
 * different ideas of what to do with it. Two of them could win the intent, and which one
 * did was left to the order the manifest happened to be merged in.
 */
class ThreeDSPasskeyCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deliver(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliver(intent)
        finish()
    }

    /** Hands the callback to the running session, if it is one of ours and one is running */
    private fun deliver(intent: Intent?) {
        val callback: Uri? = intent?.data

        if (callback == null) {
            Log.e(TAG, "the passkey callback carries no url")
            return
        }

        Log.i(TAG, "the passkey came back on $callback")

        // Android lower cases the scheme it parses, so the declared `tapCardWebSDK` and the
        // `tapcardwebsdk` that arrives are the same thing and have to be compared as such
        if (!callback.scheme.equals(CALLBACK_SCHEME, ignoreCase = true)) {
            Log.e(TAG, "that is not our scheme, ${callback.scheme}")
            return
        }

        if (!callback.host.equals(CALLBACK_HOST, ignoreCase = true)) {
            Log.e(TAG, "that is not our callback, ${callback.host}")
            return
        }

        ThreeDSPasskeySession.handleCallback(callback)

        // Handing the callback over is not the end of it. The custom tab is still sitting in
        // this task, and this activity finishing only uncovers it .. so the payer would be
        // left looking at a finished acs page. Putting the screen they started from back on
        // top is what actually ends the tab
        ThreeDSPasskeySession.returnToHost(this)
    }

    private companion object {
        private const val TAG = "ThreeDSPasskeyCallback"
        /** The scheme the card form fires its events on, which the return page bounces to */
        private const val CALLBACK_SCHEME = "tapcardwebsdk"
        /** The event the return page names, `tapCardWebSDK://onPasskeyRedirect?data=...` */
        private const val CALLBACK_HOST = "onpasskeyredirect"
    }
}
