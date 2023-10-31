package company.tap.tappaybutton

import Customer
import TapAuthentication
import TapCardConfigurations
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import company.tap.tapWebForm.R
import company.tap.tapWebForm.open.KnetPayStatusDelegate
import company.tap.tapWebForm.open.web_wrapper.TapKnetConfiguration
import company.tap.tapWebForm.open.web_wrapper.TapKnetPay
import company.tap.tapWebForm.open.web_wrapper.enums.ThreeDsPayButtonType
import company.tap.tapcardformkit.open.TapBenefitPayStatusDelegate
import company.tap.tapcardformkit.open.web_wrapper.BeneiftPayConfiguration
import company.tap.tapcardformkit.open.web_wrapper.TapBenefitPay
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tapuilibrary.themekit.ThemeManager
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by AhlaamK on 3/23/22.

Copyright (c) 2022    Tap Payments.
All rights reserved.
 **/
@SuppressLint("StaticFieldLeak")
object PayButtonConfig {
    private lateinit var tapKnetPay: TapKnetPay
    private lateinit var tapBenefitPay: TapBenefitPay
    private lateinit  var payButtonStatusDelegate: PayButtonStatusDelegate

    fun initPayButton(activity: Activity,
                      configuration: HashMap<String,Any>, payButton:PayButtonType, view:PayButton){
        if (::payButtonStatusDelegate.isInitialized) this.addPayButtonStatusDelegate(payButtonStatusDelegate)
        when(payButton){
            PayButtonType.BENEFIT_PAY ->{
                tapBenefitPay = TapBenefitPay(activity)
                view.addView(tapBenefitPay)
                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(activity,tapBenefitPay,
                    configuration,object : TapBenefitPayStatusDelegate {
                        override fun onError(error: String) = payButtonStatusDelegate.onError(error)

                        override fun onSuccess(data: String)  = payButtonStatusDelegate.onSuccess(data)

                        override fun onChargeCreated(data: String) = payButtonStatusDelegate.onChargeCreated(data)

                        override fun onClick()  = payButtonStatusDelegate.onClick()

                        override fun onReady()  = payButtonStatusDelegate.onReady()

                        override fun onOrderCreated(data: String)  = payButtonStatusDelegate.onOrderCreated(data)

                        override fun onCancel()  = payButtonStatusDelegate.onCancel()

                    })
            }
            PayButtonType.KNET,PayButtonType.BENEFIT,PayButtonType.PAYPAL,PayButtonType.TABBY,PayButtonType.FAWRY-> {
                tapKnetPay = TapKnetPay(activity)
                view.addView(tapKnetPay)
                TapKnetConfiguration.configureWithKnetDictionary(
                    activity,
                    tapKnetPay,
                    configuration,
                    object : KnetPayStatusDelegate {
                        override fun onError(error: String) = payButtonStatusDelegate.onError(error)

                        override fun onSuccess(data: String)  = payButtonStatusDelegate.onSuccess(data)

                        override fun onChargeCreated(data: String) = payButtonStatusDelegate.onChargeCreated(data)

                        override fun onClick()  = payButtonStatusDelegate.onClick()

                        override fun onReady()  = payButtonStatusDelegate.onReady()

                        override fun onOrderCreated(data: String)  = payButtonStatusDelegate.onOrderCreated(data)

                        override fun cancel() = payButtonStatusDelegate.onCancel()
                    },
                    ThreeDsPayButtonType.valueOf(payButton.name.toUpperCase())
                )
            }
            else ->{
                Toast.makeText(activity,"Check your Payment Button name", Toast.LENGTH_SHORT).show()

            }

        }
        TapKnetConfiguration.configureWithKnetDictionary(activity,tapKnetPay,configuration)
    }

    fun addPayButtonStatusDelegate(payButtonStatusDelegate: PayButtonStatusDelegate){
        this.payButtonStatusDelegate = payButtonStatusDelegate
    }






}

