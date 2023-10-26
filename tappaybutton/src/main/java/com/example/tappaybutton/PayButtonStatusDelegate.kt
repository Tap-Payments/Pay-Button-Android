package company.tap.tappaybuttons
/*
 * *
 *  * Created by AhlaamK on 10/26/23, 8:19 AM
 *  * Copyright (c) 2023 . All rights reserved Tap Payments.
 *  *
 *
 */


interface PayButtonStatusDelegate {
    fun onSuccess(data: String)
    fun onReady(){}
    fun onClick(){}
    fun onOrderCreated(data: String){}
    fun onChargeCreated(data:String){}
    fun onError(error: String)
    fun onCancel(){}
}