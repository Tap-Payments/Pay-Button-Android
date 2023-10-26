/**
 *   Created by AhlaamK on 10/26/23, 11:18 AM
 *   Copyright (c) 2023 .
 *   All rights reserved Tap Payments.
 **
 */

package company.tap.tappaybuttons

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout

class PayButton :LinearLayout {
    val mainView by lazy { findViewById<LinearLayout>(R.id.mainLL) }
    lateinit var buttonView: View
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

    init {
        View.inflate(context, R.layout.pay_button_layout,this)
    }

    private fun setButtonType(payButtonType: PayButtonType?){
        when (payButtonType?.name) {
            PayButtonType.BENEFIT_PAY.name -> {
                buttonView  = LayoutInflater.from(context).inflate(R.layout.pay_button_benefitpay, mainView, false)
                mainView.addView(buttonView)
            }
            PayButtonType.KNET.name -> {
               // buttonView = LayoutInflater.from(context).inflate(R.layout.buy_with_google_pay, mainView, false)
              //  mainView.addView(buttonView)
            }
            PayButtonType.BENEFIT.name -> {
              //  buttonView = LayoutInflater.from(context).inflate(R.layout.donate_with_google_pay, mainView, false)
               // mainView.addView(buttonView)
            }
        }
        mainView.isFocusable= true
        mainView.isEnabled= true
    }
}