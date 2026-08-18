package company.tap.paybutton


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chillibits.simplesettings.tool.getPrefBooleanValue
import com.chillibits.simplesettings.tool.getPrefStringSetValue
import com.chillibits.simplesettings.tool.getPrefStringValue
import com.chillibits.simplesettings.tool.getPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonConfiguration
import company.tap.tappaybutton.PayButtonStatusDelegate
import company.tap.tappaybutton.enums.publicKeyToGet
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal


class MainActivity : AppCompatActivity() ,PayButtonStatusDelegate{
    lateinit var payButton: PayButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

     /***
      * If pre creating intent ****/
     //  callIntentAPI()

        /*** If PASSING INTENT OBJECT **** /
         *
         */
       configureSdk(null)

    }



  /*  fun configureSdk(intentId:String?) {

      //  val publicKey = "pk_test_J2OSkKAFxu4jghc9zeRfQ0da"

        val publicKey =  getPrefStringValue("publicKey","pk_test_Vlk842B1EA7tDN5QbrfGjYzh")

        *//**
         * intent
         *//*
        val intentObj = HashMap<String,Any>()
        if (intentId != null) {
            intentObj.put("intent",intentId)
        }

        *//**
         * configureWithPayButtonDictionary and calling the PayButton SDK
         *//*
        val scope =intent.getStringExtra("scopeKey")
        val charge =intent.getStringExtra("scopeKey")
        val configuration = LinkedHashMap<String,Any>()
        val jsonObject = JSONObject()
        jsonObject.put("scope", scope)
        jsonObject.put("purpose", charge)
        jsonObject.put("statement_descriptor", "statement_descriptor")
        jsonObject.put("description", "sd")
        jsonObject.put("reference","uuid_testabcdfgkgdgd121992")
        jsonObject.put("customer_initiated", true)
        jsonObject.put("hash_string", "")
        jsonObject.put("idempotent", "")


        val merchant = JSONObject()
        val terminal = JSONObject()
        terminal.put("id","")
        val terminaldevice = JSONObject()
        terminaldevice.put("id","")
        terminal.put("terminal_device",terminaldevice)

        val medata = JSONObject()
        medata.put("uud","qq")
        medata.put("uud2","222")

        val operator = JSONObject()
        operator.put("id","")
        val device= JSONObject()
        device.put("id","")
        operator.put("device",device)
        merchant.put("id",intent.getStringExtra("merchantId"))
        merchant.put("terminal",terminal)
        merchant.put("operator",operator)
        val paymentprovider = JSONObject()
        val technology= JSONObject()
        technology.put("id","")

        val institution= JSONObject()
        institution.put("id","")

        paymentprovider.put("technology",technology)
        paymentprovider.put("institution",institution)

        val ddevelopmenthouse= JSONObject()
        ddevelopmenthouse.put("id","")

        val platform= JSONObject()
        platform.put("id","")

        merchant.put("payment_provider",paymentprovider)
        merchant.put("institution",institution)
        merchant.put("development_house",ddevelopmenthouse)
        merchant.put("platform",platform)

        jsonObject.put("merchant", merchant)

        val authenticate = JSONObject()
        authenticate.put("id","")
        authenticate.put("required",true)

        jsonObject.put("authenticate", authenticate)
        val transaction = JSONObject()
        val cardholderlogin = JSONObject()
        cardholderlogin.put("type", "GUEST")
        cardholderlogin.put("timestamp", "123213213")

        val payment_agreement  = JSONObject()

       // val contract = JSONObject()
       // contract.put("id","")
       // payment_agreement.put("id","")
       // payment_agreement.put("contract",contract)

        transaction.put("card_holder_login",cardholderlogin)
        transaction.put("metadata",medata)
        transaction.put("reference","TEST")
        transaction.put("payment_agreement",payment_agreement)

        jsonObject.put("transaction", transaction)

        val invoice = JSONObject()
        invoice.put("id","")
        jsonObject.put("invoice", invoice)


        val descriptiom = JSONObject()
        descriptiom.put("text", "name ")
        descriptiom.put( "lang", "en ")

        val reference = JSONObject()
        reference.put("sku", "stock keeping unit ")
        reference.put( "gtin", "global trade item number ")
        reference.put("code", "00dfd ")
        reference.put("financial_code", "0022343")


        val descArray = JSONArray()
        descArray.put(descriptiom)

        val product = JSONObject()
        product.put("id","")
        product.put("amount",intent.getStringExtra("amountKey")?.toDouble())
        product.put("name",descArray)
        product.put("description",descArray)
        product.put("metadata",medata)
        product.put("category","PHYSICAL_GOODS")
        product.put("reference",reference)
        val  itemsList = JSONObject()
        itemsList.put("id", " ")
        itemsList.put("quantity", 1)
        itemsList.put("pickup", false)
        itemsList.put("product", product)

        val itemsArry =  JSONArray()
        itemsArry.put(itemsList)
        val items = JSONObject()
        items.put("count",1)
        items.put("list",itemsArry)



        val order = JSONObject()
        order.put("amount",(intent.getStringExtra("amountKey")?.toDouble()))
        // order.put("amount",3)
        order.put("currency",intent.getStringExtra("orderCurrencyKey"))
        order.put("description",descArray)
        order.put("reference",intent.getStringExtra("orderRefrenceKey"))
        order.put("items",items)

        val discount = JSONObject()
        discount.put("type","F")
        discount.put("value",1)

        val tax = JSONObject()
        tax.put( "name", "VAT")
        tax.put( "description", "test")
        tax.put( "type", "F")
        tax.put( "value", 1)

        val taxarry= JSONArray()
        taxarry.put(tax)


        val adddress = JSONObject()
        adddress.put("type","home ")
        adddress.put("line1", "sdfghjk ")
        adddress.put("line2", "oiuytr ")
        adddress.put("line3", "line3 ")
        adddress.put("line4","line4 ")
        adddress.put( "apartment", " ")
        adddress.put("building", " ")
        adddress.put("street", " ")
        adddress.put("avenue", " ")
        adddress.put("block", " ")
        adddress.put("area"," ")
        adddress.put("city", "salmyia")
        adddress.put("state", "kuwait")
        adddress.put("country", "KW")
        adddress.put("zip_code","30003")
        adddress.put("postal_code", " ")

        val provider = JSONObject()
        provider.put("id","")



        val shippigobject = JSONObject()
        shippigobject.put("amount",1)
        shippigobject.put("description",descArray)
        shippigobject.put("recipient_name",descArray)
        shippigobject.put("address",adddress)
        shippigobject.put("provider",provider)
        shippigobject.put("metadata",medata)

        // order.put("tax",taxarry)
        // order.put("discount",discount)
        //  order.put("shipping",shippigobject)
        order.put("metadata",medata)


        jsonObject.put("order",order)

        val name = JSONObject()
        name.put("first", "OSAMA ")
        name.put("last", "Ahmed ")
        name.put("middle", " ")
        name.put("title","MR ")

        val nameList = JSONArray()
        nameList.put(name)

        val nameCard = JSONObject()
        nameCard.put("content", "OSAMA AHMED ")
        nameCard.put("editable",true)

        val phone = JSONObject()
        phone.put("country_code","965")
        phone.put("number","55683784")

        val contact = JSONObject()
        contact.put("email","tap.test@company")
        contact.put("phone",phone)

        val customer = JSONObject()
        customer.put("id",intent.getStringExtra("customerIdKey"))
        customer.put("name",nameList)
        customer.put("name_on_card",nameCard)
        customer.put("contact",contact)




        customer.put("address",adddress)

        jsonObject.put("customer",customer)
        val receipt = JSONObject()
        receipt.put("email", false)
        receipt.put("sms", false)

        jsonObject.put("receipt", receipt)


        val configOb = JSONObject()
        configOb.put("initiator","CHECKOUT")
        configOb.put("type","BUTTON")
        val features = JSONObject()
        features.put("acceptance_badge",true)
        features.put("order",true)
        features.put("multiple_currencies",true)

        val currency_conversions = JSONObject()
        currency_conversions.put("dynamic", true)
        currency_conversions.put("location", true)
        currency_conversions.put("payment", true)
        currency_conversions.put("cobadge", true)

        val alternative_card_inputs = JSONObject()
        alternative_card_inputs.put("card_scanner", true)
        alternative_card_inputs.put("card_nfc", true)

        val customer_cards = JSONObject()
        customer_cards.put("save_card", true)
        customer_cards.put("auto_save_card", true)
        customer_cards.put("display_saved_cards", true)


        val payments = JSONObject()
        payments.put("card", true)
        payments.put("device", true)
        payments.put("wallet", true)
        payments.put("bnpl", true)
        payments.put("mobile", true)
        payments.put("cash", true)
        payments.put("redirect", true)

        features.put("currency_conversions",currency_conversions)
        features.put("payments",payments)
        features.put("alternative_card_inputs",alternative_card_inputs)
        features.put("customer_cards",customer_cards)

        val acceptance = JSONObject()
        val supported_regions = JSONArray()
        supported_regions.put("LOCAL")
        supported_regions.put("REGIONAL")
        supported_regions.put("GLOBAL")

        val supported_countries = JSONArray()
        supported_countries.put("AE")
        supported_countries.put("SA")
        supported_countries.put("KW")
        supported_countries.put("EG")

        val supported_currencies = JSONArray()
        supported_currencies.put("KWD")
        supported_currencies.put( "SAR")
        supported_currencies.put("AED")
        supported_currencies.put("OMR")
        supported_currencies.put("QAR")
        supported_currencies.put("BHD")
        supported_currencies.put("EGP")
        supported_currencies.put("GBP")
        supported_currencies.put("USD")
        supported_currencies.put("EUR")
        supported_currencies.put("AED")



        val supported_payment_methods = JSONArray()
        supported_payment_methods.put("BENEFITPAY")

        *//*val supported_payment_types = JSONArray()
        // supported_payment_methods.put ("CARD")
        supported_payment_types.put("DEVICE")
        supported_payment_types.put("WEB")*//*

        val supported_schemes = JSONArray()
        // supported_schemes.put("CARD")
        supported_schemes.put( "MADA")
        supported_schemes.put("OMANNET")
        supported_schemes.put("VISA")
        supported_schemes.put( "MASTERCARD")
        supported_schemes.put("AMEX")
        supported_schemes.put("BENEFIT_CARD" )
        val defaultHash = hashSetOf("VISA","AMEX","MASTERCARD","BENEFIT_CARD","OMANNET","MADA")
        val defaultFundHash = hashSetOf("CREDIT","DEBIT")

        println("defaultHash"+defaultHash)

        val supported_fund_source = JSONArray()
        // supported_fund_source.put("CARD")
        supported_fund_source.put("DEBIT")
        supported_fund_source.put("CREDIT")
        val supported_payment_flows = JSONArray()
        //supported_payment_flows.put("CARD")
        supported_payment_flows.put("POPUP")
        supported_payment_flows.put( "PAGE")

        val supported_payment_authentications = JSONArray()
        supported_payment_authentications.put("3DS")
        supported_payment_authentications.put( "EMV")
        supported_payment_authentications.put( "PASSKEY")

         acceptance.put("supported_regions",supported_regions)
        // acceptance.put("supported_countries",supported_countries)
        // acceptance.put("supported_payment_types",supported_payment_types)
         acceptance.put("supported_currencies",supported_currencies)

        val paymethods = getPrefStringValue("buttonKey","KNET")

        val arry = JSONArray()
        arry.put(paymethods)

        acceptance.put("supported_payment_methods",arry)


        acceptance.put("supported_schemes",
            JSONArray(getPrefs().getStringSet("supportedSchemesKey", defaultHash.toHashSet()))
        )
        acceptance.put("supported_fund_source",
            JSONArray(getPrefs().getStringSet("supportedFundSourceKey", defaultFundHash.toHashSet()))
        )
        acceptance.put("supported_payment_authentications",supported_payment_authentications)
        acceptance.put("supported_payment_flows",supported_payment_flows)

        val interfacee = JSONObject()
        interfacee.put("locale",intent.getStringExtra("selectedlangKey") ?: "en")
        interfacee.put("theme",intent.getStringExtra("selectedthemeKey") ?: "light")
        interfacee.put("edges",intent.getStringExtra("selectedcardedgeKey") ?: "circular")
        interfacee.put("color_style",intent.getStringExtra("selectedcolorstyleKey") ?:"COLORED")
        interfacee.put("user_experience","popup")
        interfacee.put("card_direction","dynamic")
        interfacee.put("direction","ltr")
        interfacee.put("loader", true)
        interfacee.put("powered", true)


        val fieldvisibility = JSONObject()

        val card  = JSONObject()
        card.put( "number",true)
        card.put("expiry",true)
        card.put("cvv",true)
        card.put("cardholder", true)

        val conatct = JSONObject()
        conatct.put("email", true)
        conatct.put("number", true)

        val shipping  = JSONObject()
        shipping.put("address", true)

        fieldvisibility.put("name",true)
        fieldvisibility.put("card",card)
        fieldvisibility.put("contact",conatct)
        //  fieldvisibility.put("shipping",shipping)

        configOb.put("features",features)
        configOb.put("acceptance",acceptance)
        configOb.put("field_visibility",fieldvisibility)
        configOb.put("interface",interfacee)



        val checkout = JSONObject()
        checkout.put("auto", true)
        checkout.put("metadata", medata)

        val post = JSONObject()
        post.put("url","demo.com")

        val redirect = JSONObject()
        redirect.put("url","demo.com")

        val domain = JSONObject()
        domain.put("url","https://demo.dev.tap.company")

        jsonObject.put("config", configOb)
        jsonObject.put("domain",domain)
        jsonObject.put("redirect",redirect)
        jsonObject.put("post",post)
        jsonObject.put("checkout",checkout)

        val intentObjc: HashMap<String, Any> = Gson().fromJson(
            jsonObject.toString(), object : TypeToken<HashMap<String?, Any?>?>() {}.type
        )

        if(intentId==null){
            PayButtonConfiguration.configureWithPayButtonDictionary(
                this,
                publicKey,
                null,
                findViewById(R.id.redirect_pay),
                intentObjc,
                this)
        }else{
            PayButtonConfiguration.configureWithPayButtonDictionary(
                this,
                publicKey,
                intentId,
                findViewById(R.id.redirect_pay),
                null,
                this)
        }




    }*/
