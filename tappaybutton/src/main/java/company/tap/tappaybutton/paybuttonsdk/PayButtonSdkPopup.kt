package company.tap.tappaybutton.paybuttonsdk

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import company.tap.tappaybutton.PayButton

/*
 * PayButtonSdkPopup.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/PayButtonSdk/private/extensions/PayButtonSdk+Popup.swift and
 * Logic/PayButtonSdk/private/views/PayButtonPopupViewController.swift
 *
 * The windows the web sdk opens with `window.open`, ex the click to pay identity flow.
 *
 * The web view has to be the one handed back through the WebViewTransport, that is what
 * keeps the popup's `window.opener` pointing at the form. Click to pay posts the card the
 * payer picked back through it, so building our own web view and loading the url into it
 * instead would leave the form waiting forever.
 *
 * The button is only as tall as the form, so the identity flow gets a dialog of its own the
 * way iOS gives it a presented controller.
 */
internal class PayButtonPopupChromeClient(
    private val payButton: PayButton
) : WebChromeClient() {

    /** The popup web view, created out of the opener's own transport */
    private var popupWebView: WebView? = null
    /** The dialog the popup is shown in */
    private var popupDialog: Dialog? = null
    /** What the popup web view is added to */
    private var popupContainer: FrameLayout? = null

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {

        Log.d(TAG, "window.open() detected. isDialog=$isDialog, isUserGesture=$isUserGesture")

        if (resultMsg == null) {
            Log.e(TAG, "WebViewTransport message is null")
            return false
        }

        val parentContext = view?.context ?: payButton.context

        // One popup at a time, a second would be left orphaned behind the first
        closePopupWebView()

        val newWebView = WebView(parentContext)
        popupWebView = newWebView

        with(newWebView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        newWebView.setBackgroundColor(Color.WHITE)

        // The popup fires the same web sdk callbacks the form does, so it is routed the same
        // way. A passkey can arrive here too, which is why the policy runs before anything
        // is loaded rather than after
        newWebView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                webView: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false
                Log.d(TAG, "Popup URL: $url")
                return payButton.decidePolicyFor(url, webView)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Popup page started: $url")

                // Some navigations reach onPageStarted without ever reaching
                // shouldOverrideUrlLoading, ex a redirect inside a nested browsing context.
                // A passkey must not be allowed to keep loading here either
                if (isPasskeyNavigation(url)) {
                    Log.d(TAG, "Passkey URL detected in popup onPageStarted: $url")
                    view.stopLoading()
                    payButton.startFidoAuthentication(
                        threeDsUrl = url,
                        redirectUrl = payButton.lastCardRedirection?.redirectUrl
                    )
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                Log.e(TAG, "Popup WebView error: ${error.errorCode} ${error.description}")
                super.onReceivedError(view, request, error)
            }
        }

        // A popup opened from the popup gets the same treatment
        newWebView.webChromeClient = this

        val dialog = Dialog(parentContext, android.R.style.Theme_Translucent_NoTitleBar)
        popupDialog = dialog
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)

        val container = FrameLayout(parentContext)
        popupContainer = container
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.setBackgroundColor(Color.WHITE)
        container.addView(
            newWebView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        dialog.setContentView(container)

        // Back closes only the popup, the payment web view stays alive underneath
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Log.d(TAG, "Closing popup WebView")
                closePopupWebView()
                true
            } else {
                false
            }
        }

        dialog.setOnDismissListener {
            Log.d(TAG, "Popup dialog dismissed")
            cleanupPopupWebView()
        }

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Android hands us the transport through resultMsg, and the web view we attach to it
        // is the one the page ends up talking to
        val transport = resultMsg.obj as? WebView.WebViewTransport
        if (transport == null) {
            Log.e(TAG, "Unable to get WebViewTransport")
            closePopupWebView()
            return false
        }

        transport.webView = newWebView
        resultMsg.sendToTarget()

        return true
    }

    /** The page closed the window it opened, ex click to pay handed its result to the form */
    override fun onCloseWindow(window: WebView?) {
        Log.d(TAG, "window.close() received")
        if (window === popupWebView) {
            closePopupWebView()
        } else {
            super.onCloseWindow(window)
        }
    }

    /** Takes the popup down, if one is up */
    internal fun closePopupWebView() {
        try {
            popupDialog?.dismiss()
        } catch (error: Exception) {
            Log.e(TAG, "Error dismissing popup dialog", error)
            cleanupPopupWebView()
        }
    }

    /** Lets go of the popup web view once its dialog is gone */
    private fun cleanupPopupWebView() {
        try {
            popupWebView?.let { webView ->
                webView.stopLoading()
                webView.webChromeClient = null
                popupContainer?.removeView(webView)
                webView.destroy()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Error cleaning popup WebView", error)
        }

        popupWebView = null
        popupContainer = null
        popupDialog = null
    }

    private companion object {
        private const val TAG = "PayButtonChromeClient"
    }
}
