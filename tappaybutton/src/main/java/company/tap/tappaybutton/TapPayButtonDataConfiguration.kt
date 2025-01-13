package company.tap.tappaybutton

import Customer
import TapAuthentication
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources

import company.tap.tapuilibrary.themekit.ThemeManager

/**
 * Created by AhlaamK on 3/23/22.

Copyright (c) 2022    Tap Payments.
All rights reserved.
 **/
@SuppressLint("StaticFieldLeak")
object RedirectDataConfiguration {

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

    fun initializeSDK(activity: Activity, configurations:  java.util.HashMap<String, Any>, tapPayButton: TapPayButton){
        TapPayButtonConfiguration.configureWithPayButtonDictionary(activity,tapPayButton,configurations)
    }


}

interface PayButtonStatusDelegate {
    fun onPayButtonSuccess(data: String)
    fun onPayButtonReady(){}
    fun onPayButtonClick(){}
    fun onPayButtonOrderCreated(data: String){}
    fun onPayButtonChargeCreated(data:String){}
    fun onPayButtonError(error: String)
    fun onPayButtoncancel(){}
   // fun onPayButtonHeightChange(heightChange:String){}
   // fun onPayButtonBindIdentification(data: String){}

}

interface ApplicationLifecycle {

    fun onEnterForeground()
    fun onEnterBackground()


}
