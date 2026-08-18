package company.tap.tappaybutton


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

class ThreeDSPasskeyRedirectActivity : Activity() {

    companion object {
        private const val TAG = "ThreeDSPasskeyRedirect"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Passkey redirect Activity created")

        handleIntent(intent)

        finish()
        overridePendingTransition(0, 0)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Log.i(TAG, "Passkey redirect Activity received new intent")

        setIntent(intent)

        handleIntent(intent)

        finish()
        overridePendingTransition(0, 0)
    }

    private fun handleIntent(intent: Intent?) {

        if (intent == null) {
            Log.e(TAG, "Redirect intent is null")

            ThreeDSPasskeySession.current?.onFailed(
                IllegalStateException("Passkey redirect intent is null")
            )

            return
        }

        val callbackUri: Uri? = intent.data

        if (callbackUri == null) {
            Log.e(TAG, "Passkey redirect URI is null")

            ThreeDSPasskeySession.current?.onFailed(
                IllegalStateException("Passkey redirect URI is null")
            )

            return
        }

        Log.i(TAG, "Passkey callback received: $callbackUri")

        /*
         * Expected callback:
         *
         * tapCardWebSDK://onPasskeyRedirect?data=...
         *
         * Android normalizes the scheme to lowercase,
         * therefore we compare case-insensitively.
         */

        val scheme = callbackUri.scheme
        val host = callbackUri.host

        if (!scheme.equals("tapcardwebsdk", ignoreCase = true)) {

            Log.e(TAG, "Invalid callback scheme: $scheme")

            ThreeDSPasskeySession.current?.onFailed(
                IllegalArgumentException(
                    "Invalid passkey callback scheme"
                )
            )

            return
        }

        if (!host.equals("onpasskeyredirect", ignoreCase = true)) {

            Log.e(TAG, "Invalid callback host: $host")

            ThreeDSPasskeySession.current?.onFailed(
                IllegalArgumentException(
                    "Invalid passkey callback host"
                )
            )

            return
        }

        val session = ThreeDSPasskeySession.current

        if (session == null) {

            Log.w(
                TAG,
                "No active ThreeDSPasskeySession. Ignoring callback."
            )

            return
        }

        /*
         * Let the session process:
         *
         * data
         * Base64 decoding
         * redirect URL
         * duplicate protection
         * success callback
         */
        session.onCallback(callbackUri)
    }
}