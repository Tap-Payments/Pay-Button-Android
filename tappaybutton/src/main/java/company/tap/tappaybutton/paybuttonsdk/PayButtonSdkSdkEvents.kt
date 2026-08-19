package company.tap.tappaybutton.paybuttonsdk

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.enums.CallBackSchemeEnum
import company.tap.tappaybutton.enums.namesEvent
import company.tap.tappaybutton.models.Redirection
import company.tap.tappaybutton.utils.tapExtractDataFromUrl
import org.json.JSONObject

/*
 * PayButtonSdkSdkEvents.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/PayButtonSdk/private/extensions/PayButtonSdk+SdkEvents.swift
 *
 * The events the button web sdk fires over its own scheme, and what each one means for the
 * merchant's delegate.
 */

private const val TAG = "PayButton"

/** How long a cancel waits to see whether a success is going to arrive instead */
private const val CANCEL_GRACE_MILLISECONDS: Long = 3000

/**
 * Handles the events the button web sdk fires over `<type>websdk://`
 * @param url The url the web sdk tried to navigate to
 * @return True when this was a button event and has been dealt with
 */
internal fun PayButton.handleWebSdkCallback(url: Uri): Boolean {
    val absoluteString: String = url.toString()

    return when {
        absoluteString.namesEvent(CallBackSchemeEnum.onError) -> {
            handleOnError(tapExtractDataFromUrl(url))
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onOrderCreated) -> {
            PayButtonDataConfiguration.getTapKnetListener()
                ?.onPayButtonOrderCreated(tapExtractDataFromUrl(url, shouldBase64Decode = false))
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onChargeCreated) -> {
            handleOnChargeCreated(tapExtractDataFromUrl(url))
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onSuccess) -> {
            handleOnSuccess(url)
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onReady) -> {
            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonReady()
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onClick) -> {
            handleOnClick()
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.onClosePopup) -> {
            closePopupWebView()
            true
        }

        // `onCancel` is checked before `cancel`, since the longer name contains the shorter
        // one and answering both would tell the merchant the payment was canceled twice
        absoluteString.namesEvent(CallBackSchemeEnum.onCancel) -> {
            handleOnCancel()
            true
        }

        absoluteString.namesEvent(CallBackSchemeEnum.cancel) -> {
            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()
            true
        }

        else -> false
    }
}

/**
 * Will handle and start the redirection process when called
 * @param data The data string fetched from the url parameter
 */
internal fun PayButton.handleOnChargeCreated(data: String) {
    // let us make sure we have the data we need to start such a process
    val redirection: Redirection? = Redirection.fromJson(data)
    val chargeID: String? = redirection?.id

    if (redirection == null || redirection.url == null || chargeID == null) {
        Log.e(TAG, "a charge was created we can not redirect for, $data")
        PayButtonDataConfiguration.getTapKnetListener()
            ?.onPayButtonError("Failed to start redirection process")
        return
    }

    // Benefit pay finishes inside the page itself, there is no redirection to run for it
    val isBenefitPay: Boolean = runCatching {
        JSONObject(data)
            .optJSONObject("gateway_response")
            ?.optString("name")
            .equals("BENEFITPAY", ignoreCase = true)
    }.getOrDefault(false)

    // Let us pass the charge created id for the delegate
    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonChargeCreated(data)

    // Let us see if we have to redirect or not
    if (!isBenefitPay && redirection.stopRedirection != true) {
        showRedirectionView(redirection)
    }
}

/**
 * The payment went through. The merchant hears about it, then whatever the payment put on
 * screen comes down .. this payment is over and nothing of it belongs to the next one
 */
internal fun PayButton.handleOnSuccess(url: Uri) {
    val data: String = tapExtractDataFromUrl(url)
    onSuccessCalled = true
    // Kept so a cancel arriving right behind a success knows a success already landed
    successPayload = Pair(data, true)

    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonSuccess(data)

    if (iSAppInForeground) dismissDialog()
}

/** The customer pressed the button, a new payment starts and the last one is forgotten */
internal fun PayButton.handleOnClick() {
    isBenefitPayUrlIntercepted = false
    onSuccessCalled = false
    successPayload = Pair("", false)
    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonClick()
}

/**
 * The payer backed out.
 *
 * The web sdk fires a cancel on its way out of some flows that then succeed, so the merchant
 * is only told once it is clear no success is following. A cancel that is real still takes
 * the 3ds page down straight away
 */
internal fun PayButton.handleOnCancel() {
    Handler(Looper.getMainLooper()).postDelayed({
        if (!onSuccessCalled) {
            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()
        }
    }, CANCEL_GRACE_MILLISECONDS)

    if (!(successPayload.first.isNotEmpty() && successPayload.second)) {
        dismissDialog()
    }
}

/**
 * The payment failed. Same as a success as far as the button is concerned, it is over and
 * the next one starts on a clean page
 */
internal fun PayButton.handleOnError(data: String) {
    successPayload = Pair(data, true)
    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonError(data)
}
