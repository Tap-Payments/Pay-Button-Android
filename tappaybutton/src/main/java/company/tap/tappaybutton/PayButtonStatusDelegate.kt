package company.tap.tappaybutton
/*
 * *
 *  * Created by AhlaamK on 10/26/23, 8:19 AM
 *  * Copyright (c) 2023 . All rights reserved Tap Payments.
 *  *
 *
 */


interface PayButtonStatusDelegate {
    fun onPayButtonSuccess(data: String)
    fun onPayButtonReady(){}
    fun onPayButtonClick(){}
    fun onPayButtonOrderCreated(data: String){}
    fun onPayButtonChargeCreated(data:String){}
    fun onPayButtonError(error: String)
    fun onPayButtonCancel(){}
}