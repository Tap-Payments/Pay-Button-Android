/**
 *   Created by AhlaamK on 10/26/23, 11:18 AM
 *   Copyright (c) 2023 .
 *   All rights reserved Tap Payments.
 **
 */

package company.tap.tappaybutton

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import company.tap.tapWebForm.open.KnetPayStatusDelegate
import company.tap.tapWebForm.open.web_wrapper.TapKnetConfiguration
import company.tap.tapWebForm.open.web_wrapper.TapKnetPay
import company.tap.tapWebForm.open.web_wrapper.enums.ThreeDsPayButtonType
import company.tap.tapbenefitpay.open.TapBenefitPayStatusDelegate
import company.tap.tapbenefitpay.open.web_wrapper.BeneiftPayConfiguration
import company.tap.tapbenefitpay.open.web_wrapper.TapBenefitPay

class PayButton : LinearLayout {
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
        payButtonStatusDelegate: PayButtonStatusDelegate,
    ) {
        when (payButton) {
            PayButtonType.BENEFITPAY -> {

                tapBenefitPay = TapBenefitPay(context)
                tapBenefitPay.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                this.post(Runnable {
                    this.addView(tapBenefitPay)
                    this.invalidate()
                })
                BeneiftPayConfiguration.configureWithTapBenfitPayDictionaryConfiguration(context,
                    tapBenefitPay,
                    configuration,
                    object : TapBenefitPayStatusDelegate {
                        override fun onBenefitPayError(error: String) = payButtonStatusDelegate.onError(error)

                        override fun onBenefitPaySuccess(data: String) =
                            payButtonStatusDelegate.onSuccess(data)

                        override fun onBenefitPayChargeCreated(data: String) =
                            payButtonStatusDelegate.onChargeCreated(data)

                        override fun onBenefitPayClick() = payButtonStatusDelegate.onClick()

                        override fun onBenefitPayReady() = payButtonStatusDelegate.onReady()

                        override fun onBenefitPayOrderCreated(data: String) =
                            payButtonStatusDelegate.onOrderCreated(data)

                        override fun onBenefitPayCancel() = payButtonStatusDelegate.onCancel()

                    })
            }
            else ->  {

                tapKnetPay = TapKnetPay(context)
                tapKnetPay.layoutParams =
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                this.post(Runnable {
                    this.removeAllViews()
                    this.addView(tapKnetPay)
                    this.invalidate()
                })
                TapKnetConfiguration.configureWithKnetDictionary(
                    context,
                    tapKnetPay,
                    configuration,
                    object : KnetPayStatusDelegate {
                        override fun onKnetError(error: String) = payButtonStatusDelegate.onError(error)

                        override fun onKnetSuccess(data: String) =
                            payButtonStatusDelegate.onSuccess(data)

                        override fun onKnetChargeCreated(data: String) =
                            payButtonStatusDelegate.onChargeCreated(data)

                        override fun onKnetClick() = payButtonStatusDelegate.onClick()

                        override fun onKnetReady() = payButtonStatusDelegate.onReady()

                        override fun onKnetOrderCreated(data: String) =
                            payButtonStatusDelegate.onOrderCreated(data)

                        override fun onKnetcancel() = payButtonStatusDelegate.onCancel()
                    },
                    ThreeDsPayButtonType.valueOf(payButton.name.toUpperCase())
                )
            }

        }
    }

}