/*    fun configureSdk(intentId: String?) {

        val publicKey =
            getPrefStringValue(
                "publicKey",
                "pk_test_Vlk842B1EA7tDN5QbrfGjYzh"
            )

        *//**
         * Intent
         *//*
        val intentObj = HashMap<String, Any>()

        if (intentId != null) {
            intentObj["intent"] = intentId
        }

        *//**
         * Pay Button Configuration
         *//*
        val configuration = LinkedHashMap<String, Any>()
        val jsonObject = JSONObject()

        // ============================================================
        // BASIC INFORMATION
        // ============================================================

        jsonObject.put("scope", "CHARGE")
        jsonObject.put("purpose", "charge")
        jsonObject.put("statement_descriptor", "statement_descriptor")
        jsonObject.put("description", "sd")
        jsonObject.put(
            "reference",
            "uuid_testabcdfgkgdgd121992"
        )
        jsonObject.put("customer_initiated", true)
        jsonObject.put("idempotent", "")

        // ============================================================
        // MERCHANT
        // ============================================================

        val merchant = JSONObject()

        merchant.put("id", "1124340")

        // Terminal
        val terminal = JSONObject()
        terminal.put("id", "")

        val terminalDevice = JSONObject()
        terminalDevice.put("id", "")

        terminal.put(
            "terminal_device",
            terminalDevice
        )

        // Operator
        val operator = JSONObject()
        operator.put("id", "")

        val operatorDevice = JSONObject()
        operatorDevice.put("id", "")

        operator.put(
            "device",
            operatorDevice
        )

        // Payment Provider
        val paymentProvider = JSONObject()

        val technology = JSONObject()
        technology.put("id", "")

        val institution = JSONObject()
        institution.put("id", "")

        paymentProvider.put(
            "technology",
            technology
        )

        paymentProvider.put(
            "institution",
            institution
        )

        // Development House
        val developmentHouse = JSONObject()
        developmentHouse.put("id", "")

        // Platform
        val platform = JSONObject()
        platform.put("id", "")

        merchant.put("terminal", terminal)
        merchant.put("operator", operator)
        merchant.put(
            "payment_provider",
            paymentProvider
        )
        merchant.put(
            "development_house",
            developmentHouse
        )
        merchant.put("platform", platform)

        jsonObject.put("merchant", merchant)

        // ============================================================
        // AUTHENTICATE
        // ============================================================

        val authenticate = JSONObject()

        authenticate.put("id", "")
        authenticate.put("required", true)

        jsonObject.put(
            "authenticate",
            authenticate
        )

        // ============================================================
        // TRANSACTION
        // ============================================================

        val transaction = JSONObject()

        // Card Holder Login
        val cardHolderLogin = JSONObject()

        cardHolderLogin.put(
            "type",
            "GUEST"
        )

        cardHolderLogin.put(
            "timestamp",
            "123213213"
        )

        // Transaction Metadata
        val transactionMetadata = JSONObject()
        transactionMetadata.put(
            "s",
            "s"
        )

        // Payment Agreement
        val paymentAgreement = JSONObject()

        paymentAgreement.put(
            "id",
            ""
        )

        val contract = JSONObject()
        contract.put(
            "id",
            ""
        )

        paymentAgreement.put(
            "contract",
            contract
        )

        transaction.put(
            "card_holder_login",
            cardHolderLogin
        )

        transaction.put(
            "metadata",
            transactionMetadata
        )

        transaction.put(
            "reference",
            "kjhjkhk"
        )

        transaction.put(
            "payment_agreement",
            paymentAgreement
        )

        jsonObject.put(
            "transaction",
            transaction
        )

        // ============================================================
        // INVOICE
        // ============================================================

        val invoice = JSONObject()

        invoice.put(
            "id",
            ""
        )

        jsonObject.put(
            "invoice",
            invoice
        )

        // ============================================================
        // ORDER DESCRIPTION
        // ============================================================

        val orderDescriptionObject = JSONObject()

        orderDescriptionObject.put(
            "text",
            "name"
        )

        orderDescriptionObject.put(
            "lang",
            "en"
        )

        val orderDescription = JSONArray()

        orderDescription.put(
            orderDescriptionObject
        )

        // ============================================================
        // PRODUCT NAME
        // ============================================================

        val productNameObject = JSONObject()

        productNameObject.put(
            "text",
            "Laptop"
        )

        productNameObject.put(
            "lang",
            "en"
        )

        val productName = JSONArray()

        productName.put(
            productNameObject
        )

        // ============================================================
        // PRODUCT DESCRIPTION
        // ============================================================

        val productDescriptionObject = JSONObject()

        productDescriptionObject.put(
            "text",
            "سجادة"
        )

        productDescriptionObject.put(
            "lang",
            "ar"
        )

        val productDescription = JSONArray()

        productDescription.put(
            productDescriptionObject
        )

        // ============================================================
        // PRODUCT REFERENCE
        // ============================================================

        val productReference = JSONObject()

        productReference.put(
            "sku",
            "stock keeping unit"
        )

        productReference.put(
            "gtin",
            "global trade item number"
        )

        productReference.put(
            "code",
            "00dfd"
        )

        productReference.put(
            "financial_code",
            "0022343"
        )

        // ============================================================
        // PRODUCT METADATA
        // ============================================================

        val productMetadata = JSONObject()

        productMetadata.put(
            "",
            ""
        )

        // ============================================================
        // PRODUCT
        // ============================================================

        val product = JSONObject()

        product.put(
            "id",
            ""
        )

        product.put(
            "amount",
            2
        )

        product.put(
            "name",
            productName
        )

        product.put(
            "description",
            productDescription
        )

        product.put(
            "category",
            "PHYSICAL_GOODS"
        )

        product.put(
            "metadata",
            productMetadata
        )

        product.put(
            "reference",
            productReference
        )

        // ============================================================
        // ORDER ITEM
        // ============================================================

        val orderItem = JSONObject()

        orderItem.put(
            "id",
            ""
        )

        orderItem.put(
            "quantity",
            1
        )

        orderItem.put(
            "pickup",
            false
        )

        orderItem.put(
            "product",
            product
        )

        val itemsList = JSONArray()

        itemsList.put(
            orderItem
        )

        val items = JSONObject()

        items.put(
            "count",
            1
        )

        items.put(
            "list",
            itemsList
        )

        // ============================================================
        // TAX
        // ============================================================

        val taxObject = JSONObject()

        taxObject.put(
            "name",
            "VAT"
        )

        taxObject.put(
            "description",
            "test"
        )

        taxObject.put(
            "type",
            "F"
        )

        taxObject.put(
            "value",
            1
        )

        val tax = JSONArray()

        tax.put(
            taxObject
        )

        // ============================================================
        // DISCOUNT
        // ============================================================

        val discount = JSONObject()

        discount.put(
            "type",
            "F"
        )

        discount.put(
            "value",
            1
        )

        // ============================================================
        // SHIPPING ADDRESS
        // ============================================================

        val shippingAddress = JSONObject()

        shippingAddress.put(
            "type",
            "home"
        )

        shippingAddress.put(
            "line1",
            "sdfghjk"
        )

        shippingAddress.put(
            "line2",
            "oiuytr"
        )

        shippingAddress.put(
            "line3",
            "line3"
        )

        shippingAddress.put(
            "line4",
            "line4"
        )

        shippingAddress.put(
            "apartment",
            ""
        )

        shippingAddress.put(
            "building",
            ""
        )

        shippingAddress.put(
            "street",
            ""
        )

        shippingAddress.put(
            "avenue",
            ""
        )

        shippingAddress.put(
            "block",
            ""
        )

        shippingAddress.put(
            "area",
            ""
        )

        shippingAddress.put(
            "city",
            "salmyia"
        )

        shippingAddress.put(
            "state",
            "kuwait"
        )

        shippingAddress.put(
            "country",
            "kw"
        )

        shippingAddress.put(
            "zip_code",
            "30003"
        )

        shippingAddress.put(
            "postal_code",
            "30003"
        )

        // ============================================================
        // SHIPPING
        // ============================================================

        val shippingProvider = JSONObject()

        shippingProvider.put(
            "id",
            "prov_FFSFAGGAHAAJAJ"
        )

        val shippingDescriptionObject = JSONObject()

        shippingDescriptionObject.put(
            "text",
            "description"
        )

        shippingDescriptionObject.put(
            "lang",
            "en"
        )

        val shippingDescription = JSONArray()

        shippingDescription.put(
            shippingDescriptionObject
        )

        val recipientNameObject = JSONObject()

        recipientNameObject.put(
            "text",
            "Name"
        )

        recipientNameObject.put(
            "lang",
            "en"
        )

        val recipientName = JSONArray()

        recipientName.put(
            recipientNameObject
        )

        val shipping = JSONObject()

        shipping.put(
            "amount",
            1
        )

        shipping.put(
            "description",
            shippingDescription
        )

        shipping.put(
            "recipient_name",
            recipientName
        )

        shipping.put(
            "address",
            shippingAddress
        )

        shipping.put(
            "provider",
            shippingProvider
        )

        // ============================================================
        // ORDER METADATA
        // ============================================================

        val orderMetadata = JSONObject()

        orderMetadata.put(
            "o",
            "s"
        )

        // ============================================================
        // ORDER
        // ============================================================

        val order = JSONObject()

        order.put(
            "amount",
            3
        )

        order.put(
            "currency",
            "SAR"
        )

        order.put(
            "description",
            orderDescription
        )

        order.put(
            "reference",
            ""
        )

        order.put(
            "items",
            items
        )

        order.put(
            "tax",
            tax
        )

        order.put(
            "discount",
            discount
        )

        order.put(
            "shipping",
            shipping
        )

        order.put(
            "metadata",
            orderMetadata
        )

        jsonObject.put(
            "order",
            order
        )

        // ============================================================
        // CUSTOMER
        // ============================================================

        val customerNameObject = JSONObject()

        customerNameObject.put(
            "first",
            "OSAMA"
        )

        customerNameObject.put(
            "last",
            "Ahmed"
        )

        customerNameObject.put(
            "middle",
            ""
        )

        customerNameObject.put(
            "title",
            "MR"
        )

        val customerName = JSONArray()

        customerName.put(
            customerNameObject
        )

        // Name on Card
        val nameOnCard = JSONObject()

        nameOnCard.put(
            "content",
            "OSAMA AHMED"
        )

        nameOnCard.put(
            "editable",
            true
        )

        // Phone
        val customerPhone = JSONObject()

        customerPhone.put(
            "country_code",
            "965"
        )

        customerPhone.put(
            "number",
            "51234567"
        )

        // Contact
        val customerContact = JSONObject()

        customerContact.put(
            "email",
            "f.mehmood@tap.company"
        )

        customerContact.put(
            "phone",
            customerPhone
        )

        // Customer Address
        val customerAddress = JSONObject()

        customerAddress.put(
            "type",
            "home"
        )

        customerAddress.put(
            "line1",
            "sdfghjk"
        )

        customerAddress.put(
            "line2",
            "oiuytr"
        )

        customerAddress.put(
            "line3",
            "line3"
        )

        customerAddress.put(
            "line4",
            "line4"
        )

        customerAddress.put(
            "apartment",
            ""
        )

        customerAddress.put(
            "building",
            ""
        )

        customerAddress.put(
            "street",
            ""
        )

        customerAddress.put(
            "avenue",
            ""
        )

        customerAddress.put(
            "block",
            ""
        )

        customerAddress.put(
            "area",
            ""
        )

        customerAddress.put(
            "city",
            "salmyia"
        )

        customerAddress.put(
            "state",
            "kuwait"
        )

        customerAddress.put(
            "country",
            "kw"
        )

        customerAddress.put(
            "zip_code",
            "30003"
        )

        customerAddress.put(
            "postal_code",
            ""
        )

        // Customer
        val customer = JSONObject()

        customer.put(
            "id",
            ""
        )

        customer.put(
            "name",
            customerName
        )

        customer.put(
            "name_on_card",
            nameOnCard
        )

        customer.put(
            "contact",
            customerContact
        )

        customer.put(
            "address",
            customerAddress
        )

        jsonObject.put(
            "customer",
            customer
        )

        // ============================================================
        // RECEIPT
        // ============================================================

        val receipt = JSONObject()

        receipt.put(
            "email",
            false
        )

        receipt.put(
            "sms",
            false
        )

        jsonObject.put(
            "receipt",
            receipt
        )

        // ============================================================
        // CONFIGURATION
        // ============================================================

        val config = JSONObject()

        config.put(
            "initiator",
            "CHECKOUT"
        )

        config.put(
            "type",
            "BUTTON"
        )

        // ============================================================
        // FEATURES
        // ============================================================

        val features = JSONObject()

        features.put(
            "acceptance_badge",
            true
        )

        features.put(
            "order",
            true
        )

        features.put(
            "multiple_currencies",
            true
        )

        // Currency Conversions
        val currencyConversions = JSONObject()

        currencyConversions.put(
            "dynamic",
            true
        )

        currencyConversions.put(
            "location",
            true
        )

        currencyConversions.put(
            "payment",
            true
        )

        currencyConversions.put(
            "cobadge",
            true
        )

        features.put(
            "currency_conversions",
            currencyConversions
        )

        // Payments
        val payments = JSONObject()

        payments.put(
            "card",
            true
        )

        payments.put(
            "device",
            true
        )

        payments.put(
            "wallet",
            true
        )

        payments.put(
            "bnpl",
            true
        )

        payments.put(
            "mobile",
            true
        )

        payments.put(
            "cash",
            true
        )

        payments.put(
            "redirect",
            true
        )

        features.put(
            "payments",
            payments
        )

        // Alternative Card Inputs
        val alternativeCardInputs = JSONObject()

        alternativeCardInputs.put(
            "card_scanner",
            true
        )

        alternativeCardInputs.put(
            "card_nfc",
            true
        )

        features.put(
            "alternative_card_inputs",
            alternativeCardInputs
        )

        // Customer Cards
        val customerCards = JSONObject()

        customerCards.put(
            "save_card",
            true
        )

        customerCards.put(
            "auto_save_card",
            true
        )

        customerCards.put(
            "display_saved_cards",
            true
        )

        features.put(
            "customer_cards",
            customerCards
        )

        // ============================================================
        // ACCEPTANCE
        // ============================================================

        val acceptance = JSONObject()

        // Supported Regions
        val supportedRegions = JSONArray()

        supportedRegions.put("LOCAL")
        supportedRegions.put("REGIONAL")
        supportedRegions.put("GLOBAL")

        acceptance.put(
            "supported_regions",
            supportedRegions
        )

        // Supported Currencies
        val supportedCurrencies = JSONArray()

        supportedCurrencies.put("KWD")
        supportedCurrencies.put("SAR")
        supportedCurrencies.put("AED")
        supportedCurrencies.put("OMR")
        supportedCurrencies.put("QAR")
        supportedCurrencies.put("BHD")
        supportedCurrencies.put("EGP")
        supportedCurrencies.put("GBP")
        supportedCurrencies.put("USD")
        supportedCurrencies.put("EUR")
        supportedCurrencies.put("AED")

        acceptance.put(
            "supported_currencies",
            supportedCurrencies
        )

        // Supported Payment Methods
        val supportedPaymentMethods = JSONArray()

        supportedPaymentMethods.put(
            "GOOGLE_PAY"
        )

        acceptance.put(
            "supported_payment_methods",
            supportedPaymentMethods
        )

        // Supported Schemes
        val supportedSchemes = JSONArray()

        supportedSchemes.put("MADA")
        supportedSchemes.put("OMANNET")
        supportedSchemes.put("VISA")
        supportedSchemes.put("MASTERCARD")
        supportedSchemes.put("AMEX")
        supportedSchemes.put("BENEFIT_CARD")

        acceptance.put(
            "supported_schemes",
            supportedSchemes
        )

        // Supported Fund Sources
        val supportedFundSource = JSONArray()

        supportedFundSource.put("DEBIT")
        supportedFundSource.put("CREDIT")

        acceptance.put(
            "supported_fund_source",
            supportedFundSource
        )

        // Supported Payment Authentications
        val supportedPaymentAuthentications = JSONArray()

        supportedPaymentAuthentications.put("3DS")
        supportedPaymentAuthentications.put("EMV")
        supportedPaymentAuthentications.put("PASSKEY")

        acceptance.put(
            "supported_payment_authentications",
            supportedPaymentAuthentications
        )

        // Supported Payment Flows
        val supportedPaymentFlows = JSONArray()

        supportedPaymentFlows.put("POPUP")
        supportedPaymentFlows.put("PAGE")

        acceptance.put(
            "supported_payment_flows",
            supportedPaymentFlows
        )

        // ============================================================
        // FIELD VISIBILITY
        // ============================================================

        val fieldVisibility = JSONObject()

        fieldVisibility.put(
            "name",
            true
        )

        val card = JSONObject()

        card.put("number", true)
        card.put("expiry", true)
        card.put("cvv", true)
        card.put("cardholder", true)

        fieldVisibility.put(
            "card",
            card
        )

        val contactVisibility = JSONObject()

        contactVisibility.put(
            "email",
            true
        )

        contactVisibility.put(
            "number",
            true
        )

        fieldVisibility.put(
            "contact",
            contactVisibility
        )

        val shippingVisibility = JSONObject()

        shippingVisibility.put(
            "address",
            true
        )

        fieldVisibility.put(
            "shipping",
            shippingVisibility
        )

        // ============================================================
        // INTERFACE
        // ============================================================

        val interfaceObject = JSONObject()

        interfaceObject.put(
            "user_experience",
            "popup"
        )

        interfaceObject.put(
            "locale",
            "en"
        )

        interfaceObject.put(
            "direction",
            "dynamic"
        )

        interfaceObject.put(
            "card_direction",
            "ltr"
        )

        interfaceObject.put(
            "edges",
            "circular"
        )

        interfaceObject.put(
            "theme",
            "light"
        )

        interfaceObject.put(
            "color_style",
            "coloured"
        )

        interfaceObject.put(
            "loader",
            true
        )

        interfaceObject.put(
            "powered",
            true
        )

        // ============================================================
        // CONFIG
        // ============================================================

        config.put(
            "features",
            features
        )

        config.put(
            "acceptance",
            acceptance
        )

        config.put(
            "field_visibility",
            fieldVisibility
        )

        config.put(
            "interface",
            interfaceObject
        )

        jsonObject.put(
            "config",
            config
        )

        // ============================================================
        // DOMAIN
        // ============================================================

        val domain = JSONObject()

        domain.put(
            "url",
            "tap.PayButtonSDK.demo"
        )

        jsonObject.put(
            "domain",
            domain
        )

        // ============================================================
        // REDIRECT
        // ============================================================

        val redirect = JSONObject()

        redirect.put(
            "url",
            "demo.com"
        )

        jsonObject.put(
            "redirect",
            redirect
        )

        // ============================================================
        // POST
        // ============================================================

        val post = JSONObject()

        post.put(
            "url",
            "demo.com"
        )

        jsonObject.put(
            "post",
            post
        )

        // ============================================================
        // CHECKOUT
        // ============================================================

        val checkout = JSONObject()

        checkout.put(
            "auto",
            true
        )

        val checkoutMetadata = JSONObject()

        checkoutMetadata.put(
            "udf1",
            "test 1"
        )

        checkoutMetadata.put(
            "udf2",
            "test 2"
        )

        checkout.put(
            "metadata",
            checkoutMetadata
        )

        jsonObject.put(
            "checkout",
            checkout
        )

        // ============================================================
        // CONVERT JSON TO HASHMAP
        // ============================================================

        val intentObjc: HashMap<String, Any> = Gson().fromJson(
            jsonObject.toString(),
            object : TypeToken<HashMap<String?, Any?>?>() {}.type
        )

        // ============================================================
        // CONFIGURE PAY BUTTON
        // ============================================================

        if (intentId == null) {

            PayButtonConfiguration.configureWithPayButtonDictionary(
                this,
                publicKey,
                null,
                findViewById(R.id.redirect_pay),
                intentObjc,
                this
            )

        } else {

            PayButtonConfiguration.configureWithPayButtonDictionary(
                this,
                publicKey,
                intentId,
                findViewById(R.id.redirect_pay),
                null,
                this
            )
        }
    }*/
    fun configureSdk(intentId: String?) {

        val publicKey = getPrefStringValue(
            "publicKey",
            "pk_test_YhUjg9PNT8oDlKJ1aE2fMRz7"
        )

        /**
         * Intent
         */
        val intentObj = HashMap<String, Any>()

        if (intentId != null) {
            intentObj["intent"] = intentId
        }

        /**
         * Pay Button Configuration
         */
        val configuration = LinkedHashMap<String, Any>()
        val jsonObject = JSONObject()

        // ============================================================
        // BASIC INFORMATION
        // ============================================================

        // No preference exists for scope in the XML except scopeKey.
        // Keep CHARGE as the fallback/default behavior.
        jsonObject.put(
            "scope",
            getPrefStringValue("scopeKey", "charge")
        )

        jsonObject.put(
            "purpose",
            getPrefStringValue("purposeKey", "charge")
        )

        jsonObject.put(
            "statement_descriptor",
            getPrefStringValue(
                "statementDescriptorKey",
                "statement_descriptor"
            )
        )

        jsonObject.put(
            "description",
            getPrefStringValue(
                "descriptionKey",
                "sd"
            )
        )

        jsonObject.put(
            "reference",
            getPrefStringValue(
                "referenceKey",
                "uuid_testabcdfgkgdgd121992"
            )
        )

        jsonObject.put(
            "customer_initiated",
            getPrefBooleanValue(
                "customerInitiatedKey",
                true
            )
        )

        // No preference exists for idempotent.
        jsonObject.put("idempotent", "")

        // ============================================================
        // MERCHANT
        // ============================================================

        val merchant = JSONObject()

        // merchantId exists in Preference XML.
        merchant.put(
            "id",
            getPrefStringValue("merchantId", "1124340")
        )

        // Terminal
        // No terminal preference exists -> keep hardcoded.
        val terminal = JSONObject()
        terminal.put("id", "")

        val terminalDevice = JSONObject()
        terminalDevice.put("id", "")

        terminal.put(
            "terminal_device",
            terminalDevice
        )

        // Operator
        // No operator preference exists -> keep hardcoded.
        val operator = JSONObject()
        operator.put("id", "")

        val operatorDevice = JSONObject()
        operatorDevice.put("id", "")

        operator.put(
            "device",
            operatorDevice
        )

        // Payment Provider
        // No corresponding preferences exist -> keep hardcoded.
        val paymentProvider = JSONObject()

        val technology = JSONObject()
        technology.put("id", "")

        val institution = JSONObject()
        institution.put("id", "")

        paymentProvider.put(
            "technology",
            technology
        )

        paymentProvider.put(
            "institution",
            institution
        )

        // Development House
        val developmentHouse = JSONObject()
        developmentHouse.put("id", "")

        // Platform
        val platform = JSONObject()
        platform.put("id", "")

        merchant.put("terminal", terminal)
        merchant.put("operator", operator)
        merchant.put("payment_provider", paymentProvider)
        merchant.put("development_house", developmentHouse)
        merchant.put("platform", platform)

        jsonObject.put("merchant", merchant)

        // ============================================================
        // AUTHENTICATE
        // ============================================================

        val authenticate = JSONObject()

        authenticate.put("id", "")

        authenticate.put(
            "required",
            getPrefBooleanValue(
                "authenticateRequiredKey",
                true
            )
        )

        jsonObject.put(
            "authenticate",
            authenticate
        )

        // ============================================================
        // TRANSACTION
        // ============================================================

        val transaction = JSONObject()

        // No preference exists for card holder login.
        val cardHolderLogin = JSONObject()

        cardHolderLogin.put(
            "type",
            "GUEST"
        )

        cardHolderLogin.put(
            "timestamp",
            "123213213"
        )

        // Transaction Metadata
        val transactionMetadata = JSONObject()
        transactionMetadata.put(
            "s",
            "s"
        )

        // Payment Agreement
        val paymentAgreement = JSONObject()

        paymentAgreement.put(
            "id",
            ""
        )

        val contract = JSONObject()
        contract.put(
            "id",
            ""
        )

        paymentAgreement.put(
            "contract",
            contract
        )

        transaction.put(
            "card_holder_login",
            cardHolderLogin
        )

        transaction.put(
            "metadata",
            transactionMetadata
        )

        transaction.put(
            "reference",
            getPrefStringValue(
                "transactionRefrenceKey",
                "kjhjkhk"
            )
        )

        transaction.put(
            "payment_agreement",
            paymentAgreement
        )

        jsonObject.put(
            "transaction",
            transaction
        )

        // ============================================================
        // INVOICE
        // ============================================================

        val invoice = JSONObject()

        invoice.put(
            "id",
            ""
        )

        jsonObject.put(
            "invoice",
            invoice
        )

        // ============================================================
        // ORDER DESCRIPTION
        // ============================================================

        val orderDescriptionObject = JSONObject()

        orderDescriptionObject.put(
            "text",
            "name"
        )

        orderDescriptionObject.put(
            "lang",
            "en"
        )

        val orderDescription = JSONArray()

        orderDescription.put(
            orderDescriptionObject
        )

        // ============================================================
        // PRODUCT NAME
        // ============================================================

        val productNameObject = JSONObject()

        productNameObject.put(
            "text",
            "Laptop"
        )

        productNameObject.put(
            "lang",
            "en"
        )

        val productName = JSONArray()

        productName.put(
            productNameObject
        )

        // ============================================================
        // PRODUCT DESCRIPTION
        // ============================================================

        val productDescriptionObject = JSONObject()

        productDescriptionObject.put(
            "text",
            "سجادة"
        )

        productDescriptionObject.put(
            "lang",
            "ar"
        )

        val productDescription = JSONArray()

        productDescription.put(
            productDescriptionObject
        )

        // ============================================================
        // PRODUCT REFERENCE
        // ============================================================

        val productReference = JSONObject()

        productReference.put(
            "sku",
            "stock keeping unit"
        )

        productReference.put(
            "gtin",
            "global trade item number"
        )

        productReference.put(
            "code",
            "00dfd"
        )

        productReference.put(
            "financial_code",
            "0022343"
        )

        // ============================================================
        // PRODUCT METADATA
        // ============================================================

        val productMetadata = JSONObject()

        productMetadata.put(
            "",
            ""
        )

        // ============================================================
        // PRODUCT
        // ============================================================

        val product = JSONObject()

        product.put(
            "id",
            ""
        )

        /*
         * No product amount preference exists.
         * Keep existing hardcoded value.
         *
         * IMPORTANT:
         * The actual order amount below comes dynamically from amountKey.
         */
        product.put(
            "amount",
            intent.getStringExtra("amountKey")?.toDouble()
        )

        product.put(
            "name",
            productName
        )

        product.put(
            "description",
            productDescription
        )

        product.put(
            "category",
            "PHYSICAL_GOODS"
        )

        product.put(
            "metadata",
            productMetadata
        )

        product.put(
            "reference",
            productReference
        )

        // ============================================================
        // ORDER ITEM
        // ============================================================

        val orderItem = JSONObject()

        orderItem.put(
            "id",
            ""
        )

        orderItem.put(
            "quantity",
            1
        )

        orderItem.put(
            "pickup",
            false
        )

        orderItem.put(
            "product",
            product
        )

        val itemsList = JSONArray()

        itemsList.put(
            orderItem
        )

        val items = JSONObject()

        items.put(
            "count",
            1
        )

        items.put(
            "list",
            itemsList
        )

        // ============================================================
        // TAX
        // ============================================================

        val taxObject = JSONObject()

        taxObject.put(
            "name",
            "VAT"
        )

        taxObject.put(
            "description",
            "test"
        )

        taxObject.put(
            "type",
            "F"
        )

        taxObject.put(
            "value",
            1
        )

        val tax = JSONArray()

        tax.put(
            taxObject
        )

        // ============================================================
        // DISCOUNT
        // ============================================================

        val discount = JSONObject()

        discount.put(
            "type",
            "F"
        )

        discount.put(
            "value",
            1
        )

        // ============================================================
        // SHIPPING ADDRESS
        // ============================================================

        val shippingAddress = JSONObject()

        shippingAddress.put(
            "type",
            "home"
        )

        shippingAddress.put(
            "line1",
            "sdfghjk"
        )

        shippingAddress.put(
            "line2",
            "oiuytr"
        )

        shippingAddress.put(
            "line3",
            "line3"
        )

        shippingAddress.put(
            "line4",
            "line4"
        )

        shippingAddress.put(
            "apartment",
            ""
        )

        shippingAddress.put(
            "building",
            ""
        )

        shippingAddress.put(
            "street",
            ""
        )

        shippingAddress.put(
            "avenue",
            ""
        )

        shippingAddress.put(
            "block",
            ""
        )

        shippingAddress.put(
            "area",
            ""
        )

        shippingAddress.put(
            "city",
            "salmyia"
        )

        shippingAddress.put(
            "state",
            "kuwait"
        )

        shippingAddress.put(
            "country",
            "kw"
        )

        shippingAddress.put(
            "zip_code",
            "30003"
        )

        shippingAddress.put(
            "postal_code",
            "30003"
        )

        // ============================================================
        // SHIPPING
        // ============================================================

        val shippingProvider = JSONObject()

        shippingProvider.put(
            "id",
            "prov_FFSFAGGAHAAJAJ"
        )

        val shippingDescriptionObject = JSONObject()

        shippingDescriptionObject.put(
            "text",
            "description"
        )

        shippingDescriptionObject.put(
            "lang",
            "en"
        )

        val shippingDescription = JSONArray()

        shippingDescription.put(
            shippingDescriptionObject
        )

        val recipientNameObject = JSONObject()

        recipientNameObject.put(
            "text",
            "Name"
        )

        recipientNameObject.put(
            "lang",
            "en"
        )

        val recipientName = JSONArray()

        recipientName.put(
            recipientNameObject
        )

        val shipping = JSONObject()

        shipping.put(
            "amount",
            1
        )

        shipping.put(
            "description",
            shippingDescription
        )

        shipping.put(
            "recipient_name",
            recipientName
        )

        shipping.put(
            "address",
            shippingAddress
        )

        shipping.put(
            "provider",
            shippingProvider
        )

        // ============================================================
        // ORDER METADATA
        // ============================================================

        val orderMetadata = JSONObject()

        orderMetadata.put(
            "o",
            "s"
        )

        // ============================================================
        // ORDER
        // ============================================================

        val order = JSONObject()

        /*
         * IMPORTANT:
         * amountKey exists in Preference XML.
         * Use it dynamically.
         */
        val amountString = getPrefStringValue(
            "amountKey",
            "1"
        ).trim()

        val amount: Any = if (amountString.isNotEmpty()) {
            try {
                BigDecimal(amountString)
            } catch (e: NumberFormatException) {
                // Keep the existing hardcoded fallback if preference
                // is empty/invalid.
                BigDecimal("1")
            }
        } else {
            BigDecimal("1")
        }

        order.put(
            "amount",
            amount
        )

        /*
         * IMPORTANT:
         * orderCurrencyKey is the actual preference for order.currency.
         * Do NOT use selectedCurrencyKey here.
         */
        val orderCurrency = getPrefStringValue(
            "orderCurrencyKey",
            "KWD"
        ).trim()

        order.put(
            "currency",
            if (orderCurrency.isNotEmpty()) {
                orderCurrency
            } else {
                "KWD"
            }
        )

        order.put(
            "description",
            orderDescription
        )

        order.put(
            "reference",
            getPrefStringValue(
                "orderRefrenceKey",
                ""
            )
        )

        order.put(
            "items",
            items
        )

     /*   order.put(
            "tax",
            tax
        )

        order.put(
            "discount",
            discount
        )

        order.put(
            "shipping",
            shipping
        )*/

        order.put(
            "metadata",
            orderMetadata
        )

        jsonObject.put(
            "order",
            order
        )

        // ============================================================
        // CUSTOMER
        // ============================================================

        val customerNameObject = JSONObject()

        customerNameObject.put(
            "first",
            "OSAMA"
        )

        customerNameObject.put(
            "last",
            "Ahmed"
        )

        customerNameObject.put(
            "middle",
            ""
        )

        customerNameObject.put(
            "title",
            "MR"
        )

        val customerName = JSONArray()

        customerName.put(
            customerNameObject
        )

        // Name on Card
        val nameOnCard = JSONObject()

        nameOnCard.put(
            "content",
            "OSAMA AHMED"
        )

        nameOnCard.put(
            "editable",
            true
        )

        // Phone
        val customerPhone = JSONObject()

        customerPhone.put(
            "country_code",
            "965"
        )

        customerPhone.put(
            "number",
            "51234567"
        )

        // Contact
        val customerContact = JSONObject()

        customerContact.put(
            "email",
            "buttonsdkd@tap.company"
        )

        customerContact.put(
            "phone",
            customerPhone
        )

        // Customer Address
        val customerAddress = JSONObject()

        customerAddress.put(
            "type",
            "home"
        )

        customerAddress.put(
            "line1",
            "sdfghjk"
        )

        customerAddress.put(
            "line2",
            "oiuytr"
        )

        customerAddress.put(
            "line3",
            "line3"
        )

        customerAddress.put(
            "line4",
            "line4"
        )

        customerAddress.put(
            "apartment",
            ""
        )

        customerAddress.put(
            "building",
            ""
        )

        customerAddress.put(
            "street",
            ""
        )

        customerAddress.put(
            "avenue",
            ""
        )

        customerAddress.put(
            "block",
            ""
        )

        customerAddress.put(
            "area",
            ""
        )

        customerAddress.put(
            "city",
            "salmyia"
        )

        customerAddress.put(
            "state",
            "kuwait"
        )

        customerAddress.put(
            "country",
            "kw"
        )

        customerAddress.put(
            "zip_code",
            "30003"
        )

        customerAddress.put(
            "postal_code",
            ""
        )

        // Customer
        val customer = JSONObject()

        customer.put(
            "id",
            getPrefStringValue(
                "customerIdKey",
                ""
            )
        )

        customer.put(
            "name",
            customerName
        )

        customer.put(
            "name_on_card",
            nameOnCard
        )

        customer.put(
            "contact",
            customerContact
        )

        customer.put(
            "address",
            customerAddress
        )

        jsonObject.put(
            "customer",
            customer
        )

        // ============================================================
        // RECEIPT
        // ============================================================

        val receipt = JSONObject()

        receipt.put(
            "email",
            false
        )

        receipt.put(
            "sms",
            false
        )

        jsonObject.put(
            "receipt",
            receipt
        )

        // ============================================================
        // CONFIGURATION
        // ============================================================

        val config = JSONObject()

        config.put(
            "initiator",
            "CHECKOUT"
        )

        /*
         * No preference exists for config.type.
         */
        config.put(
            "type",
            "BUTTON"
        )

        // ============================================================
        // FEATURES
        // ============================================================

        val features = JSONObject()

        features.put(
            "acceptance_badge",
            getPrefBooleanValue(
                "acceptanceBadgeKey",
                true
            )
        )

        features.put(
            "order",
            getPrefBooleanValue(
                "featureOrderKey",
                true
            )
        )

        features.put(
            "multiple_currencies",
            getPrefBooleanValue(
                "multipleCurrenciesKey",
                true
            )
        )

        // Currency Conversions
        // No preference exists for these individual flags.
        val currencyConversions = JSONObject()

        currencyConversions.put(
            "dynamic",
            true
        )

        currencyConversions.put(
            "location",
            true
        )

        currencyConversions.put(
            "payment",
            true
        )

        currencyConversions.put(
            "cobadge",
            true
        )

        features.put(
            "currency_conversions",
            currencyConversions
        )

        // Payments
        // No individual preference exists for these.
        val payments = JSONObject()

        payments.put("card", true)
        payments.put("device", true)
        payments.put("wallet", true)
        payments.put("bnpl", true)
        payments.put("mobile", true)
        payments.put("cash", true)
        payments.put("redirect", true)

        features.put(
            "payments",
            payments
        )

        // Alternative Card Inputs
        val alternativeCardInputs = JSONObject()

        alternativeCardInputs.put(
            "card_scanner",
            getPrefBooleanValue(
                "cardScannerKey",
                true
            )
        )

        alternativeCardInputs.put(
            "card_nfc",
            getPrefBooleanValue(
                "cardNfcKey",
                true
            )
        )

        features.put(
            "alternative_card_inputs",
            alternativeCardInputs
        )

        // Customer Cards
        val customerCards = JSONObject()

        customerCards.put(
            "save_card",
            getPrefBooleanValue(
                "displaySaveCardKey",
                true
            )
        )

        customerCards.put(
            "auto_save_card",
            getPrefBooleanValue(
                "displayAutosaveCardKey",
                true
            )
        )

        customerCards.put(
            "display_saved_cards",
            getPrefBooleanValue(
                "displaySavedCardsKey",
                true
            )
        )

        features.put(
            "customer_cards",
            customerCards
        )

        // ============================================================
        // ACCEPTANCE
        // ============================================================

        val acceptance = JSONObject()

        // Supported Regions
        val supportedRegions = JSONArray()

        getPrefStringSetValue(
            "supportedRegionsKey",
            setOf("LOCAL", "REGIONAL", "GLOBAL")
        ).forEach {
            supportedRegions.put(it)
        }

        acceptance.put(
            "supported_regions",
            supportedRegions
        )

        // Supported Currencies
        val supportedCurrencies = JSONArray()

        getPrefStringSetValue(
            "supportedCurrenciesKey",
            setOf(
                "KWD",
                "SAR",
                "AED",
                "OMR",
                "QAR",
                "BHD",
                "EGP",
                "GBP",
                "USD",
                "EUR"
            )
        ).forEach {
            supportedCurrencies.put(it)
        }

        acceptance.put(
            "supported_currencies",
            supportedCurrencies
        )


        // Supported Payment Methods
        val supportedPaymentMethods = JSONArray()

        val buttonType = getPrefStringValue(
            "buttonKey",
            "KNET"
        )

        supportedPaymentMethods.put(buttonType)

        acceptance.put(
            "supported_payment_methods",
            supportedPaymentMethods
        )
        // Supported Schemes
        val supportedSchemes = JSONArray()

        getPrefStringSetValue(
            "supportedSchemesKey",
            setOf(
                "MADA",
                "OMANNET",
                "VISA",
                "MASTERCARD",
                "AMEX",
                "BENEFIT_CARD"
            )
        ).forEach {
            supportedSchemes.put(it)
        }

        acceptance.put(
            "supported_schemes",
            supportedSchemes
        )

        // Supported Fund Source
        val supportedFundSource = JSONArray()

        getPrefStringSetValue(
            "supportedFundSourceKey",
            setOf("DEBIT", "CREDIT")
        ).forEach {
            supportedFundSource.put(it)
        }

        acceptance.put(
            "supported_fund_source",
            supportedFundSource
        )

        // Supported Payment Authentications
        val supportedPaymentAuthentications = JSONArray()

        getPrefStringSetValue(
            "supportedPaymentAuthenticationsKey",
            setOf(
                "3DS",
                "EMV",
                "PASSKEY"
            )
        ).forEach {
            supportedPaymentAuthentications.put(it)
        }

        acceptance.put(
            "supported_payment_authentications",
            supportedPaymentAuthentications
        )

        // Supported Payment Flows
        val supportedPaymentFlows = JSONArray()

        getPrefStringSetValue(
            "supportedPaymentFlowsKey",
            setOf(
                "POPUP",
                "PAGE"
            )
        ).forEach {
            supportedPaymentFlows.put(it)
        }

        acceptance.put(
            "supported_payment_flows",
            supportedPaymentFlows
        )

        // ============================================================
        // FIELD VISIBILITY
        // ============================================================

        val fieldVisibility = JSONObject()

        fieldVisibility.put(
            "name",
            getPrefBooleanValue(
                "displayHoldernameKey",
                true
            )
        )

        val card = JSONObject()

        card.put(
            "number",
            true
        )

        card.put(
            "expiry",
            true
        )

        card.put(
            "cvv",
            getPrefBooleanValue(
                "displayCVVKey",
                true
            )
        )

        card.put(
            "cardholder",
            getPrefBooleanValue(
                "displayHoldernameKey",
                true
            )
        )

        fieldVisibility.put(
            "card",
            card
        )

        val contactVisibility = JSONObject()

        contactVisibility.put(
            "email",
            true
        )

        contactVisibility.put(
            "number",
            true
        )

        fieldVisibility.put(
            "contact",
            contactVisibility
        )

        val shippingVisibility = JSONObject()

        shippingVisibility.put(
            "address",
            true
        )

        fieldVisibility.put(
            "shipping",
            shippingVisibility
        )

        // ============================================================
        // INTERFACE
        // ============================================================

        val interfaceObject = JSONObject()

        interfaceObject.put(
            "user_experience",
            "popup"
        )

        interfaceObject.put(
            "locale",
            getPrefStringValue(
                "selectedlangKey",
                "en"
            )
        )

        interfaceObject.put(
            "direction",
            getPrefStringValue(
                "selectedcardirectKey",
                "dynamic"
            )
        )

        interfaceObject.put(
            "card_direction",
            getPrefStringValue(
                "selectedcardirectKey",
                "ltr"
            )
        )

        interfaceObject.put(
            "edges",
            getPrefStringValue(
                "selectedcardedgeKey",
                "circular"
            )
        )

        interfaceObject.put(
            "theme",
            getPrefStringValue(
                "selectedthemeKey",
                "light"
            )
        )

        interfaceObject.put(
            "color_style",
            getPrefStringValue(
                "selectedcolorstyleKey",
                "coloured"
            )
        )

        interfaceObject.put(
            "loader",
            getPrefBooleanValue(
                "loaderKey",
                true
            )
        )

        // No preference exists for powered.
        interfaceObject.put(
            "powered",
            true
        )

        // ============================================================
        // CONFIG
        // ============================================================

        config.put(
            "features",
            features
        )

        config.put(
            "acceptance",
            acceptance
        )

        config.put(
            "field_visibility",
            fieldVisibility
        )

        config.put(
            "interface",
            interfaceObject
        )

        jsonObject.put(
            "config",
            config
        )

        // ============================================================
        // DOMAIN
        // ============================================================

        val domain = JSONObject()

        domain.put(
            "url",
            "tap.PayButtonSDK.demo"
        )

        jsonObject.put(
            "domain",
            domain
        )

        // ============================================================
        // REDIRECT
        // ============================================================

        val redirect = JSONObject()

        redirect.put(
            "url",
            getPrefStringValue(
                "redirectUrlKey",
                "demo.tap.PayButtonSDK"
            )
        )

        jsonObject.put(
            "redirect",
            redirect
        )

        // ============================================================
        // POST
        // ============================================================

        val post = JSONObject()

        post.put(
            "url",
            getPrefStringValue(
                "posturlKey",
                "demo.tap.PayButtonSDK"
            )
        )

        jsonObject.put(
            "post",
            post
        )

        // ============================================================
        // CHECKOUT
        // ============================================================

        val checkout = JSONObject()

        checkout.put(
            "auto",
            true
        )

        val checkoutMetadata = JSONObject()

        checkoutMetadata.put(
            "udf1",
            "test 1"
        )

        checkoutMetadata.put(
            "udf2",
            "test 2"
        )

        checkout.put(
            "metadata",
            checkoutMetadata
        )

        jsonObject.put(
            "checkout",
            checkout
        )

        // ============================================================
        // CONVERT JSON TO HASHMAP
        // ============================================================

        val intentObjc: HashMap<String, Any> = Gson().fromJson(
            jsonObject.toString(),
            object : TypeToken<HashMap<String?, Any?>?>() {}.type
        )

        // ============================================================
        // CONFIGURE PAY BUTTON
        // ============================================================

        if (intentId == null) {

            PayButtonConfiguration.configureWithPayButtonDictionary(
                this,
                publicKey,
                null,
                findViewById(R.id.redirect_pay),
                intentObjc,
                this
            )

        } else {

            PayButtonConfiguration.configureWithPayButtonDictionary(
                this,
                publicKey,
                intentId,
                findViewById(R.id.redirect_pay),
                null,
                this
            )
        }
    }

    override fun onPayButtonReady() {
        findViewById<TextView>(R.id.text).text = ""
        findViewById<TextView>(R.id.text).text = "onReady"
        Toast.makeText(this, "onReady", Toast.LENGTH_SHORT).show()
    }

    override fun onPayButtonSuccess(data: String) {

        Log.i("onSuccess", data)

        val formattedJson = try {
            JSONObject(data).toString(4)
        } catch (e: JSONException) {
            // In case the callback doesn't contain valid JSON
            data
        }

        val textView = findViewById<TextView>(R.id.text)

        textView.text = "onPayButtonSuccess>>"+"\n"+formattedJson
        textView.movementMethod = ScrollingMovementMethod()

        // Copy formatted JSON to clipboard
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText(
            "Pay Button Success JSON",
            formattedJson
        )

        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            this,
            "Success JSON copied to clipboard",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPayButtonClick() {
        Toast.makeText(this, "onClick", Toast.LENGTH_SHORT).show()
        findViewById<TextView>(R.id.text).text = ""
        findViewById<TextView>(R.id.text).text = "onClick "

    }


    override fun onPayButtonChargeCreated(data: String) {
        Log.e("data",data.toString())
        val formattedJson = try {
            JSONObject(data).toString(4)
        } catch (e: JSONException) {
            // In case the callback doesn't contain valid JSON
            data
        }
        findViewById<TextView>(R.id.text).text = ""
        findViewById<TextView>(R.id.text).text = "onChargeCreated"+"\n"+formattedJson
        Toast.makeText(this, "onChargeCreated $data", Toast.LENGTH_SHORT).show()

    }

    override fun onPayButtonOrderCreated(data: String) {
        val formattedJson = try {
            JSONObject(data).toString(4)
        } catch (e: JSONException) {
            // In case the callback doesn't contain valid JSON
            data
        }
        findViewById<TextView>(R.id.text).text = ""
        findViewById<TextView>(R.id.text).text = "onOrderCreated >>"+"\n"+formattedJson
        Log.e("mainactv", "onPayButtonOrderCreated: "+data )
        Toast.makeText(this, "onOrderCreated $data", Toast.LENGTH_SHORT).show()
    }

    override fun onPayButtoncancel() {
        Toast.makeText(this, "Cancel ", Toast.LENGTH_SHORT).show()
    }

    override fun onPayButtonError(error: String) {
        Log.e("error",error.toString())
        findViewById<TextView>(R.id.text).text = ""
        findViewById<TextView>(R.id.text).text = "onError $error"
        Toast.makeText(this, "onError received $error ", Toast.LENGTH_SHORT).show()

    }

    private fun callIntentAPI(){
        val builder: OkHttpClient.Builder = OkHttpClient().newBuilder()
        val interceptor = HttpLoggingInterceptor()
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        builder.addInterceptor(interceptor)

        val okHttpClient: OkHttpClient = builder.build()
        val mediaType = "application/json".toMediaType()
        val scope =intent.getStringExtra("scopeKey")
        val charge =intent.getStringExtra("scopeKey")



        val jsonObject = JSONObject()
        jsonObject.put("scope", scope)
        jsonObject.put("purpose", charge)
        jsonObject.put("statement_descriptor", "statement_descriptor")
        jsonObject.put("description", "sd")
        jsonObject.put("reference","uuid_testabcdfgkgdgd121992")
        jsonObject.put("customer_initiated", true)
        jsonObject.put("hash_string", "")
        jsonObject.put("idempotent", "")


        val merchant = JSONObject()
        val terminal = JSONObject()
        terminal.put("id","")
        val terminaldevice = JSONObject()
        terminaldevice.put("id","")
        terminal.put("terminal_device",terminaldevice)

        val medata = JSONObject()
        medata.put("uud","qq")
        medata.put("uud2","222")

        val operator = JSONObject()
        operator.put("id","")
        val device= JSONObject()
        device.put("id","")
        operator.put("device",device)
        merchant.put("id",intent.getStringExtra("merchantId"))
        merchant.put("terminal",terminal)
        merchant.put("operator",operator)
        val paymentprovider = JSONObject()
        val technology= JSONObject()
        technology.put("id","")

        val institution= JSONObject()
        institution.put("id","")

        paymentprovider.put("technology",technology)
        paymentprovider.put("institution",institution)

        val ddevelopmenthouse= JSONObject()
        ddevelopmenthouse.put("id","")

        val platform= JSONObject()
        platform.put("id","")

       // merchant.put("payment_provider",paymentprovider)
      //  merchant.put("institution",institution)
        merchant.put("development_house",ddevelopmenthouse)
        merchant.put("platform",platform)

        jsonObject.put("merchant", merchant)

        val authenticate = JSONObject()
        authenticate.put("id","")
        authenticate.put("required",true)

        jsonObject.put("authenticate", authenticate)
        val transaction = JSONObject()
        val cardholderlogin = JSONObject()
        cardholderlogin.put("type", "GUEST")
        cardholderlogin.put("timestamp", "123213213")

        val payment_agreement  = JSONObject()

        val contract = JSONObject()
        contract.put("id","")
        payment_agreement.put("id","")
        payment_agreement.put("contract",contract)

        transaction.put("card_holder_login",cardholderlogin)
        transaction.put("metadata",medata)
        transaction.put("reference","TEST")
        transaction.put("payment_agreement",payment_agreement)

        jsonObject.put("transaction", transaction)

        val invoice = JSONObject()
        invoice.put("id","")
        jsonObject.put("invoice", invoice)


        val descriptiom = JSONObject()
        descriptiom.put("text", "name ")
        descriptiom.put( "lang", "en ")

        val reference = JSONObject()
        reference.put("sku", "stock keeping unit ")
        reference.put( "gtin", "global trade item number ")
        reference.put("code", "00dfd ")
        reference.put("financial_code", "0022343 ")


        val descArray = JSONArray()
        descArray.put(descriptiom)

        val product = JSONObject()
        product.put("id","")
        product.put("amount",intent.getStringExtra("amountKey")?.toDouble())
        product.put("name",descArray)
        product.put("description",descArray)
        product.put("metadata",medata)
        product.put("category","PHYSICAL_GOODS")
        product.put("reference",reference)
        val  itemsList = JSONObject()
        itemsList.put("id", " ")
        itemsList.put("quantity", 1)
        itemsList.put("pickup", false)
        itemsList.put("product", product)

        val itemsArry =  JSONArray()
        itemsArry.put(itemsList)
        val items = JSONObject()
        items.put("count",1)
        items.put("list",itemsArry)



        val order = JSONObject()
        order.put("amount",(intent.getStringExtra("amountKey")?.toDouble()))
        // order.put("amount",3)
        order.put("currency",intent.getStringExtra("orderCurrencyKey"))
        order.put("description",descArray)
        order.put("reference",intent.getStringExtra("orderRefrenceKey"))
        order.put("items",items)

        val discount = JSONObject()
        discount.put("type","F")
        discount.put("value",1)

        val tax = JSONObject()
        tax.put( "name", "VAT")
        tax.put( "description", "test")
        tax.put( "type", "F")
        tax.put( "value", 1)

        val taxarry= JSONArray()
        taxarry.put(tax)


        val adddress = JSONObject()
        adddress.put("type","home ")
        adddress.put("line1", "sdfghjk ")
        adddress.put("line2", "oiuytr ")
        adddress.put("line3", "line3 ")
        adddress.put("line4","line4 ")
        adddress.put( "apartment", " ")
        adddress.put("building", " ")
        adddress.put("street", " ")
        adddress.put("avenue", " ")
        adddress.put("block", " ")
        adddress.put("area"," ")
        adddress.put("city", "salmyia")
        adddress.put("state", "kuwait")
        adddress.put("country", "KW")
        adddress.put("zip_code","30003")
        adddress.put("postal_code", " ")

        val provider = JSONObject()
        provider.put("id","")



        val shippigobject = JSONObject()
        shippigobject.put("amount",1)
        shippigobject.put("description",descArray)
        shippigobject.put("recipient_name",descArray)
        shippigobject.put("address",adddress)
        shippigobject.put("provider",provider)
        shippigobject.put("metadata",medata)

        // order.put("tax",taxarry)
        // order.put("discount",discount)
      //  order.put("shipping",shippigobject)
        order.put("metadata",medata)


        jsonObject.put("order",order)

        val name = JSONObject()
        name.put("first", "OSAMA ")
        name.put("last", "Ahmed ")
        name.put("middle", " ")
        name.put("title","MR ")

        val nameList = JSONArray()
        nameList.put(name)

        val nameCard = JSONObject()
        nameCard.put("content", "OSAMA AHMED ")
        nameCard.put("editable",true)

        val phone = JSONObject()
        phone.put("country_code","965")
        phone.put("number","55683784")

        val contact = JSONObject()
        contact.put("email","tap.test@company")
        contact.put("phone",phone)

        val customer = JSONObject()
        customer.put("id",intent.getStringExtra("customerIdKey"))
        customer.put("name",nameList)
        customer.put("name_on_card",nameCard)
        customer.put("contact",contact)




        customer.put("address",adddress)

        jsonObject.put("customer",customer)
        val receipt = JSONObject()
        receipt.put("email", false)
        receipt.put("sms", false)

        jsonObject.put("receipt", receipt)


        val configOb = JSONObject()
        configOb.put("initiator","CHECKOUT")
        configOb.put("type","BUTTON")
        val features = JSONObject()
        features.put("acceptance_badge",true)
        features.put("order",true)
        features.put("multiple_currencies",true)

        val currency_conversions = JSONObject()
        currency_conversions.put("dynamic", true)
        currency_conversions.put("location", true)
        currency_conversions.put("payment", true)
        currency_conversions.put("cobadge", true)

        val alternative_card_inputs = JSONObject()
        alternative_card_inputs.put("card_scanner", true)
        alternative_card_inputs.put("card_nfc ", true)

        val customer_cards = JSONObject()
        customer_cards.put("save_card", true)
        customer_cards.put("auto_save_card", true)
        customer_cards.put("display_saved_cards", true)


        val payments = JSONObject()
        payments.put("card", true)
        payments.put("device", true)
        payments.put("wallet", true)
        payments.put("bnpl", true)
        payments.put("mobile", true)
        payments.put("cash", true)
        payments.put("redirect", true)

        features.put("currency_conversions",currency_conversions)
        features.put("payments",payments)
        features.put("alternative_card_inputs",alternative_card_inputs)
        features.put("customer_cards",customer_cards)

        val acceptance = JSONObject()
        val supported_regions = JSONArray()
        supported_regions.put("LOCAL")
        supported_regions.put("REGIONAL")
        supported_regions.put("GLOBAL")

        val supported_countries = JSONArray()
        supported_countries.put("AE")
        supported_countries.put("SA")
        supported_countries.put("KW")
        supported_countries.put("EG")

        val supported_currencies = JSONArray()
        supported_currencies.put("KWD")
        supported_currencies.put( "SAR")
        supported_currencies.put("AED")
        supported_currencies.put("OMR")
        supported_currencies.put("QAR")
        supported_currencies.put("BHD")
        supported_currencies.put("EGP")
        supported_currencies.put("GBP")
        supported_currencies.put("USD")
        supported_currencies.put("EUR")
        supported_currencies.put("AED")



        val supported_payment_methods = JSONArray()
        supported_payment_methods.put("BENEFITPAY")

        /*val supported_payment_types = JSONArray()
        // supported_payment_methods.put ("CARD")
        supported_payment_types.put("DEVICE")
        supported_payment_types.put("WEB")*/

        val supported_schemes = JSONArray()
        // supported_schemes.put("CARD")
        supported_schemes.put( "MADA")
        supported_schemes.put("OMANNET")
        supported_schemes.put("VISA")
        supported_schemes.put( "MASTERCARD")
        supported_schemes.put("AMEX")
        supported_schemes.put("BENEFIT_CARD" )
        val defaultHash = hashSetOf("VISA","AMEX","MASTERCARD","BENEFIT_CARD","OMANNET","MADA")
        val defaultFundHash = hashSetOf("CREDIT","DEBIT")

        println("defaultHash"+defaultHash)

        val supported_fund_source = JSONArray()
        // supported_fund_source.put("CARD")
        supported_fund_source.put("DEBIT")
        supported_fund_source.put("CREDIT")
        val supported_payment_flows = JSONArray()
        //supported_payment_flows.put("CARD")
        supported_payment_flows.put("POPUP")
        supported_payment_flows.put( "PAGE")

        val supported_payment_authentications = JSONArray()
        supported_payment_authentications.put("3DS")
        supported_payment_authentications.put( "EMV")
        supported_payment_authentications.put( "PASSKEY")

       // acceptance.put("supported_regions",supported_regions)
       // acceptance.put("supported_countries",supported_countries)
       // acceptance.put("supported_payment_types",supported_payment_types)
       // acceptance.put("supported_currencies",supported_currencies)

        val paymethods = getPrefStringValue("buttonKey","KNET")

        val arry = JSONArray()
        arry.put(paymethods)

        acceptance.put("supported_payment_methods",arry)


        acceptance.put("supported_schemes",
            JSONArray(getPrefs().getStringSet("supportedSchemesKey", defaultHash.toHashSet()))
        )
        acceptance.put("supported_fund_source",
            JSONArray(getPrefs().getStringSet("supportedFundSourceKey", defaultFundHash.toHashSet()))
        )
        acceptance.put("supported_payment_authentications",supported_payment_authentications)
        acceptance.put("supported_payment_flows",supported_payment_flows)

        val interfacee = JSONObject()
        interfacee.put("locale",intent.getStringExtra("selectedlangKey") ?: "EN")
        interfacee.put("theme",intent.getStringExtra("selectedthemeKey") ?: "LIGHT")
        interfacee.put("edges",intent.getStringExtra("selectedcardedgeKey") ?: "CURVED")
        interfacee.put("color_style",intent.getStringExtra("selectedcolorstyleKey") ?:"COLORED")
        interfacee.put("user_experience","POPUP")
        interfacee.put("card_direction","DYNAMIC")
        interfacee.put("loader", true)
        interfacee.put("powered", true)


        val fieldvisibility = JSONObject()

        val card  = JSONObject()
        card.put( "number",true)
        card.put("expiry",true)
        card.put("cvv",true)
        card.put("cardholder", true)

        val conatct = JSONObject()
        conatct.put("email", true)
        conatct.put("number", true)

        val shipping  = JSONObject()
        shipping.put("address", true)

        fieldvisibility.put("name",true)
        fieldvisibility.put("card",card)
        fieldvisibility.put("contact",conatct)
      //  fieldvisibility.put("shipping",shipping)

        configOb.put("features",features)
        configOb.put("acceptance",acceptance)
        configOb.put("field_visibility",fieldvisibility)
        configOb.put("interface",interfacee)



        val checkout = JSONObject()
        checkout.put("auto", true)
        checkout.put("metadata", medata)

        val post = JSONObject()
        post.put("url","osama.cm")

        val redirect = JSONObject()
        redirect.put("url","osama.cm")

        val domain = JSONObject()
        domain.put("url","demo.tap.PayButtonSDK")

        jsonObject.put("config", configOb)
        jsonObject.put("domain",domain)
        jsonObject.put("redirect",redirect)
        jsonObject.put("post",post)
        jsonObject.put("checkout",checkout)

        val body = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request: Request = Request.Builder()
           // .url("https://api.tap.company/v2/intent")
            .url(" https://mw-sdk.dev.tap.company/v2/intent")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", getPrefStringValue("publicKey","pk_test_ohzQrUWRnTkCLD1cqMeudyjX"))
            .addHeader("mdn", "alcREQ7HvHCihdZ889eq/I2EbXyOecV5KN2IvpjMxP8U2yx/50UDy0R86CixOsC1TzsfW40AhiuT6G3aRui4OocT3EcKBpSjeXgDH6aJKhfTO33zbrK8ZA3eLZxKRwmvH9bugKRodb6lfG1PPN5dDZWiDqA6Je/suSr9hVUzrzg=")
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    var responseBody: JSONObject? =
                        response.body?.string()?.let { JSONObject(it) } // toString() is not the response body, it is a debug representation of the response body
                    println("responseBody>>"+responseBody)
                    if(!responseBody.toString().contains("errors")){
                        /*
                       *Pass to the sdk
                       ***/
                        var intentID:String? =null

                        intentID = responseBody?.getString("id")

                        configureSdk(intentID)


                    }else{


                        val errorObject = responseBody?.getJSONArray("errors")

                        for(j in 0 until errorObject?.length()!!){
                            val mediaEntryObj = errorObject.getJSONObject(j)
                            val description = mediaEntryObj.getString("description")
                            Handler(Looper.getMainLooper()).post {
                                // write your code here
                                Toast.makeText(this@MainActivity, description, Toast.LENGTH_SHORT).show()
                                finish()
                            }



                        }
                    }

                } catch (ex: JSONException) {
                    throw RuntimeException(ex)
                }
            }

        })
    }





}