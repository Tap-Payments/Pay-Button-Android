package company.tap.tappaybutton

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

class PasskeyWebViewActivity : Activity() {

    companion object {

        private const val TAG = "PasskeyWebViewActivity"

        const val EXTRA_URL =
            "passkey_url"

        private const val CALLBACK_SCHEME =
            "tapcardwebsdk"

        private const val CALLBACK_HOST =
            "onpasskeyredirect"
    }

    private var callbackHandled = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        Log.d(
            TAG,
            "PasskeyWebViewActivity created"
        )

        /*
         * We don't need a WebView here.
         *
         * This Activity exists only to receive the
         * browser callback.
         */
        handleIntent(intent)
    }

    override fun onNewIntent(
        intent: Intent?
    ) {
        super.onNewIntent(intent)

        setIntent(intent)

        Log.d(
            TAG,
            "onNewIntent received: ${intent?.data}"
        )

        handleIntent(intent)
    }

    private fun handleIntent(
        intent: Intent?
    ) {

        if (callbackHandled) {
            return
        }

        val callbackUri =
            intent?.data

        Log.d(
            TAG,
            "Callback URI: $callbackUri"
        )

        if (callbackUri == null) {

            Log.e(
                TAG,
                "Passkey callback URI is null."
            )

            callbackHandled = true

            ThreeDSPasskeySession.current
                ?.onFailed(
                    IllegalArgumentException(
                        "Passkey callback URI is null"
                    )
                )

            finish()

            return
        }

        /*
         * Validate that this is our callback.
         */
        if (
            !callbackUri.scheme.equals(
                CALLBACK_SCHEME,
                ignoreCase = true
            )
        ) {

            Log.e(
                TAG,
                "Unexpected callback scheme: " +
                        "${callbackUri.scheme}"
            )

            return
        }

        if (
            !callbackUri.host.equals(
                CALLBACK_HOST,
                ignoreCase = true
            )
        ) {

            Log.e(
                TAG,
                "Unexpected callback host: " +
                        "${callbackUri.host}"
            )

            return
        }

        callbackHandled = true

        Log.d(
            TAG,
            "Valid Passkey callback received."
        )

        /*
         * ThreeDSPasskeySession will:
         *
         * 1. Decode the callback
         * 2. Call onSucceeded()
         * 3. Clear the current session
         */
        ThreeDSPasskeySession.handleCallback(
            callbackUri
        )

        /*
         * IMPORTANT:
         *
         * Do not keep this Activity alive.
         *
         * The PayButton listener will restore the
         * original WebView.
         */
        finish()
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "PasskeyWebViewActivity destroyed"
        )

        super.onDestroy()
    }
}