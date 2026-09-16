package company.tap.tappaybutton.views

/*
 * ThreeDSReturn.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/views/ThreeDSReturn.swift
 *
 * What a finished 3ds authentication is expected to come back on, and what can go wrong on
 * the way there. Both belong to ThreeDSPasskeySession, which is the only thing that runs a
 * passkey now.
 */

/**
 * Names the url a finished authentication comes back on.
 *
 * The acs is given this as its return url. On iOS the sdk recognises it among the redirects
 * safari reports; a custom tab reports nothing at all, so here it is the url the return page
 * is rebuilt from when the callback carries no url of its own
 */
sealed class ThreeDSCallback {

    /**
     * The https return url, ex `https://sdk.dev.tap.company/`. Host and path are what
     * identify it, the query is where the acs puts its answer so it is never compared
     */
    data class Https(val host: String, val path: String) : ThreeDSCallback()

    /** The return url this describes */
    val httpsReturnUrl: String?
        get() = when (this) {
            is Https -> "https://$host$path"
        }
}

/** Errors surfaced while running the 3ds process in the system browser */
sealed class ThreeDSSessionError(message: String) : Exception(message) {
    /** The payer dismissed the browser before finishing the authentication */
    object CanceledByUser : ThreeDSSessionError("The payer closed the browser")
    /** The session could not be started, ex no activity to launch the browser from */
    object FailedToStart : ThreeDSSessionError("The browser could not be started")
    /** No return url was named, so there is nothing to rebuild the answer from */
    object ReturnUrlUnavailable : ThreeDSSessionError("No return url was named")
    /** The ACS page url could not be parsed */
    object InvalidThreeDSUrl : ThreeDSSessionError("The three ds url could not be parsed")
}
