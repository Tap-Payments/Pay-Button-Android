package company.tap.tappaybutton.enums

/*
 * CallBackSchemeEnum.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/enums/CallBackSchemeEnum.swift
 *
 * Every event the web sdk fires arrives as a navigation to `<scheme>://<event>?data=...`,
 * and this names the events. Keep it in step with the iOS enum, case for case .. the
 * routers on both sides switch over exactly these.
 */

/** Every callback the web pay button and the card form can fire */
internal enum class CallBackSchemeEnum(val rawValue: String) {
    /** Fired only when the button is rendered */
    onReady("onReady"),
    /** Fired when the customer clicked the button */
    onClick("onClick"),
    /** Fired upon a successful charge */
    onSuccess("onSuccess"),
    /** An error happened with the charge */
    onError("onError"),
    /** The customer canceled the payment */
    onCancel("onCancel"),
    /**
     * The web sdk asking the button to cancel. Android only .. the web sdk fires both
     * `cancel` and `onCancel`, and the button has always answered each of them
     */
    cancel("cancel"),
    /** An order has been created */
    onOrderCreated("onOrderCreated"),
    /** The charge has been created */
    onChargeCreated("onChargeCreated"),
    /** A popup the page opened, ex google pay, has to be closed */
    onClosePopup("onClosePopup"),
    /** The card based button (click to pay, card) resized itself */
    onHeightChange("onHeightChange"),
    /** The card based button identified the brand of the typed card */
    onBinIdentification("onBinIdentification"),
    /** The customer asked to scan a card */
    onScannerClick("onScannerClick"),
    /** The customer asked to read a card over NFC */
    onNfcClick("onNfcClick"),
    /** The card form needs a 3ds page displayed to authenticate the customer */
    on3dsRedirect("on3dsRedirect"),
    /** A passkey authentication finished and the return page is handing the answer back */
    onPasskeyRedirect("onPasskeyRedirect");

    override fun toString(): String = rawValue
}

/**
 * True when the url names this event.
 *
 * Matched case sensitively, exactly as the iOS routers match. It is not a detail that can
 * be relaxed .. `onCancel` contains `cancel` once casing stops mattering, and the button
 * would answer a cancel twice, once as each event.
 */
internal fun String?.namesEvent(event: CallBackSchemeEnum): Boolean =
    this?.contains(event.rawValue) == true
