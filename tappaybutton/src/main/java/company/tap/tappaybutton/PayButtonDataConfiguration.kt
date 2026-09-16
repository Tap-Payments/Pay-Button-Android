package company.tap.tappaybutton


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import com.tap.commondatamodels.Customer
import com.tap.commondatamodels.TapAuthentication


import java.security.PublicKey

/**
 * Created by AhlaamK on 3/23/22.

Copyright (c) 2022    Tap Payments.
All rights reserved.
 **/
@SuppressLint("StaticFieldLeak")
object PayButtonDataConfiguration {

    private var payButtonStatusDelegate: PayButtonStatusDelegate? = null
    private var applicationLifecycle: ApplicationLifecycle? = null

    var customerExample: Customer? = null
        get() = field
        set(value) {
            field = value
        }

    var authenticationExample: TapAuthentication? = null
        get() = field
        set(value) {
            field = value
        }


    var configurationsAsHashMap: HashMap<String,Any>? = null
        get() = field
        set(value) {
            field = value
        }

    var lanuage: String? = null
        get() = field
        set(value) {
            field = value
        }









    fun setTheme(
        context: Context?,
        resources: Resources?,
        urlString: String?,
        urlPathLocal: Int?,
        fileName: String?
    ) {
        if (resources != null && urlPathLocal != null) {
            if (fileName != null && fileName.contains("dark")) {
                if (urlPathLocal != null) {
                    ThemeManager.loadTapTheme(resources, urlPathLocal, "darktheme")
                }
            } else {
                if (urlPathLocal != null) {
                    ThemeManager.loadTapTheme(resources, urlPathLocal, "lighttheme")
                }
            }
        } else if (urlString != null) {
            if (context != null) {
                println("urlString>>>" + urlString)
                ThemeManager.loadTapTheme(context, urlString, "lighttheme")
            }
        }

    }



    fun setCustomer(customer: Customer) {
        customerExample = customer
    }


    fun setTapAuthentication(tapAuthentication: TapAuthentication) {
        authenticationExample = tapAuthentication
    }

    fun addTapBenefitPayStatusDelegate(_tapCardStatuDelegate: PayButtonStatusDelegate?) {
        this.payButtonStatusDelegate = _tapCardStatuDelegate

    }
    fun addAppLifeCycle(appLifeCycle: ApplicationLifecycle?) {
        this.applicationLifecycle = appLifeCycle
    }

    fun getAppLifeCycle(): ApplicationLifecycle? {
        return this.applicationLifecycle
    }
    fun getTapKnetListener(): PayButtonStatusDelegate? {
        return payButtonStatusDelegate
    }

    fun initializeSDK(activity: Activity, configurations:  java.util.HashMap<String, Any>, payButton: PayButton, publicKey: String?,intentId:String?){
        PayButtonConfiguration.configureWithPayButtonDictionary(activity,publicKey,intentId,payButton,configurations)
    }


}

/**
 * The events the pay button reports back.
 *
 * Mirrors Pay-Button-iOS Logic/Shared/Public/PayButtonDelegate.swift. Every method there
 * is optional, so every method here that is not already required carries a default body ..
 * a new event added on the iOS side can be added here without breaking an integrator who
 * has not implemented it yet.
 */
interface PayButtonStatusDelegate {
    /** Fired whenever the charge is successful, carrying the charge as json */
    fun onPayButtonSuccess(data: String)
    /** Fired whenever the button is rendered and loaded */
    fun onPayButtonReady(){}
    /** Fired whenever the customer clicked the button */
    fun onPayButtonClick(){}
    /** Fired whenever the order is created, carrying the order id */
    fun onPayButtonOrderCreated(data: String){}
    /** Fired whenever the charge is created, carrying the charge as json */
    fun onPayButtonChargeCreated(data:String){}
    /** Fired whenever there is an error related to the connectivity or the apis */
    fun onPayButtonError(error: String)
    /** Fired whenever the customer cancels the payment */
    fun onPayButtoncancel(){}
    /** Fired by the card based buttons whenever the rendered form changes its size */
    fun onPayButtonHeightChange(heightChange:String){}
    /** Fired by the card based buttons once the brand of the typed card is identified */
    fun onPayButtonBindIdentification(data: String){}
    /**
     * Fired by the card form when the customer has to be authenticated on a 3ds page.
     * The button runs the authentication itself, this is for integrators who drive their
     * own ui from it. Mirrors `onThreeDSRedirect(data:)`
     * @param data json describing the 3ds page to be displayed
     */
    fun onPayButtonThreeDSRedirect(data: String){}
    /** Fired when the customer asks to scan a card. Present your card scanner from here */
    fun onPayButtonScannerClick(){}
    /** Fired when the customer asks to read a card over NFC. Start your NFC reader from here */
    fun onPayButtonNfcClick(){}

}

interface ApplicationLifecycle {

    fun onEnterForeground()
    fun onEnterBackground()


}
