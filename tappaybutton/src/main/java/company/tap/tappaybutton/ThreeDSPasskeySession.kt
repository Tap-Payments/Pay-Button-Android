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
                            "Ignoring new authentication."
                )

                return it
            }

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

        /*
         * Put your existing keyword logic here.
         *
         * Returning null is safer than inventing
         * a query parameter.
         */
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

            String(decoded, Charsets.UTF_8)

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

        return value.startsWith("https://") ||
                value.startsWith("http://")
    }

    private fun assumedReturnUrl(): String? {

        val base = redirectUrl
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val key = keyword
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val id = authenticationIdentifier
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

        synchronized(ThreeDSPasskeySession::class.java) {

            if (current === this) {
                current = null
            }
        }
    }
}