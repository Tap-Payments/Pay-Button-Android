package company.tap.tappaybutton.paybuttonsdk

import android.net.Uri
import android.util.Log
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.PayButtonView
import company.tap.tappaybutton.PaymentFlow
import company.tap.tappaybutton.enums.cardPrefix
import company.tap.tappaybutton.models.CardRedirection
import company.tap.tappaybutton.models.Redirection
import company.tap.tappaybutton.views.ThreeDSPasskeySession

/*
 * PayButtonSdkThreeDS.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/PayButtonSdk/private/extensions/PayButtonSdk+ThreeDS.swift
 *
 * Everything that runs an authentication .. the redirection page the shared buttons use,
 * the card form's own 3ds page, the passkey that has to leave the web view for the browser,
 * and handing the answer back to whichever form asked for it.
 *
 * Swift grows a type with `extension`, Kotlin with extension functions on it, so the file
 * split is the same on both sides and a change made to the Swift file has one file here to
 * land in. What Kotlin can not do is add an interface to a type from the outside, so the
 * passkey delegate is a small class at the bottom rather than a conformance.
 */

private const val TAG = "PayButton"

/** The event name the card form fires when a passkey has to leave the web view */
internal const val PASSKEY_MARKER = "passkey"
/** The query key an acs uses when it is a payer authentication that can carry a passkey */
internal const val AUTH_PAYER_KEYWORD = "auth_payer"

//MARK: - The redirection page the shared buttons use

/**
 * Will create a redirection view and display it on top of the current screen
 * @param redirection The redirection model that contains the redirection url and the
 * redirection finished keyword
 */
internal fun PayButton.showRedirectionView(redirection: Redirection) {
    // The 3ds page reads the details off the button, the same way the iOS view is handed them
    PayButton.threeDsResponse = redirection
    navigateTo3dsActivity(PaymentFlow.PAYMENTBUTTON.name)
}

//MARK: - The card form's own 3ds page

/**
 * Starts the 3ds authentication the card form asked for.
 *
 * The card form behind the button is the same web sdk Card-iOS renders, so the contract is
 * the same: load `threeDsUrl`, watch the loaded pages for `keyword`, then hand the whole
 * url back.
 * @param data The decoded json the card sdk sent with `on3dsRedirect`
 */
internal fun PayButton.handleCardRedirection(data: String) {
    // Let the merchant see it either way, some integrators drive their own ui from it
    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonThreeDSRedirect(data)

    // Make sure we have what it takes to run the process. A payload that does not decode is
    // an answer, not a crash .. the old code let Gson and the base64 decoder throw straight
    // out of shouldOverrideUrlLoading, which took the host app down with it
    val cardRedirection: CardRedirection? = CardRedirection.fromJson(data)
    val threeDsUrl: String? = cardRedirection?.threeDsUrl?.takeIf { it.isNotEmpty() }

    if (cardRedirection == null || threeDsUrl == null || cardRedirection.redirectUrl == null) {
        Log.e(TAG, "the card form asked for an authentication we can not start, $data")
        PayButtonDataConfiguration.getTapKnetListener()
            ?.onPayButtonError("{\"error\":\"Failed to start authentication process\"}")
        return
    }

    // Kept so a passkey that arrives later as a plain navigation still knows the return url
    lastCardRedirection = cardRedirection

    // An ACS that asks for a passkey can not run in a web view, it has no
    // navigator.credentials. Hand the whole process over to the system browser instead
    if (requiresSystemBrowser(threeDsUrl, cardRedirection.keyword)) {
        startFidoAuthentication(cardRedirection)
        return
    }

    PayButton.threeDsResponseCardPayButtons = cardRedirection
    navigateTo3dsActivity(PaymentFlow.CARDPAY.name)
}

/**
 * Tells the card form the payer finished authenticating.
 *
 * The button page wraps the card in an iframe, and its own `window.loadAuthentication` posts
 * through the button's iframe events which need an `iframeId` the mobile url never carries.
 * `window.CardSDK` talks to the card iframe directly, so prefer it and keep the others as
 * fallbacks. `loadAuthernticate`, misspelled, is what older button bundles expose and is
 * tried last so an app on an older bundle still finishes.
 * @param redirectionUrl The whole url the 3ds page landed on
 */
internal fun PayButton.passCardAuthenticationToSDK(redirectionUrl: String) {
    // Keep the url safe to drop inside a single quoted js string
    val escapedUrl: String = redirectionUrl
        .replace("\\", "\\\\")
        .replace("'", "\\'")

    val javaScript = """
        (function() {
            var authenticationUrl = '$escapedUrl';
            if (window.CardSDK && typeof window.CardSDK.loadAuthentication === 'function') {
                window.CardSDK.loadAuthentication(authenticationUrl);
                return 'CardSDK';
            }
            if (typeof window.loadAuthentication === 'function') {
                window.loadAuthentication(authenticationUrl);
                return 'window';
            }
            if (typeof window.loadAuthernticate === 'function') {
                window.loadAuthernticate(authenticationUrl);
                return 'legacy';
            }
            return 'none';
        })()
    """.trimIndent()

    Log.i(TAG, "handing the card form $redirectionUrl")
    evaluateOnWebSdk(javaScript) { result ->
        // `none` means the page has neither function, ex the button page reloaded and took
        // the card iframe with it, so there is nobody left to finish the authentication
        Log.i(TAG, "loadAuthentication handled by $result")
    }
}

