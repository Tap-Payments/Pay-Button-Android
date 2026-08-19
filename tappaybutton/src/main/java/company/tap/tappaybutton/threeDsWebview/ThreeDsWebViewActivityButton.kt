package company.tap.tappaybutton.threeDsWebview

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tappaybutton.PaymentFlow
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.ThreeDSPasskeySession
import company.tap.tappaybutton.doAfterSpecificTime
import company.tap.tappaybutton.getDeviceSpecs
import org.json.JSONObject
import java.util.Locale

const val delayTime = 5000L

class ThreeDsWebViewActivityButton : AppCompatActivity() {

    lateinit var threeDsBottomsheet: BottomSheetDialogFragment
    lateinit var paymentFlow: String

    var loadedBottomSheet = false

    private lateinit var webView: WebView

    private var shouldShowBottomSheet = false
    var authenticationUrl=""
    /*
     * ONLY Passkey authentication URL.
     *
     * Example:
     *
     * https://sdk.dev.tap.company/?auth_payer=XXXX
     *
     * This comes ONLY from ThreeDSPasskeySession.
     */
    private var passkeyAuthenticationUrl: String? = null

    /*
     * Prevent calling loadAuthernticate() more than once.
     */
    private var passkeyAuthenticationLoaded = false

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var payButton: PayButton
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_three_ds_web_view
        )

        LocalizationManager.setLocale(
            this,
            Locale(
                PayButtonDataConfiguration.lanuage.toString()
            )
        )

        val linearLayout: LinearLayout =
            findViewById(R.id.linear)

        /*
         * ---------------------------------------------------------
         * PAYMENT FLOW
         * ---------------------------------------------------------
         */
        paymentFlow =
            intent.extras?.getString("flow")
                ?: PaymentFlow.PAYMENTBUTTON.name

        /*
         * ---------------------------------------------------------
         * PASSKEY SESSION URL
         * ---------------------------------------------------------
         *
         * IMPORTANT:
         *
         * We ONLY get the URL from ThreeDSPasskeySession.
         *
         * There is NO savedInstanceState URL.
         * There is NO last3DsUrl.
         */
        passkeyAuthenticationUrl =
            ThreeDSPasskeySession.getLastAuthenticatedUrl()

        if (
            passkeyAuthenticationUrl.isNullOrBlank()
        ) {

            Log.e(
                "3DS",
                "No Passkey authentication URL found"
            )

        } else {

            Log.d(
                "3DS",
                "PASSKEY URL = $passkeyAuthenticationUrl"
            )
        }

        /*
         * ---------------------------------------------------------
         * WEBVIEW
         * ---------------------------------------------------------
         */
        webView = WebView(this)

        webView.layoutParams =
            this.getDeviceSpecs().first.let {

                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    it
                )
            }

        with(webView.settings) {

            javaScriptEnabled = true

            domStorageEnabled = true

            javaScriptCanOpenWindowsAutomatically =
                true

            setSupportMultipleWindows(true)

            allowContentAccess = true

            cacheMode =
                android.webkit.WebSettings.LOAD_NO_CACHE
        }

        webView.requestFocus()

        webView.webViewClient =
            threeDsWebViewClient()

        linearLayout.addView(
            webView
        )

        /*
         * ---------------------------------------------------------
         * LOAD ORIGINAL 3DS PAGE
         * ---------------------------------------------------------
         *
         * IMPORTANT:
         *
         * We DO NOT load the Passkey authentication URL directly.
         *
         * The original 3DS page must be loaded first because it
         * contains:
         *
         * window.loadAuthernticate(...)
         *
         * Then onPageFinished() will inject the Passkey URL.
         */
        val original3DsUrl =
            when (paymentFlow) {

                PaymentFlow.PAYMENTBUTTON.name -> {

                    try {
                        PayButton.threeDsResponse.url
                    } catch (e: Exception) {

                        Log.e(
                            "3DS",
                            "Unable to get PAYMENTBUTTON 3DS URL",
                            e
                        )

                        null
                    }
                }

                PaymentFlow.CARDPAY.name -> {

                    try {
                        PayButton
                            .threeDsResponseCardPayButtons
                            .threeDsUrl
                    } catch (e: Exception) {

                        Log.e(
                            "3DS",
                            "Unable to get CARDPAY 3DS URL",
                            e
                        )

                        null
                    }
                }

                else -> {
                    null
                }
            }

        if (
            !original3DsUrl.isNullOrBlank()
        ) {

            Log.d(
                "3DS",
                "Loading ORIGINAL 3DS URL = $original3DsUrl"
            )

            webView.loadUrl(
                original3DsUrl
            )

        } else {

            Log.e(
                "3DS",
                "Original 3DS URL is empty"
            )
        }

        /*
         * ---------------------------------------------------------
         * BOTTOM SHEET
         * ---------------------------------------------------------
         */
        threeDsBottomsheet =
            ThreeDsBottomSheetFragmentButton(
                webView,
                onCancel = {

                    when (paymentFlow) {

                        PaymentFlow.PAYMENTBUTTON.name -> {
                            PayButton.cancel()
                        }

                        PaymentFlow.CARDPAY.name -> {
                            PayButton.cancel()
                        }
                    }

                    PayButtonDataConfiguration
                        .getTapKnetListener()
                        ?.onPayButtoncancel()
                }
            )
    }

    /*
     * -------------------------------------------------------------
     * NO onSaveInstanceState()
     * -------------------------------------------------------------
     *
     * We intentionally DO NOT save:
     *
     * - last3DsUrl
     * - WebView state
     * - current WebView URL
     * - Passkey URL as saved state
     *
     * Every time this Activity is created, it uses the current
     * ThreeDSPasskeySession value.
     */

    inner class threeDsWebViewClient : WebViewClient() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {

            val url =
                request.url.toString()

            Log.d(
                "3DS",
                "3DS navigation URL = $url"
            )

            when (paymentFlow) {

                /*
                 * -------------------------------------------------
                 * PAYMENT BUTTON
                 * -------------------------------------------------
                 */
                PaymentFlow.PAYMENTBUTTON.name -> {

                    if (
                        url.contains(
                            "tap_id",
                            ignoreCase = true
                        )
                    ) {

                        threeDsBottomsheet
                            .dialog
                            ?.dismiss()

                        val split =
                            url.split("?")

                        try {

                            if (split.size > 1) {

                                PayButton.retrieve(
                                    split[1]
                                )

                            } else {

                                PayButtonDataConfiguration
                                    .getTapKnetListener()
                                    ?.onPayButtonError(
                                        "Invalid tap_id URL"
                                    )
                            }

                        } catch (e: Exception) {

                            PayButtonDataConfiguration
                                .getTapKnetListener()
                                ?.onPayButtonError(
                                    e.message.toString()
                                )
                        }

                        return true
                    }
                }

                /*
                 * -------------------------------------------------
                 * CARD PAY
                 * -------------------------------------------------
                 */
                PaymentFlow.CARDPAY.name -> {

                    if (
                        url.contains(
                            PayButton
                                .threeDsResponseCardPayButtons
                                .keyword
                        )
                    ) {

                        threeDsBottomsheet
                            .dialog
                            ?.dismiss()

                        PayButton.generateTapAuthenticate(
                            url
                        )

                        return true
                    }

                    /*
                     * -------------------------------------------------
                     * PASSKEY REDIRECT
                     * -------------------------------------------------
                     *
                     * Never allow the Passkey redirect to load
                     * inside this WebView.
                     */
                    if (
                        url.contains(
                            "passkey/redirect",
                            ignoreCase = true
                        ) ||
                        url.contains(
                            "/passkey/",
                            ignoreCase = true
                        )
                    ) {

                        Log.d(
                            "3DS",
                            "Passkey redirect intercepted = $url"
                        )

                        view.stopLoading()

                        return true
                    }
                }
            }

            return false
        }

        val javascript =
            "window.loadAuthernticate(" +
                    JSONObject.quote(authenticationUrl) +
                    ");"

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {

            Log.e(
                "3DS",
                "WebView error ${error.errorCode}: ${error.description}"
            )

            super.onReceivedError(
                view,
                request,
                error
            )
        }
    }

    /*
     * -------------------------------------------------------------
     * ACTIVITY RESUMED
     * -------------------------------------------------------------
     *
     * There is NO URL restoration here.
     *
     * We only show the BottomSheet if it was waiting.
     */
    override fun onPostResume() {

        super.onPostResume()

        Log.d(
            "3DS",
            "onPostResume()"
        )

        /*
         * ---------------------------------------------------------
         * GET PASSKEY CALLBACK
         * ---------------------------------------------------------
         *
         * Example:
         *
         * tapcardwebsdk://onPasskeyRedirect?data=Imh0dHA6Ly9zZGsu...
         */
        val passkeySessionUrl =
            ThreeDSPasskeySession.getLastAuthenticatedUrl()

        Log.d(
            "3DS",
            "Current Passkey Session URL = $passkeySessionUrl"
        )

        if (!passkeySessionUrl.isNullOrBlank()) {

             authenticationUrl =
                 extractAuthenticationUrl(
                     passkeySessionUrl
                 ).toString()

            if (!authenticationUrl.isNullOrBlank()) {

                Log.d(
                    "3DS",
                    "========================================"
                )

                Log.d(
                    "3DS",
                    "PASSKEY AUTHENTICATION URL EXTRACTED"
                )

                Log.d(
                    "3DS",
                    "Authentication URL = $authenticationUrl"
                )

                /*
                 * Call JavaScript on the CURRENT 3DS WebView.
                 *
                 * IMPORTANT:
                 *
                 * Do NOT call:
                 *
                 * webView.loadUrl(authenticationUrl)
                 *
                 * We call the JS function from the Mastercard
                 * 3DS page instead.
                 */
                webView.post {

                    try {

                        val javascript =
                            "window.loadAuthernticate(" +
                                    JSONObject.quote(
                                        authenticationUrl
                                    ) +
                                    ");"

                        Log.d(
                            "3DS",
                            "Calling loadAuthernticate()"
                        )

                        Log.d(
                            "3DS",
                            "JavaScript = $javascript"
                        )

                        webView.evaluateJavascript(
                            javascript
                        ) { result ->

                            Log.d(
                                "3DS",
                                "loadAuthernticate result = $result"
                            )
                        }

                    } catch (e: Exception) {

                        Log.e(
                            "3DS",
                            "Failed to call loadAuthernticate()",
                            e
                        )
                    }
                }

                /*
                 * Authentication URL has now been consumed.
                 *
                 * Clear the tapcardwebsdk:// callback so that
                 * onPostResume() does not inject it again.
                 */
                ThreeDSPasskeySession.clearLastAuthenticatedUrl()

                Log.d(
                    "3DS",
                    "Passkey session cleared after injection"
                )
            } else {

                Log.e(
                    "3DS",
                    "Unable to extract authentication URL from Passkey session"
                )
            }
        }

        /*
         * Existing BottomSheet behavior.
         */
        if (shouldShowBottomSheet) {

            shouldShowBottomSheet = false

            showThreeDsBottomSheet()
        }
    }

    /*
     * -------------------------------------------------------------
     * SHOW BOTTOM SHEET
     * -------------------------------------------------------------
     */
    private fun showThreeDsBottomSheet() {

        if (
            !threeDsBottomsheet.isAdded &&
            !supportFragmentManager.isStateSaved
        ) {

            Log.d(
                "3DS",
                "Showing 3DS BottomSheet"
            )

            threeDsBottomsheet.show(
                supportFragmentManager,
                "3DS_BOTTOM_SHEET"
            )
        }
    }
    private fun extractAuthenticationUrl(
        passkeySessionUrl: String
    ): String? {

        return try {

            val uri = Uri.parse(
                passkeySessionUrl
            )

            /*
             * Extract:
             *
             * ?data=XXXX
             */
            val encodedData =
                uri.getQueryParameter("data")

            if (encodedData.isNullOrBlank()) {

                Log.e(
                    "3DS",
                    "Passkey session does not contain data parameter"
                )

                return null
            }

            Log.d(
                "3DS",
                "Passkey encoded data = $encodedData"
            )

            /*
             * Decode Base64.
             */
            val decodedBytes =
                android.util.Base64.decode(
                    encodedData,
                    android.util.Base64.DEFAULT
                )

            val decodedValue =
                String(
                    decodedBytes,
                    Charsets.UTF_8
                )

            Log.d(
                "3DS",
                "Passkey decoded value = $decodedValue"
            )

            /*
             * Your decoded value is:
             *
             * "https://sdk.dev.tap.company/?auth_payer=XXXX"
             *
             * Notice the extra quotes.
             *
             * JSONObject(String) removes those JSON-string quotes
             * correctly.
             */
            val authenticationUrl =
                if (
                    decodedValue.startsWith("\"") &&
                    decodedValue.endsWith("\"")
                ) {

                    JSONObject(
                        "{\"url\":$decodedValue}"
                    ).getString("url")

                } else {

                    decodedValue
                }

            Log.d(
                "3DS",
                "Final Authentication URL = $authenticationUrl"
            )

            authenticationUrl

        } catch (e: Exception) {

            Log.e(
                "3DS",
                "Failed to extract Passkey authentication URL",
                e
            )

            null
        }
    }
}