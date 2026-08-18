package company.tap.tappaybutton

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PasskeyWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private var callbackHandled = false

    companion object {

        const val EXTRA_URL = "passkey_url"

        var onAuthenticationCompleted: ((String) -> Unit)? = null

        var onAuthenticationCancelled: (() -> Unit)? = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)

        setContentView(webView)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowContentAccess = true
            allowFileAccess = false
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {

                val url = request.url.toString()

                Log.d(
                    "PasskeyWebView",
                    "Navigation URL: $url"
                )

                if (isAuthenticationCallback(url)) {

                    handleAuthenticationCallback(url)

                    return true
                }

                return false
            }

            override fun onPageStarted(
                view: WebView,
                url: String,
                favicon: Bitmap?
            ) {

                Log.d(
                    "PasskeyWebView",
                    "Page started: $url"
                )

                if (isAuthenticationCallback(url)) {

                    handleAuthenticationCallback(url)

                    return
                }

                super.onPageStarted(view, url, favicon)
            }
        }

        val passkeyUrl =
            intent.getStringExtra(EXTRA_URL)

        if (passkeyUrl.isNullOrBlank()) {

            Log.e(
                "PasskeyWebView",
                "Passkey URL is empty"
            )

            finish()
            return
        }

        Log.d(
            "PasskeyWebView",
            "Loading Passkey URL: $passkeyUrl"
        )

        webView.loadUrl(passkeyUrl)
    }

    /**
     * Checks only for the Tap authentication callback.
     *
     * Example:
     *
     * https://sdk.dev.tap.company/?auth_payer=auth_payer_sSMda2926157ogpi14tM7K734
     */
    private fun isAuthenticationCallback(url: String): Boolean {

        return try {

            val uri = android.net.Uri.parse(url)

            uri.scheme.equals(
                "https",
                ignoreCase = true
            ) &&
                    uri.host.equals(
                        "sdk.dev.tap.company",
                        ignoreCase = true
                    ) &&
                    !uri.getQueryParameter("auth_payer")
                        .isNullOrBlank()

        } catch (e: Exception) {

            Log.e(
                "PasskeyWebView",
                "Unable to parse URL: $url",
                e
            )

            false
        }
    }

    /**
     * Called exactly once when:
     *
     * https://sdk.dev.tap.company/?auth_payer=XXXX
     *
     * is reached.
     */
    private fun handleAuthenticationCallback(url: String) {

        if (callbackHandled) {
            Log.d(
                "PasskeyWebView",
                "Authentication callback already handled"
            )
            return
        }

        callbackHandled = true

        Log.d(
            "PasskeyWebView",
            "AUTHENTICATION CALLBACK RECEIVED"
        )

        Log.d(
            "PasskeyWebView",
            "Callback URL: $url"
        )

        /*
         * Stop the callback page from loading.
         */
        webView.stopLoading()

        /*
         * Send the FULL callback URL back to PayButton.
         */
        onAuthenticationCompleted?.invoke(url)
        finish()
        overridePendingTransition(0, 0)

        /*
         * Clear callback so it cannot be called again.
         */
        onAuthenticationCompleted = null
        onAuthenticationCancelled = null

        /*
         * Close the Passkey WebView immediately.
         */
     //   finish()
    }

    /**
     * User pressed Back.
     *
     * No authentication callback was received.
     * Therefore DO NOT call loadAuthernticate().
     */
    override fun onBackPressed() {

        if (!callbackHandled) {

            Log.d(
                "PasskeyWebView",
                "Passkey WebView closed by user"
            )

            onAuthenticationCancelled?.invoke()
        }

        onAuthenticationCompleted = null
        onAuthenticationCancelled = null

        cleanupWebView()

        super.onBackPressed()
    }

    override fun onDestroy() {

        Log.d(
            "PasskeyWebView",
            "Passkey WebView destroyed"
        )

        cleanupWebView()

        super.onDestroy()
    }

    private fun cleanupWebView() {

        try {

            if (::webView.isInitialized) {

                webView.stopLoading()

                webView.webChromeClient = null
               // webView.webViewClient = null

                webView.removeAllViews()

                webView.destroy()
            }

        } catch (e: Exception) {

            Log.e(
                "PasskeyWebView",
                "Error destroying WebView",
                e
            )
        }
    }
}