/** The payer backed out of the 3ds page */
internal fun PayButton.handleCardAuthenticationCanceled() {
    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()
    val javaScript = """
        (function() {
            if (window.CardSDK && typeof window.CardSDK.cancelAuthentication === 'function') {
                window.CardSDK.cancelAuthentication();
                return 'CardSDK';
            }
            if (typeof window.cancel === 'function') {
                window.cancel();
                return 'window';
            }
            return 'none';
        })()
    """.trimIndent()
    evaluateOnWebSdk(javaScript, null)
}

//MARK: - The passkey that has to leave the web view

/**
 * Decides whether the authentication has to leave the web view. An ACS url that advertises
 * a passkey challenge can not run inside a WebView, it does not expose
 * `navigator.credentials`
 * @param threeDsUrl The ACS page coming from the redirection details
 * @param key The keyword the card form named
 * @return True when the process belongs in the system browser
 */
internal fun requiresSystemBrowser(threeDsUrl: String?, key: String?): Boolean {
    if (threeDsUrl == null) return false
    if (key == null) return false
    return threeDsUrl.contains(PASSKEY_MARKER, ignoreCase = true) && key == AUTH_PAYER_KEYWORD
}

/**
 * Runs the authentication inside the system browser, which unlike a WebView can execute
 * `navigator.credentials` and therefore serve a passkey challenge
 * @param cardRedirection The validated redirection details
 */
internal fun PayButton.startFidoAuthentication(cardRedirection: CardRedirection) {
    startFidoAuthentication(cardRedirection.threeDsUrl, cardRedirection.redirectUrl)
}

/**
 * Runs the authentication inside the system browser
 * @param threeDsUrl The acs page to load
 * @param redirectUrl The https return url the callback is mapped back onto. Null when the
 * challenge arrived as a plain navigation and no `on3dsRedirect` announced it first
 */
internal fun PayButton.startFidoAuthentication(threeDsUrl: String?, redirectUrl: String?) {
    // One authentication, one browser. The card form can announce the same challenge more
    // than once, and starting a second session would put a second browser over the first ..
    // finishing then takes only the newest one down and leaves the payer looking at the one
    // underneath
    if (threeDSPasskeySession != null) {
        Log.i(TAG, "a passkey is already running, ignoring this one")
        Log.i(TAG, "it was for ${threeDsUrl ?: "nil"}")
        return
    }

    Log.i(TAG, "running the passkey in the system browser")
    val passkeySession = ThreeDSPasskeySession()
    passkeySession.delegate = PayButtonPasskeyDelegate(this)
    threeDSPasskeySession = passkeySession

    // The return page bounces to the scheme the card form fires its events on, ex
    // tapCardWebSDK://onPasskeyRedirect?data=..., which ThreeDSPasskeyCallbackActivity claims
    val callbackScheme: String = cardPrefix.replace("://", "")

    // A passkey that arrived as a bare navigation carries no redirection details, so fall
    // back to the return url the configured callback already names
    passkeySession.start(
        threeDsUrl = threeDsUrl,
        redirectUrl = redirectUrl ?: PayButtonView.threeDSCallback.httpsReturnUrl,
        callbackScheme = callbackScheme,
        keyword = lastCardRedirection?.keyword,
        context = context
    )
}

/**
 * Receives the outcome of a passkey authentication that ran in the system browser.
 *
 * The iOS side writes this as a conformance on the button itself. Kotlin can not add one
 * from the outside, so it is a small object holding the button instead, which keeps the
 * logic in this file where the Swift keeps it
 */
internal class PayButtonPasskeyDelegate(
    private val payButton: PayButton
) : ThreeDSPasskeySession.Delegate {

    /** The callback the browser came back on, before anything is read out of it */
    override fun onReachedCallback(session: ThreeDSPasskeySession, callbackUrl: Uri) {
        Log.i(TAG, "the passkey came back on $callbackUrl")
    }

    /** The browser came back, hand the url over to the card form */
    override fun onSucceeded(session: ThreeDSPasskeySession, redirectionUrl: String) {
        payButton.threeDSPasskeySession = null
        payButton.passCardAuthenticationToSDK(redirectionUrl)
    }

    /** The payer closed the browser before finishing the authentication */
    override fun onCanceled(session: ThreeDSPasskeySession) {
        payButton.threeDSPasskeySession = null
        PayButtonDataConfiguration.getTapKnetListener()
            ?.onPayButtonError("Payer canceled three ds process")
    }

    /** The process could not be completed, treat it the same as a failed start */
    override fun onFailed(session: ThreeDSPasskeySession, error: Throwable) {
        payButton.threeDSPasskeySession = null
        PayButtonDataConfiguration.getTapKnetListener()
            ?.onPayButtonError("Failed to start authentication process")
    }
}
