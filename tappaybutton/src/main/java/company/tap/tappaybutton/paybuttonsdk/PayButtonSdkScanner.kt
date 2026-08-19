package company.tap.tappaybutton.paybuttonsdk

import android.util.Log
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.views.CardScannerActivity
import org.json.JSONObject

/*
 * PayButtonSdkScanner.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/PayButtonSdk/private/extensions/PayButtonSdk+Scanner.swift
 *
 * The card form asks for the camera, and something has to type back what was read.
 *
 * iOS answers the whole event itself: it presents its own scanner and fills the form. Android
 * answers the half that has no owner .. the merchant still runs the scanner, because the camera
 * and its permission belong to the host app here, but until now a scanned card had nowhere to
 * go. `onPayButtonScannerClick` fired and the result could not be handed back.
 */

private const val TAG = "PayButton"

/**
 * Types a scanned card into the form.
 *
 * Call it from `onPayButtonScannerClick` once your scanner has read a card. Every field is
 * optional .. a scanner that reads only the number sends only the number, and the payer fills
 * in the rest.
 *
 * @param cardNumber The PAN, digits only
 * @param expiryMonth Two digits, ex `04`
 * @param expiryYear Two digits, ex `27`
 * @param cvv The security code, when the scanner read one
 * @param cardHolderName The name on the card, when the scanner read one
 */
fun PayButton.fillScannedCard(
    cardNumber: String? = null,
    expiryMonth: String? = null,
    expiryYear: String? = null,
    cvv: String? = null,
    cardHolderName: String? = null
) {
    // The form wants one string for the expiry, and nothing at all rather than a lone slash
    val expiry: String = if (expiryMonth.isNullOrEmpty()) "" else "$expiryMonth/${expiryYear.orEmpty()}"

    val scanned = JSONObject().apply {
        put("cardNumber", cardNumber.orEmpty())
        put("expiryDate", expiry)
        put("cvv", cvv.orEmpty())
        put("cardHolderName", cardHolderName.orEmpty())
    }

    val javaScript = """
        (function() {
            var scanned = $scanned;
            if (typeof window.fillCardInputs === 'function') {
                window.fillCardInputs(scanned);
                return 'window';
            }
            return 'none';
        })()
    """.trimIndent()

    Log.i(TAG, "filling the form with the scanned card")
    evaluateOnWebSdk(javaScript) { result ->
        // `none` means the page has no such function, so the scan has nowhere to go
        Log.i(TAG, "fillCardInputs handled by $result")
    }
}


/**
 * Asks for the camera and shows the scanner.
 *
 * Mirrors `scanCard()`. The permission is asked for by the scanner screen itself rather than
 * here, because a view has nothing to request one with, and the screen that needs the camera is
 * already an Activity
 */
internal fun PayButton.scanCard() {
    Log.i(TAG, "opening the card scanner")
    CardScannerActivity.start(context, PayButtonScanListener(this))
}

/**
 * Receives what the scanner read.
 *
 * The iOS side writes this as a conformance on the button itself. Kotlin cannot add one from
 * the outside, so it is a small object holding the button, the same shape as the passkey one
 */
internal class PayButtonScanListener(
    private val payButton: PayButton
) : CardScannerActivity.ScanListener {

    /** A card was read, type it into the form */
    override fun cardScanned(cardNumber: String, cardHolderName: String, expiryDate: String) {
        // The kit reports the expiry as one string, ex 04/27, and the form wants the two halves
        val month: String = expiryDate.substringBefore('/', "").trim()
        val year: String = expiryDate.substringAfter('/', "").trim()

        payButton.fillScannedCard(
            cardNumber = cardNumber,
            expiryMonth = month,
            expiryYear = year,
            cardHolderName = cardHolderName
        )
    }

    /** The payer closed the scanner without one being read. The form is left as it was */
    override fun scannerCanceled() {
        Log.i(TAG, "the payer closed the scanner")
    }

    /** The camera could not be used at all */
    override fun scannerFailed(error: String) {
        PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonError(error)
    }
}
