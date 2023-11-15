package com.example.knet_android.cardSdk.model

data class CardResponse(
    val card: Card,
    val created: Long,
    val id: String,
    val live_mode: Boolean,
    val `object`: String,
    val purpose: String,
    val save_card: Boolean,
    val type: String,
    val url: String,
    val used: Boolean,
    val source: source
)
data class source(var id:String)
data class Card(
    val address: Address,
    val brand: String,
    val category: String,
    val exp_month: Int,
    val exp_year: Int,
    val fingerprint: String,
    val first_eight: String,
    val first_six: String,
    val funding: String,
    val id: String,
    val issuer: Issuer,
    val last_four: String,
    val name: String,
    val `object`: String,
    val scheme: String
)
data class Issuer(
    val bank: String,
    val country: String,
    val id: String
)
class Address