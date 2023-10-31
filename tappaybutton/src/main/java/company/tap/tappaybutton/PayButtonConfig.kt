package company.tap.tappaybutton

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import company.tap.tapWebForm.open.KnetPayStatusDelegate
import company.tap.tapWebForm.open.web_wrapper.TapKnetConfiguration
import company.tap.tapWebForm.open.web_wrapper.TapKnetPay
import company.tap.tapWebForm.open.web_wrapper.enums.ThreeDsPayButtonType
import company.tap.tapcardformkit.open.TapBenefitPayStatusDelegate
import company.tap.tapcardformkit.open.web_wrapper.BeneiftPayConfiguration
import company.tap.tapcardformkit.open.web_wrapper.TapBenefitPay
import java.util.*

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

    fun initPayButton(context: Context, configuration: HashMap<String,Any>, payButton:PayButtonType,payButtonView:PayButton){
        when(payButton){
            PayButtonType.BENEFIT_PAY ->{
                tapBenefitPay = TapBenefitPay(context)
                tapBenefitPay.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                val layout2 = LinearLayout(context)
                layout2.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layout2.orientation = LinearLayout.VERTICAL
                layout2.post {
                    layout2.addView(tapBenefitPay)
                    layout2.invalidate()
                }
                payButtonView.post {
                    payButtonView.addView(layout2)

                }

                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(context,tapBenefitPay,
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
                tapKnetPay = TapKnetPay(context)
                tapKnetPay.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                val layout2 = LinearLayout(context)
                layout2.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                layout2.orientation = LinearLayout.VERTICAL
                layout2.post {
                    layout2.addView(tapKnetPay)
                    layout2.invalidate()
                }
                payButtonView.post {
                    payButtonView.addView(layout2)

                }
                payButtonView.addView(layout2)
                TapKnetConfiguration.configureWithKnetDictionary(
                    context,
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
                Toast.makeText(context,"Check your Payment Button name", Toast.LENGTH_SHORT).show()

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

