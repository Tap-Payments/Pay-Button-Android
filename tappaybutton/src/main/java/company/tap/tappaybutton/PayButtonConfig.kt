package company.tap.tappaybutton

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import com.example.tappaybutton.R
import company.tap.tapWebForm.open.KnetPayStatusDelegate
import company.tap.tapWebForm.open.web_wrapper.TapKnetConfiguration
import company.tap.tapWebForm.open.web_wrapper.TapKnetPay
import company.tap.tapWebForm.open.web_wrapper.enums.ThreeDsPayButtonType
import company.tap.tapbenefitpay.open.TapBenefitPayStatusDelegate
import company.tap.tapbenefitpay.open.web_wrapper.BeneiftPayConfiguration
import company.tap.tapbenefitpay.open.web_wrapper.TapBenefitPay
import java.util.*

/**
 * Created by AhlaamK on 3/23/22.

Copyright (c) 2022    Tap Payments.
All rights reserved.
 **/
@SuppressLint("StaticFieldLeak")



/**
 * specifically for flutter
 */
object PayButtonConfig {
    private lateinit var tapKnetPay: TapKnetPay
    private lateinit var tapBenefitPay: TapBenefitPay
    private  var payButtonStatusDelegate: PayButtonStatusDelegate ?=null

    fun initPayButton(context: Context, configuration: HashMap<String,Any>, payButton:PayButtonType,payButtonView:PayButton){
        when(payButton){
            PayButtonType.BENEFITPAY ->{
                val view = LayoutInflater.from(context).inflate(R.layout.benefit_pay_pay_button,payButtonView)
                tapBenefitPay = view.findViewById<TapBenefitPay>(R.id.benefit)
                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(context,tapBenefitPay,
                    configuration,object : TapBenefitPayStatusDelegate {
                        override fun onBenefitPayError(error: String) =  getPayButtonStatusDelegate()?.onError(error) ?: Unit

                        override fun onBenefitPaySuccess(data: String)  =  getPayButtonStatusDelegate()?.onSuccess(data) ?: Unit

                        override fun onBenefitPayChargeCreated(data: String) =  getPayButtonStatusDelegate()?.onChargeCreated(data) ?: Unit

                        override fun onBenefitPayClick()  =  getPayButtonStatusDelegate()?.onClick()?: Unit

                        override fun onBenefitPayReady()  =  getPayButtonStatusDelegate()?.onReady()?: Unit

                        override fun onBenefitPayOrderCreated(data: String)  =  getPayButtonStatusDelegate()?.onOrderCreated(data)?: Unit

                        override fun onBenefitPayCancel()  =  getPayButtonStatusDelegate()?.onCancel()?: Unit

                    })
            }
            else ->{
                val view = LayoutInflater.from(context).inflate(R.layout.knet_pay,payButtonView)
                tapKnetPay = view.findViewById<TapKnetPay>(R.id.tapKnet)
                TapKnetConfiguration.configureWithKnetDictionary(
                    context,
                    tapKnetPay,
                    configuration,
                    object : KnetPayStatusDelegate {
                        override fun onKnetError(error: String) =  getPayButtonStatusDelegate()?.onError(error) ?: Unit

                        override fun onKnetSuccess(data: String)  =  getPayButtonStatusDelegate()?.onSuccess(data)?: Unit

                        override fun onKnetChargeCreated(data: String) =  getPayButtonStatusDelegate()?.onChargeCreated(data)?: Unit

                        override fun onKnetClick()  =  getPayButtonStatusDelegate()?.onClick()?: Unit

                        override fun onKnetReady()  =  getPayButtonStatusDelegate()?.onReady()?: Unit

                        override fun onKnetOrderCreated(data: String)  =  getPayButtonStatusDelegate()?.onOrderCreated(data)?: Unit

                        override fun onKnetcancel() =  getPayButtonStatusDelegate()?.onCancel()?: Unit
                    },
                    ThreeDsPayButtonType.valueOf(payButton.name.toUpperCase())
                )
            }

        }
    }

    fun addPayButtonStatusDelegate(payButtonStatusDelegate: PayButtonStatusDelegate?){
        this.payButtonStatusDelegate = payButtonStatusDelegate
    }

    fun getPayButtonStatusDelegate(): PayButtonStatusDelegate? {
        return payButtonStatusDelegate
    }






}

