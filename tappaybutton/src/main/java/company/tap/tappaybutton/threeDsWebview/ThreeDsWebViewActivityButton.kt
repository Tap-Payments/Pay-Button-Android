package company.tap.tappaybutton.threeDsWebview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.PaymentFlow
import company.tap.tappaybutton.enums.tapID
import company.tap.tappaybutton.getDeviceSpecs
import company.tap.tappaybutton.models.CardRedirection
import company.tap.tappaybutton.models.Redirection
import company.tap.tappaybutton.paybuttonsdk.handleCardAuthenticationCanceled
import company.tap.tappaybutton.paybuttonsdk.passCardAuthenticationToSDK
import company.tap.tappaybutton.utils.tapExtractDataFromUrl
import java.util.Locale

/*
 * ThreeDsWebViewActivityButton.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/views/ThreeDSViewController.swift
 *
 * The page an authentication runs on. It loads out of sight first and only comes forward
 * once the acs has stopped redirecting, so the payer sees the page they are meant to act on
 * rather than the bounces on the way to it.
 *
 * Two flows end here and they end differently:
 *
 *  - The shared buttons watch for `tap_id` and hand the web sdk the query string, which is
 *    all `window.retrieve` wants.
 *  - The card form names the query key itself, ex `auth_payer`, and wants the whole url,
 *    which it reads the result out of on its own.
 *
 * A return is recognised by the query key being present, not by the url containing the word
 * somewhere. The old `contains` matched an acs url that merely mentioned it in a path and
 * ended the authentication before the payer had done anything.
 */
class ThreeDsWebViewActivityButton : AppCompatActivity() {

    /** The bottom sheet the page is shown in once it settles */
    private lateinit var threeDsBottomsheet: BottomSheetDialogFragment
    /** Which of the two flows is running */
    private lateinit var paymentFlow: String

    /** The web view the 3ds page renders in */
    private lateinit var webView: WebView

    /** The details of the page to load. Mirrors `redirectionData` */
    private var redirectionData: Redirection? = null
    /**
     * Set for the card based buttons only. The card web sdk names the query parameter it
     * wants us to watch for, ex `auth_payer`, instead of the shared redirection keyword.
     * When it is set the whole url is handed back rather than only its query string
     */
    private var cardRedirectionKeyword: String? = null

    /** Fires when nothing has loaded for a while, which is when the page comes forward */
    private val idleHandler: Handler = Handler(Looper.getMainLooper())
    private var idleCallback: Runnable? = null
    /** Set once the page has been shown, it only comes forward once */
    private var hasPresented: Boolean = false
    /** Set when the sheet could not be shown yet because the activity was not resumed */
    private var shouldShowBottomSheet: Boolean = false
    /** Set once the authentication has ended, so a late navigation can not end it twice */
    private var hasFinishedAuthentication: Boolean = false

    companion object {
        /** How long nothing has to load before the page is taken to be ready for the payer */
        private const val IDLE_DELAY_MILLISECONDS: Long = 1000

        /**
         * The button that opened this page, set by `navigateTo3dsActivity`.
         *
         * Nullable rather than lateinit .. a page reached without a button behind it is a
         * state to notice and log, not one to crash on
         */
        @SuppressLint("StaticFieldLeak")
        var payButton: PayButton? = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_three_ds_web_view)

        LocalizationManager.setLocale(
            this,
            Locale(PayButtonDataConfiguration.lanuage.toString())
        )

        paymentFlow = intent.extras?.getString("flow") ?: PaymentFlow.PAYMENTBUTTON.name

        /*
         * What to load, and what ends it, comes from whichever flow asked for the page
         */
        when (paymentFlow) {
            PaymentFlow.CARDPAY.name -> {
                val cardRedirection: CardRedirection? = PayButton.threeDsResponseCardPayButtons
                redirectionData = Redirection(
                    url = cardRedirection?.threeDsUrl,
                    id = null,
                    powered = cardRedirection?.powered,
                    stopRedirection = false
                )
                // Watch for the card sdk's own keyword instead of the shared redirection one
                cardRedirectionKeyword = cardRedirection?.keyword
            }

            else -> {
                redirectionData = PayButton.threeDsResponse
                cardRedirectionKeyword = null
            }
        }

        buildWebView()

        val pageUrl: String? = redirectionData?.url?.takeIf { it.isNotEmpty() }
        if (pageUrl == null) {
            Log.e(TAG, "there is no 3ds url to load for $paymentFlow")
            finish()
            return
        }

        threeDsBottomsheet = ThreeDsBottomSheetFragmentButton(
            webView = webView,
            powered = redirectionData?.powered ?: true,
            onCancel = { threeDSCanceled() }
        )

