package company.tap.tappaybutton.paybuttonsdk

import android.util.Log
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.views.CardNfcReader

/*
 * PayButtonSdkNfc.kt
 *
 * The card form asks for a contactless read, the sdk answers with one.
 *
 * There is no iOS counterpart to mirror .. iOS fires `onNfcClick` and leaves it to the merchant.
 * This follows Card-Android, which answers the same event from the same web sdk with the same
 * reader, and it lands where the scanner lands: `window.fillCardInputs`.
 */

private const val TAG = "PayButton"

/**
 * Reads a card over nfc and types it into the form.
 *
 * A device with no nfc, or with it switched off, is reported as an error rather than opening a
 * screen that could never read anything
 */
internal fun PayButton.readCardOverNfc() {
    if (!CardNfcReader.isAvailable(context)) {
        Log.i(TAG, "this device has no nfc")
        PayButtonDataConfiguration.getTapKnetListener()
            ?.onPayButtonError("{\"error\":\"NFC is not supported on this device\"}")
        return
    }

    Log.i(TAG, "raising the nfc prompt")
    CardNfcReader.start(context, PayButtonNfcListener(this))
}

/** Receives what was read over nfc, the same shape as the scanner's listener */
internal class PayButtonNfcListener(
    private val payButton: PayButton
) : CardNfcReader.NfcListener {

    /** A card was read, type it into the form */
    override fun cardRead(cardNumber: String, cardHolderName: String, expiryDate: String) {
        val month: String = expiryDate.substringBefore('/', "").trim()
        val year: String = expiryDate.substringAfter('/', "").trim()

        payButton.fillScannedCard(
            cardNumber = cardNumber,
            expiryMonth = month,
            expiryYear = year,
            cardHolderName = cardHolderName
        )
    }

    /** The payer put the phone down without a card being read. The form is left as it was */
    override fun nfcCanceled() {
        Log.i(TAG, "the payer closed the nfc reader")
    }

    /** Nfc could not be used at all */
    override fun nfcFailed(error: String) {
        PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonError(error)
    }
}
