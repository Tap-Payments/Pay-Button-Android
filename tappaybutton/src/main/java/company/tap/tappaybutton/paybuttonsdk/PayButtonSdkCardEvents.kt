package company.tap.tappaybutton.paybuttonsdk

import android.net.Uri
import android.util.Log
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.enums.CallBackSchemeEnum
import company.tap.tappaybutton.enums.namesEvent
import company.tap.tappaybutton.utils.tapExtractDataFromUrl

/*
 * PayButtonSdkCardEvents.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/PayButtonSdk/private/extensions/PayButtonSdk+CardEvents.swift
 *
 * The events the card form fires over `tapCardWebSDK://`, ex the form resizing itself or a
 * card being identified. Its own scheme, so its own handler.
 */

private const val TAG = "PayButton"

/**
 * Handles the events fired by the card based buttons (click to pay, card) over the
 * `tapCardWebSDK://` scheme
 * @param url The url the web sdk tried to navigate to
 * @return True when this was a card event and has been dealt with
 */
internal fun PayButton.handleCardWebSdkCallback(url: Uri): Boolean {
    val absoluteString: String = url.toString()

    return when {
        absoluteString.namesEvent(CallBackSchemeEnum.onHeightChange) -> {
            // The height comes as a plain number, not as a base64 encoded json
            val reportedHeight: String = tapExtractDataFromUrl(url, shouldBase64Decode = false)
            val height: Int? = reportedHeight.toDoubleOrNull()?.toInt()
            if (height != null) {
                // Resize ourselves, then tell the merchant the height we settled on
                updateHeight(height)
                PayButtonDataConfiguration.getTapKnetListener()
                    ?.onPayButtonHeightChange(height.toString())
            } else {
                Log.w(TAG, "the card form reported a height we can not read, $reportedHeight")
            }
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onBinIdentification) -> {
            PayButtonDataConfiguration.getTapKnetListener()
                ?.onPayButtonBindIdentification(tapExtractDataFromUrl(url))
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onScannerClick) -> {
            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonScannerClick()
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onNfcClick) -> {
            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonNfcClick()
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onPasskeyRedirect) -> {
            // The browser normally takes this one, ThreeDSPasskeyCallbackActivity claims the
            // scheme and it never reaches the web view. It lands here when the page bounces
            // to it from inside the form
            Log.i(TAG, "a passkey callback arrived in the web view, $absoluteString")
            threeDSPasskeySession?.handleCallback(url)
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.on3dsRedirect) -> {
            handleCardRedirection(tapExtractDataFromUrl(url))
            true
        }

        else -> false
    }
}