        Log.d(TAG, "loading the 3ds url $pageUrl")
        webView.loadUrl(pageUrl)
    }

    /** Builds the web view the page renders in. Mirrors `themeWebView` */
    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView() {
        webView = WebView(this)
        webView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            this.getDeviceSpecs().first
        )

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        webView.requestFocus()
        webView.webViewClient = ThreeDsWebViewClient()

        findViewById<LinearLayout>(R.id.linear).addView(webView)
    }

    //MARK: - Ending the authentication

    /**
     * The acs landed on the url that ends the authentication.
     *
     * Mirrors `redirectionReached`. The page comes down first and the answer is handed over
     * after, so the form is not asked to finish while a sheet is still on top of it
     * @param redirectionUrl What to hand back, the whole url or its query depending on the flow
     */
    private fun redirectionReached(redirectionUrl: String) {
        if (hasFinishedAuthentication) return
        hasFinishedAuthentication = true

        cancelIdleTimer()
        Log.d(TAG, "the authentication ended on $redirectionUrl")

        dismissSheet()

        when (paymentFlow) {
            // The card form is finished through the button that started this page. It is set
            // by navigateTo3dsActivity, and only a page reached some other way could be
            // missing it, in which case there is nobody to hand the answer to
            PaymentFlow.CARDPAY.name -> {
                val button: PayButton? = payButton
                if (button != null) {
                    button.passCardAuthenticationToSDK(redirectionUrl)
                } else {
                    Log.e(TAG, "the authentication ended with no button to hand it back to")
                }
            }

            else -> PayButton.retrieve(redirectionUrl)
        }

        finish()
    }

    /** The payer backed out of the page. Mirrors `threeDSCanceled` */
    private fun threeDSCanceled() {
        if (hasFinishedAuthentication) return
        hasFinishedAuthentication = true

        cancelIdleTimer()
        dismissSheet()

        when (paymentFlow) {
            PaymentFlow.CARDPAY.name -> payButton?.handleCardAuthenticationCanceled()
            else -> {
                PayButton.cancel()
                PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()
            }
        }

        finish()
    }

    /** Takes the sheet down, if it went up at all */
    private fun dismissSheet() {
        if (::threeDsBottomsheet.isInitialized && threeDsBottomsheet.isAdded) {
            threeDsBottomsheet.dismissAllowingStateLoss()
        }
    }

    //MARK: - Coming forward

    /**
     * Restarts the idle timer.
     *
     * Mirrors the Timer in `webView(_:didFinish:)`. Every page that finishes pushes the
     * moment back, so the sheet only appears once the acs has stopped moving. The old code
     * waited a flat five seconds from the first page instead, which showed the payer
     * whatever happened to be on screen at that point
     */
    private fun restartIdleTimer() {
        cancelIdleTimer()
        val callback = Runnable { idleForWhile() }
        idleCallback = callback
        idleHandler.postDelayed(callback, IDLE_DELAY_MILLISECONDS)
    }

    private fun cancelIdleTimer() {
        idleCallback?.let { idleHandler.removeCallbacks(it) }
        idleCallback = null
    }

    /** Nothing has loaded for a while, so the page is ready to be acted on */
    private fun idleForWhile() {
        if (hasPresented || hasFinishedAuthentication) return
        hasPresented = true

        if (isFinishing || isDestroyed) return

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            showThreeDsBottomSheet()
        } else {
            shouldShowBottomSheet = true
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (shouldShowBottomSheet) {
            shouldShowBottomSheet = false
            showThreeDsBottomSheet()
        }
    }

    private fun showThreeDsBottomSheet() {
        if (!::threeDsBottomsheet.isInitialized) return
        if (threeDsBottomsheet.isAdded || supportFragmentManager.isStateSaved) return

        Log.d(TAG, "showing the 3ds page")
        threeDsBottomsheet.show(supportFragmentManager, "3DS_BOTTOM_SHEET")
    }

    override fun onDestroy() {
        cancelIdleTimer()
        super.onDestroy()
    }

    //MARK: - Web view delegate

    /** Mirrors the WKNavigationDelegate on the iOS ThreeDSView */
    private inner class ThreeDsWebViewClient : WebViewClient() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url ?: return false
            val absoluteString: String = url.toString()
            Log.d(TAG, "3ds navigation $absoluteString")

            val keyword: String? = cardRedirectionKeyword?.takeIf { it.isNotEmpty() }

            if (keyword != null) {
                // The card sdk wants the whole url back, it reads the result out of it itself
                val answer: String = tapExtractDataFromUrl(url, keyword, shouldBase64Decode = false)
                if (answer.isNotEmpty()) {
                    redirectionReached(absoluteString)
                    return true
                }
                return false
            }

            // The shared buttons only need the query string, which is what `retrieve` reads
            val answer: String = tapExtractDataFromUrl(url, tapID, shouldBase64Decode = false)
            if (answer.isNotEmpty()) {
                redirectionReached(url.encodedQuery ?: absoluteString)
                return true
            }

            return false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            // A page that started loading means the acs is still moving, so the wait restarts
            cancelIdleTimer()
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            Log.d(TAG, "3ds page finished $url")
            restartIdleTimer()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            Log.e(TAG, "3ds page error ${error.errorCode}: ${error.description}")
            // A page that failed still stopped moving, so the payer is shown what there is
            restartIdleTimer()
            super.onReceivedError(view, request, error)
        }
    }
}

private const val TAG = "3DS"
