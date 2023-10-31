

/**
 *   Created by AhlaamK on 10/26/23, 11:18 AM
 *   Copyright (c) 2023 .
 *   All rights reserved Tap Payments.
 **
 */

package company.tap.tappaybutton
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.tappaybutton.R
import company.tap.tapWebForm.open.KnetPayStatusDelegate
import company.tap.tapWebForm.open.web_wrapper.TapKnetConfiguration
import company.tap.tapWebForm.open.web_wrapper.TapKnetPay
import company.tap.tapWebForm.open.web_wrapper.enums.ThreeDsPayButtonType
import company.tap.tapcardformkit.open.TapBenefitPayStatusDelegate
import company.tap.tapcardformkit.open.web_wrapper.BeneiftPayConfiguration
import company.tap.tapcardformkit.open.web_wrapper.TapBenefitPay

class PayButton :LinearLayout {
    lateinit var tapKnetPay: TapKnetPay
    lateinit var tapBenefitPay: TapBenefitPay

    /**
     * Simple constructor to use when creating a TapPayCardSwitch from code.
     *  @param context The Context the view is running in, through which it can
     *  access the current theme, resources, etc.
     **/
    constructor(context: Context) : super(context)

    /**
     *  @param context The Context the view is running in, through which it can
     *  access the current theme, resources, etc.
     *  @param attrs The attributes of the XML Button tag being used to inflate the view.
     *
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)


    fun initPayButton(
        context: Context,
        configuration: HashMap<String, Any>,
        payButton: PayButtonType,
        payButtonStatusDelegate: PayButtonStatusDelegate
    ){
        when(payButton){
            PayButtonType.BENEFIT_PAY ->{
                tapBenefitPay = TapBenefitPay(context)
                tapBenefitPay.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                this.post(Runnable {
                    this.addView(tapBenefitPay)
                    this.invalidate()
                })
                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(context,tapBenefitPay,
                    configuration,object :TapBenefitPayStatusDelegate{
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
                    tapKnetPay = TapKnetPay(context)
                    tapKnetPay.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                this.post(Runnable {
                    this.addView(tapKnetPay)
                    this.invalidate()
                })
                    TapKnetConfiguration.configureWithKnetDictionary(
                        context,
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
                Toast.makeText(context,"Check your Payment Button name",Toast.LENGTH_SHORT).show()

            }

            }
        }

}