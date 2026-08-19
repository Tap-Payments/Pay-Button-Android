package company.tap.tappaybutton

import android.net.Uri
import android.util.Base64
import android.util.Log

class ThreeDSPasskeySession private constructor() {

    companion object {

        private const val TAG = "ThreeDSPasskeySession"

        @Volatile
        var current: ThreeDSPasskeySession? = null
            private set

        /*
         * IMPORTANT:
         *
         * Keep the last successful Passkey authentication URL even
         * after the current session is cleared.
         *
         * This is used by ThreeDsWebViewActivityButton when the
         * 3DS screen is opened/recreated.
         */
        @Volatile
        private var lastAuthenticatedUrl: String? = null

        @Synchronized
        fun start(
            threeDsUrl: String,
            redirectUrl: String?,
            keyword: String?,
            listener: Listener
        ): ThreeDSPasskeySession {

            current?.let {

                Log.i(
                    TAG,
                    "A passkey authentication is already running. " +
                            "Updating listener."
                )

                it.listener = listener

                return it
            }

            /*
             * New authentication session.
             *
             * Remove the URL from an old authentication so that
             * the new 3DS flow cannot accidentally use an old URL.
             */
            lastAuthenticatedUrl = null

            val session = ThreeDSPasskeySession()

            session.listener = listener
            session.redirectUrl = redirectUrl

            val threeDsUri = Uri.parse(threeDsUrl)

            session.authenticationIdentifier =
                threeDsUri.lastPathSegment

            session.keyword =
                keyword?.takeIf { it.isNotEmpty() }
                    ?: session.keywordFrom(
                        session.authenticationIdentifier
                    )

            session.reported = false

            current = session

            Log.i(
                TAG,
                "Passkey session created. " +
                        "authenticationIdentifier=" +
                        session.authenticationIdentifier
            )

            return session
        }

        /**
         * Returns the URL received after Passkey authentication.
         *
         * Example:
         * https://sdk.dev.tap.company/?auth_payer=XXXX
         */
        @JvmStatic
        fun getLastAuthenticatedUrl(): String? {
            return lastAuthenticatedUrl
        }

        /**
         * Clears the stored authentication URL.
         *
         * Call this only after the URL has been consumed by
         * the 3DS WebView.
         */
        @JvmStatic
        fun clearLastAuthenticatedUrl() {
            lastAuthenticatedUrl = null
        }

        /**
         * Called by PasskeyWebViewActivity when the browser redirects
         * back to:
         *
         * tapcardwebsdk://onpasskeyredirect/...
         */
        @JvmStatic
        fun handleCallback(callback: Uri?) {

            val session = current

            if (session == null) {

                Log.e(
                    TAG,
                    "Passkey callback received but no active session exists."
                )

                return
            }

            session.onCallback(callback)
        }

        /**
         * Called when callback Activity is destroyed without
         * receiving a callback.
         */
        @JvmStatic
        fun cancelCurrent() {
            current?.onCanceled()
        }
    }

    interface Listener {

        fun onSucceeded(redirectionUrl: String)

        fun onCanceled()

        fun onFailed(error: Throwable)
    }

    private var listener: Listener? = null

    private var redirectUrl: String? = null

    private var keyword: String? = null

    private var authenticationIdentifier: String? = null

    @Volatile
    private var reported = false

    private fun keywordFrom(
        authenticationIdentifier: String?
    ): String? {

        return null
    }

    @Synchronized
    fun onCallback(callback: Uri?) {

        if (reported) {

            Log.i(
                TAG,
                "Callback already reported. Ignoring duplicate."
            )

            return
        }

        if (callback == null) {

            onFailed(
                IllegalArgumentException(
                    "Passkey callback URI is null"
                )
            )

            return
        }

        reported = true

        Log.i(
            TAG,
            "Processing passkey callback: $callback"
        )

        val data = callback.getQueryParameter("data")

        val finalUrl: String

        if (data.isNullOrEmpty()) {

            Log.i(
                TAG,
                "Callback contains no data parameter."
            )

            finalUrl =
                assumedReturnUrl()
                    ?: callback.toString()

        } else {

            val decoded = base64Decoded(data)

            val unwrapped =
                decoded ?: data

            Log.i(
                TAG,
                "Decoded passkey callback data: $unwrapped"
            )

            finalUrl =
                if (isHttpUrl(unwrapped)) {

                    unwrapped

                } else {

                    Log.i(
                        TAG,
                        "Callback data is not an HTTP/HTTPS URL."
                    )

                    assumedReturnUrl()
                        ?: callback.toString()
                }
        }

        Log.i(
            TAG,
            "Passkey authentication succeeded. " +
                    "Returning URL: $finalUrl"
        )

        /*
         * IMPORTANT:
         *
         * Save the authentication URL BEFORE clearing the session.
         *
         * ThreeDsWebViewActivityButton can now retrieve it even though
         * current becomes null.
         */
        lastAuthenticatedUrl = finalUrl

        val callbackListener = listener

        clearCurrent()

        callbackListener?.onSucceeded(finalUrl)
    }

    @Synchronized
    fun onCanceled() {

        if (reported) {
            return
        }

        reported = true

        Log.i(
            TAG,
            "Passkey authentication cancelled."
        )

        val callbackListener = listener

        clearCurrent()

        callbackListener?.onCanceled()
    }

    @Synchronized
    fun onFailed(error: Throwable?) {

        if (reported) {
            return
        }

        reported = true

        val actualError =
            error
                ?: Exception(
                    "Unknown passkey authentication error"
                )

        Log.e(
            TAG,
            "Passkey authentication failed.",
            actualError
        )

        val callbackListener = listener

        clearCurrent()

        callbackListener?.onFailed(actualError)
    }

    private fun base64Decoded(
        value: String
    ): String? {

        return try {

            val decoded =
                Base64.decode(
                    value,
                    Base64.URL_SAFE or
                            Base64.NO_WRAP or
                            Base64.NO_PADDING
                )

            String(
                decoded,
                Charsets.UTF_8
            )

        } catch (e: IllegalArgumentException) {

            Log.w(
                TAG,
                "Unable to Base64 decode callback data.",
                e
            )

            null
        }
    }

    private fun isHttpUrl(
        value: String?
    ): Boolean {

        if (value.isNullOrEmpty()) {
            return false
        }

        return value.startsWith(
            "https://",
            ignoreCase = true
        ) ||
                value.startsWith(
                    "http://",
                    ignoreCase = true
                )
    }

    private fun assumedReturnUrl(): String? {

        val base =
            redirectUrl
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        val key =
            keyword
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        val id =
            authenticationIdentifier
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        return try {

            Uri.parse(base)
                .buildUpon()
                .clearQuery()
                .appendQueryParameter(
                    key,
                    id
                )
                .build()
                .toString()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Unable to build assumed return URL.",
                e
            )

            null
        }
    }

    private fun clearCurrent() {

        synchronized(
            ThreeDSPasskeySession::class.java
        ) {

            if (current === this) {
                current = null
            }
        }
    }
}