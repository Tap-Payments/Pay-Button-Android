package company.tap.tappaybutton.paybuttonsdk

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.WebView
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.enums.cardPrefix
import company.tap.tappaybutton.enums.careemPayUrlHandler
import company.tap.tappaybutton.models.Redirection
import java.net.URISyntaxException

/*
 * PayButtonSdkNavigationPolicy.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/PayButtonSdk/private/extensions/PayButtonSdk+NavigationPolicy.swift
 *
 * Every navigation the web sdk attempts comes through here first. A url is either an event
 * the button sdk is firing at us, an event from the card form, a handoff to an app that is
 * not us, or a real page to load, and this is what tells them apart and hands each one to
 * its own handler.
 *
 * The handoffs are the one section with no iOS counterpart. iOS has the system open a
 * universal link on its own; on Android the page navigates to `samsungpay://`, `intent://`
 * or an app store url and the host is expected to resolve it, so the button does.
 */

private const val TAG = "PayButton"

/** Where samsung pay hands over to its own app */
private const val SAMSUNG_PAY_URL_PREFIX = "samsungpay"
/** Where the payer is sent when samsung pay is not installed */
private const val SAMSUNG_APP_STORE_URL = "samsungapps://ProductDetail/com.samsung.android.spay"

/**
 * Decides what to do with a navigation the web sdk attempted
 * @param url The url being navigated to
 * @param webView The web view attempting it, the popup's own when it came from a popup
 * @return True when the navigation was ours and must not be loaded
 */
internal fun PayButton.decidePolicyFor(url: Uri, webView: WebView?): Boolean {
    val absoluteString: String = url.toString()
    Log.d(TAG, "webactionsent $absoluteString")

    // An app that is not us wants this navigation
    if (handleNativeHandoff(url, webView)) return true

    // An acs that asks for a passkey can not run in a web view at all, whichever web view
    // it reached. It leaves for the system browser before anything tries to load it
    if (isPasskeyNavigation(absoluteString)) {
        Log.i(TAG, "a passkey navigation arrived, $absoluteString")
        webView?.stopLoading()
        startFidoAuthentication(threeDsUrl = absoluteString, redirectUrl = lastCardRedirection?.redirectUrl)
        return true
    }

    // The scheme is the only part of a url that may be case folded, so match it that way
    val isCardWebSdkCallback: Boolean = absoluteString.startsWith(cardPrefix, ignoreCase = true)
    val isWebSdkCallback: Boolean = absoluteString.startsWith(webViewScheme, ignoreCase = true)

    // The card based buttons fire their own events on the card scheme. For the card button
    // that is the same scheme the button events arrive on, so a url that is not a card event
    // falls through to the button events rather than being swallowed here
    if (isCardWebSdkCallback && handleCardWebSdkCallback(url)) return true
    if (isWebSdkCallback && handleWebSdkCallback(url)) return true

    // One of ours by scheme but not an event we know. Cancelling it keeps a `tapbuttonsdk://`
    // url from being handed to the network stack, which would only fail to load
    return isCardWebSdkCallback || isWebSdkCallback
}

/**
 * True when this navigation is an acs page that advertises a passkey challenge.
 *
 * It is matched on the url alone, unlike `requiresSystemBrowser`, because a challenge can
 * also arrive as a plain navigation with no `on3dsRedirect` announcing it and so no keyword
 * to check against
 */
internal fun isPasskeyNavigation(absoluteString: String): Boolean =
    absoluteString.contains("passkey/redirect", ignoreCase = true) ||
            absoluteString.contains("/passkey/", ignoreCase = true)

/**
 * Hands a navigation over to the app it belongs to, when it is not a page at all.
 *
 * Android only. The web sdk expects the host to resolve these, a web view can not
 * @return True when the navigation was handed off
 */
internal fun PayButton.handleNativeHandoff(url: Uri, webView: WebView?): Boolean {
    val absoluteString: String = url.toString()

    // Samsung pay leaves for its own app, and sends the payer to the store when it is missing
    if (absoluteString.startsWith(SAMSUNG_PAY_URL_PREFIX, ignoreCase = true) ||
        absoluteString.startsWith(SAMSUNG_APP_STORE_URL, ignoreCase = true)
    ) {
        webView?.post {
            webView.stopLoading()
            webView.visibility = View.GONE
        }

        try {
            val handoff: Intent = Intent.parseUri(absoluteString, Intent.URI_INTENT_SCHEME)
            handoff.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            onSuccessCalled = false
            context.startActivity(handoff)
        } catch (error: ActivityNotFoundException) {
            val install: Intent = Intent.parseUri(SAMSUNG_APP_STORE_URL, Intent.URI_INTENT_SCHEME)
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(install)
        } catch (error: URISyntaxException) {
            Log.e(TAG, "can not resolve the samsung pay handoff", error)
        }
        return true
    }

    // Careem pay authenticates on a checkout page of its own, shown the same way a 3ds page is
    if (absoluteString.startsWith(careemPayUrlHandler)) {
        // Its page wants the whole frame, not the strip a button occupies
        expandToFullScreen()
        showRedirectionView(
            Redirection(
                id = "",
                url = absoluteString,
                powered = true,
                stopRedirection = false
            )
        )
        return true
    }

    // `intent://` names an app and how to reach it. It is only ours to handle when something
    // on the device answers it, otherwise the page keeps whatever fallback it declared
    if (absoluteString.startsWith("intent://")) {
        return try {
            val handoff: Intent = Intent.parseUri(absoluteString, Intent.URI_INTENT_SCHEME)
            val resolved = context.packageManager.resolveActivity(handoff, 0)
            if (resolved == null) {
                false
            } else {
                handoff.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(handoff)
                true
            }
        } catch (error: URISyntaxException) {
            Log.e(TAG, "can not resolve intent://", error)
            false
        }
    }

    return false
}
