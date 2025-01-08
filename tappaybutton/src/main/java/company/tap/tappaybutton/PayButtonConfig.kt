package company.tap.tappaybutton

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import com.example.tappaybutton.R


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
   // private lateinit var tapKnetPay: TapKnetPay
  //  private lateinit var tapBenefitPay: TapBenefitPay
    private  var payButtonStatusDelegate: PayButtonStatusDelegate ?=null

    fun initPayButton(context: Context, configuration: HashMap<String,Any>, payButton: PayButtonType){
        when(payButton){
            PayButtonType.BENEFITPAY ->{
               /* val view = LayoutInflater.from(context).inflate(R.layout.benefit_pay_pay_button,payButtonView)
                tapBenefitPay = view.findViewById<TapBenefitPay>(R.id.benefit)
                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(context,tapBenefitPay,
                    configuration,object : TapBenefitPayStatusDelegate {
                        override fun onBenefitPayError(error: String) =  getPayButtonStatusDelegate()?.onPayButtonError(error) ?: Unit

                        override fun onBenefitPaySuccess(data: String)  =  getPayButtonStatusDelegate()?.onPayButtonSuccess(data) ?: Unit

                        override fun onBenefitPayChargeCreated(data: String) =  getPayButtonStatusDelegate()?.onPayButtonChargeCreated(data) ?: Unit

                        override fun onBenefitPayClick()  =  getPayButtonStatusDelegate()?.onPayButtonClick()?: Unit

                        override fun onBenefitPayReady()  =  getPayButtonStatusDelegate()?.onPayButtonReady()?: Unit

                        override fun onBenefitPayOrderCreated(data: String)  =  getPayButtonStatusDelegate()?.onPayButtonOrderCreated(data)?: Unit

                        override fun onBenefitPayCancel()  =  getPayButtonStatusDelegate()?.onPayButtonCancel()?: Unit

                    })*/
            }
            else ->{
         /*       val view = LayoutInflater.from(context).inflate(R.layout.knet_pay,payButtonView)
                tapKnetPay = view.findViewById<TapKnetPay>(R.id.tapKnet)
                TapKnetConfiguration.configureWithKnetDictionary(
                    context,
                    tapKnetPay,
                    configuration,
                    object : KnetPayStatusDelegate {
                        override fun onKnetError(error: String) =  getPayButtonStatusDelegate()?.onPayButtonError(error) ?: Unit

                        override fun onKnetSuccess(data: String)  =  getPayButtonStatusDelegate()?.onPayButtonSuccess(data)?: Unit

                        override fun onKnetChargeCreated(data: String) =  getPayButtonStatusDelegate()?.onPayButtonChargeCreated(data)?: Unit

                        override fun onKnetClick()  =  getPayButtonStatusDelegate()?.onPayButtonClick()?: Unit

                        override fun onKnetReady()  =  getPayButtonStatusDelegate()?.onPayButtonReady()?: Unit

                        override fun onKnetOrderCreated(data: String)  =  getPayButtonStatusDelegate()?.onPayButtonOrderCreated(data)?: Unit

                        override fun onKnetcancel() =  getPayButtonStatusDelegate()?.onPayButtonCancel()?: Unit
                    },
                    ThreeDsPayButtonType.valueOf(payButton.name.toUpperCase())
                )*/
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

