

/**
 *   Created by AhlaamK on 10/26/23, 11:18 AM
 *   Copyright (c) 2023 .
 *   All rights reserved Tap Payments.
 **
 */

package com.example.tappaybutton

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import company.tap.tapcardformkit.open.KnetPayStatusDelegate
import company.tap.tapcardformkit.open.web_wrapper.TapKnetConfiguration
import company.tap.tapcardformkit.open.web_wrapper.TapKnetPay

class PayButton :LinearLayout {
    var tapKnetPay: TapKnetPay
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
        tapKnetPay = TapKnetPay(context)
        this.addView(tapKnetPay)
    }


    fun initPayButton(
        context: Context,
        configuration: LinkedHashMap<String, Any>,
        payButton: PayButtonType
    ){

        TapKnetConfiguration.configureWithKnetDictionary(
            context,
            tapKnetPay,
            configuration,
            object :KnetPayStatusDelegate{
                override fun onError(error: String) {

                }

                override fun onSuccess(data: String) {

                }

            }
        )



    }
}