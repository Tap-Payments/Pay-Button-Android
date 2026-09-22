package company.tap.tappaybutton

import company.tap.tappaybutton.views.ThreeDSCallback

/*
 * PayButtonView.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Public/PayButtonView.swift
 *
 * On iOS `PayButtonView` is both the public view and where the sdk wide 3ds knobs live. On
 * Android the view is `PayButton`, which integrators already place in their layouts, so
 * only the knobs live here .. same names, same defaults, same meanings, so a change made to
 * the Swift statics has one obvious place to land.
 */

/** The sdk wide switches that decide how a 3ds authentication behaves */
object PayButtonView {

    /**
     * Whether closing the browser is taken as the authentication having finished.
     *
     * The browser never says what page it ended on, so a payer who authenticated and one who
     * gave up look the same from here. With this on, leaving the browser hands the card form
     * the return url rebuilt from the details the acs was given, and the backend decides
     * whether the authentication actually passed. Turn it off to treat every dismissal as a
     * cancel, which is stricter but leaves a completed passkey with no way home unless the
     * return page bounced to the callback first
     */
    @JvmStatic
    var threeDSAssumesReturnOnDismiss: Boolean = true

    /**
     * Whether the system browser runs as a private session during a passkey authentication.
     *
     * Kept for parity with iOS, where it drops the "<app> wants to use <domain> to sign in"
     * alert. Android has no public way to ask a browser for a private tab, so this is read
     * by nothing today .. it is here so the two sdks keep the same surface, and so the
     * Android side has somewhere to hang the behaviour if a browser ever offers it
     */
    @JvmStatic
    var threeDSPrefersEphemeralSession: Boolean = true

    /**
     * The url a finished passkey authentication comes back on.
     *
     * The acs is given this as its return url. It is what the callback is rebuilt from when
     * the return page bounces back carrying no url of its own
     */
    @JvmStatic
    var threeDSCallback: ThreeDSCallback = ThreeDSCallback.Https(host = "sdk.dev.tap.company", path = "/")
}
