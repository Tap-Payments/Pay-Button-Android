package company.tap.tappaybutton

import Customer
import TapAuthentication
import TapCardConfigurations
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import company.tap.tapWebForm.R
import company.tap.tapWebForm.open.DataConfiguration
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
    private  var payButtonStatusDelegate: PayButtonStatusDelegate ?=null

    fun initPayButton(activity: Activity, configuration: HashMap<String,Any>, payButton:PayButtonType, view:PayButton){
      addPayButtonStatusDelegate(payButtonStatusDelegate)
        when(payButton){
            PayButtonType.BENEFIT_PAY ->{
                tapBenefitPay = TapBenefitPay(activity)
                tapBenefitPay.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                view.addView(tapBenefitPay)
                view.invalidate()
                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(activity,tapBenefitPay,
                    configuration,object : TapBenefitPayStatusDelegate {
                        override fun onError(error: String) =  getPayButtonStatusDelegate()?.onError(error) ?: Unit


                        override fun onSuccess(data: String)  =  getPayButtonStatusDelegate()?.onSuccess(data) ?: Unit

                        override fun onChargeCreated(data: String) =  getPayButtonStatusDelegate()?.onChargeCreated(data) ?: Unit

                        override fun onClick()  =  getPayButtonStatusDelegate()?.onClick()?: Unit

                        override fun onReady()  =  getPayButtonStatusDelegate()?.onReady()?: Unit

                        override fun onOrderCreated(data: String)  =  getPayButtonStatusDelegate()?.onOrderCreated(data)?: Unit

                        override fun onCancel()  =  getPayButtonStatusDelegate()?.onCancel()?: Unit

                    })
            }
            PayButtonType.KNET,PayButtonType.BENEFIT,PayButtonType.PAYPAL,PayButtonType.TABBY,PayButtonType.FAWRY-> {
                tapKnetPay = TapKnetPay(activity)
                tapKnetPay.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                view.addView(tapKnetPay)
                view.invalidate()
                TapKnetConfiguration.configureWithKnetDictionary(
                    activity,
                    tapKnetPay,
                    configuration,
                    object : KnetPayStatusDelegate {
                        override fun onError(error: String) =  getPayButtonStatusDelegate()?.onError(error) ?: Unit

                        override fun onSuccess(data: String)  =  getPayButtonStatusDelegate()?.onSuccess(data)?: Unit

                        override fun onChargeCreated(data: String) =  getPayButtonStatusDelegate()?.onChargeCreated(data)?: Unit

                        override fun onClick()  =  getPayButtonStatusDelegate()?.onClick()?: Unit

                        override fun onReady()  =  getPayButtonStatusDelegate()?.onReady()?: Unit

                        override fun onOrderCreated(data: String)  =  getPayButtonStatusDelegate()?.onOrderCreated(data)?: Unit

                        override fun cancel() =  getPayButtonStatusDelegate()?.onCancel()?: Unit
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

    fun addPayButtonStatusDelegate(payButtonStatusDelegate: PayButtonStatusDelegate?){
        this.payButtonStatusDelegate = payButtonStatusDelegate
    }

    fun getPayButtonStatusDelegate(): PayButtonStatusDelegate? {
        return payButtonStatusDelegate
    }






}

