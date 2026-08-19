package company.tap.paybutton


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chillibits.simplesettings.tool.getPrefBooleanValue
import com.chillibits.simplesettings.tool.getPrefStringSetValue
import com.chillibits.simplesettings.tool.getPrefStringValue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.PayButtonConfiguration
import company.tap.tappaybutton.PayButtonStatusDelegate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal


class MainActivity : AppCompatActivity() ,PayButtonStatusDelegate{
    lateinit var payButton: PayButton

    /**
     * A payload edited by hand in the json editor.
     *
     * Null until something is saved there, and from then on it is what configures the button
     * and what the sdk creates the intent from .. the editor is not a preview
     */
    private var intentJsonOverride: JSONObject? = null

    /** Everything the sdk has reported, newest first, the way the iOS example keeps it */
    private val eventLog = StringBuilder()

    /** Opens the json editor and takes back what was saved */
    private val intentJsonEditor = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val edited: String? = result.data?.getStringExtra(IntentJsonEditorActivity.EXTRA_JSON)
        if (result.resultCode != RESULT_OK || edited.isNullOrBlank()) return@registerForActivityResult

        intentJsonOverride = try {
            JSONObject(edited)
        } catch (error: JSONException) {
            appendEvent("intent json rejected", error.message ?: "")
            null
        }

        if (intentJsonOverride != null) {
            appendEvent("intent json saved", "the button is being configured with it")
            configureSdk(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
         * Who creates the intent, from the settings screen's "Created by" row.
         *
         * Either way it is built from the same payload: the sdk is handed the dictionary and
         * posts it, or the app posts it here and hands the sdk the id that came back
         */
        applyWindowInsets()

        startPayment()

        findViewById<android.widget.Button>(R.id.refresh).setOnClickListener {
            // The intent a payment used is spent, so running another one needs a new intent.
            // Nothing does this on its own .. starting over by itself took the result off screen
            // and replaced the button under whoever was reading it
            appendEvent("starting over")
            it.visibility = android.view.View.GONE
            startPayment()
        }

        findViewById<android.widget.Button>(R.id.options).setOnClickListener {
            showOptions()
        }
    }

    /**
     * Keeps the screen out from under the status bar and the navigation bar.
     *
     * Targeting sdk 35 and up, Android lays every app out edge to edge whether it asked to or
     * not, so the top of this screen sat behind the clock and the camera cutout. The cutout is
     * asked for alongside the bars because on a phone with a punch hole the two do not always
     * cover the same strip
     */
    private fun applyWindowInsets() {
        val root: android.view.View = findViewById(R.id.root)
        val spacing: Int = (16 * resources.displayMetrics.density).toInt()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                        androidx.core.view.WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                view.paddingLeft,
                bars.top + spacing,
                view.paddingRight,
                bars.bottom + spacing
            )
            insets
        }
    }

    /**
     * Creates an intent and hands the button its payload.
     *
     * Called on open and again from Refresh. An intent is spent once it has been paid, so a
     * second payment needs a second intent, not just a reloaded button
     */
    private fun startPayment() {
        // Adds to the log rather than replacing it .. a start over is not a reason to lose what
        // the payment before it did
        appendEvent(
            "Creating an intent",
            "button ${selectedPaymentMethod()}, " +
                    "currency ${orderCurrencyForTheButton()}, " +
                    "amount ${orderAmountForTheButton()}, " +
                    "key ${examplePublicKey()}"
        )

        // Nothing to press until the intent exists, so the button waits out of sight rather than
        // sitting there attached to nothing
        findViewById<company.tap.tappaybutton.PayButton>(R.id.redirect_pay).visibility =
            android.view.View.INVISIBLE

        // One way to create an intent. The demo hands the sdk the payload and the sdk creates it,
        // which is the path a merchant integrating the button actually takes .. a second route
        // through the demo's own http call only meant two things to keep working
        configureSdk(null)
    }

    /** Brings the button back once there is something behind it, or once there never will be */
    private fun showTheButton() {
        findViewById<company.tap.tappaybutton.PayButton>(R.id.redirect_pay).visibility =
            android.view.View.VISIBLE
    }

    /** Offers a way to run the next payment, once this one has ended however it ended */
    private fun offerToStartOver() {
        findViewById<android.widget.Button>(R.id.refresh).visibility = android.view.View.VISIBLE
    }

    /**
     * The Options menu from the iOS example, as an Android dialog.
     *
     * Same four things: take the log away with you, clear it, change the configuration, or
     * edit the payload by hand
     */
    private fun showOptions() {
        val actions = arrayOf(
            getString(R.string.copy_logs),
            getString(R.string.clear_logs),
            getString(R.string.configs),
            getString(R.string.edit_intent_json)
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> copyLogs()
                    1 -> clearLogs()
                    2 -> startActivity(android.content.Intent(this, SettingsActivity::class.java))
                    3 -> editIntentJson()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyLogs() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Pay Button logs", eventLog.toString()))
        Toast.makeText(this, "Logs copied", Toast.LENGTH_SHORT).show()
    }

    private fun clearLogs() {
        eventLog.setLength(0)
        findViewById<TextView>(R.id.text).text = ""
    }

    /** Opens the payload that is about to be used, so it can be changed before it is */
    private fun editIntentJson() {
        val editing: String = try {
            currentIntentJson().toString(4)
        } catch (error: JSONException) {
            currentIntentJson().toString()
        }

        intentJsonEditor.launch(
            android.content.Intent(this, IntentJsonEditorActivity::class.java)
                .putExtra(IntentJsonEditorActivity.EXTRA_JSON, editing)
        )
    }

    /**
     * Records an event the way the iOS example does: newest first, each one separated, and
     * nothing thrown away. The old screen replaced the text on every callback, so an event
     * was only readable until the next one arrived
     * @param name The callback that fired
     * @param data Whatever it carried, pretty printed when it is json
     */
    private fun appendEvent(name: String, data: String = "") {
        val body: String = if (data.isBlank()) "" else " " + prettyJson(data)
        eventLog.insert(0, "\n\n========\n\n$name$body")

        val textView = findViewById<TextView>(R.id.text)
        textView.text = eventLog.toString()
        textView.movementMethod = ScrollingMovementMethod()
    }

    /** Pretty prints a payload, or hands it back as it is when it is not json */
    private fun prettyJson(data: String): String = try {
        JSONObject(data).toString(4)
    } catch (error: JSONException) {
        data
    }

    /*
     * ---------------------------------------------------------------------
     * WHAT EACH BUTTON NEEDS ASKING FOR
     * ---------------------------------------------------------------------
     *
     * Some sandbox buttons will not render an intent that was not built the way they want it.
     * Ported from the iOS example, which keeps the same four tables.
     *
     * iOS has to remember the value it replaced and put it back when the button changes, because
     * it edits one long lived model. This rebuilds the payload from the preferences on every
     * call, so an override lives only as long as the payload does .. what was typed in the
     * settings is still there the moment the button changes back. No remembering needed.
     */

    /** The public key most of the sandbox buttons run on */
    private val defaultPublicKey = "pk_test_YhUjg9PNT8oDlKJ1aE2fMRz7"

    /**
     * The methods that live on a merchant account of their own in the sandbox, and the key each
     * is enabled on. Asking for one of these with the default key gets an intent the button
     * cannot render, so the key follows the button rather than the other way round
     */
    private val publicKeysByPaymentMethod = mapOf(
        "DEEMA" to "pk_test_KTTwK3QmcWVf9v1pRtl5EFHyXgqxS",
        "TAMARA" to "pk_test_5TkexzQXSKCM4RcWUnJPoqbH"
    )

    /**
     * The currency a button has to be asked for in, when it only takes one. Paypal is not enabled
     * on the sandbox merchant's local currency, so an order in anything else is refused
     */
    private val currenciesByPaymentMethod = mapOf(
        "PAYPAL" to "USD",
        "DEEMA" to "KWD",
        "KNET" to "KWD",
        // Fawry settles in Egypt and takes nothing else
        "FAWRY" to "EGP"
    )

    /**
     * The amount a button has to be asked for, when it only works above or around one. Deema
     * finances the order rather than charging it, so a couple of dinars is below what it offers on
     */
    private val amountsByPaymentMethod = mapOf(
        "DEEMA" to "40"
    )

    /** The method the intent is about to ask for */
    private fun selectedPaymentMethod(): String =
        getPrefStringValue("buttonKey", "KNET").trim().uppercase()

    /**
     * The key the picked button is created and configured with. Read everywhere a key is needed,
     * so picking a button in the settings is the only thing that has to change
     */
    private fun examplePublicKey(): String =
        publicKeysByPaymentMethod[selectedPaymentMethod()]
            ?: getPrefStringValue("publicKey", defaultPublicKey)

    /**
     * The merchant id to send.
     *
     * Empty for a button that came with a key of its own .. the id in the settings belongs to the
     * account the default key opens, and sending it alongside deema's or tamara's key names a
     * merchant that key has no business with. Sent empty, the mw resolves it from the key
     */
    private fun merchantIdForTheButton(): String =
        if (publicKeysByPaymentMethod.containsKey(selectedPaymentMethod())) ""
        else getPrefStringValue("merchantId", "1124340")

    /** The order currency, which the button overrides when it only takes one */
    private fun orderCurrencyForTheButton(): String =
        currenciesByPaymentMethod[selectedPaymentMethod()]
            ?: getPrefStringValue("orderCurrencyKey", "KWD").trim().ifEmpty { "KWD" }

    /** The order amount, which the button overrides when it only works around one */
    private fun orderAmountForTheButton(): BigDecimal {
        val asked: String = amountsByPaymentMethod[selectedPaymentMethod()]
            ?: getPrefStringValue("amountKey", "1").trim()
        return asked.toBigDecimalOrNull() ?: BigDecimal("1")
    }

    /**
     * The one place the intent payload is built.
     *
     * It was built in two places, here and in a second http call the demo made itself, and the
     * two had drifted .. so the payload the button was configured with was not the payload that
     * would have been posted to create the intent. That second route is gone and this is the one
     * builder, read through `currentIntentJson` by the button and by the json editor alike.
     */
    private fun buildIntentJson(): JSONObject {

        /**
         * Pay Button Configuration
         */
        val jsonObject = JSONObject()


        // ============================================================
        // BASIC INFORMATION
        // ============================================================

        // No preference exists for scope in the XML except scopeKey.
        // Keep CHARGE as the fallback/default behavior.
        jsonObject.put(
            "scope",
            // The list offers CHARGE, AUTHORIZE, TOKEN and the rest, all upper case
            getPrefStringValue("scopeKey", "CHARGE")
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

        // Emptied for a button that carries its own key, see merchantIdForTheButton
        merchant.put(
            "id",
            merchantIdForTheButton()
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
        // Read from the preference, like the order amount below. It used to come from an intent
        // extra, which meant the product carried no amount at all whenever this screen was not
        // opened from the settings screen .. Refresh, or a launch straight into it
        // The same amount the order carries, overrides included
        product.put(
            "amount",
            orderAmountForTheButton()
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

        val amount: BigDecimal = orderAmountForTheButton()

        order.put(
            "amount",
            amount
        )

        order.put(
            "currency",
            orderCurrencyForTheButton()
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
            getPrefBooleanValue(
                "receiptEmailKey",
                false
            )
        )

        receipt.put(
            "sms",
            getPrefBooleanValue(
                "receiptSmsKey",
                false
            )
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

        // Its own row now. `name` and `card.cardholder` are separate rows on iOS, and both
        // were reading the card holder toggle here
        fieldVisibility.put(
            "name",
            getPrefBooleanValue(
                "fieldNameKey",
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
            getPrefBooleanValue(
                "contactEmailKey",
                true
            )
        )

        contactVisibility.put(
            "number",
            getPrefBooleanValue(
                "contactNumberKey",
                true
            )
        )

        fieldVisibility.put(
            "contact",
            contactVisibility
        )

        val shippingVisibility = JSONObject()

        shippingVisibility.put(
            "address",
            getPrefBooleanValue(
                "shippingAddressKey",
                true
            )
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
            getPrefStringValue(
                "userExperienceKey",
                "popup"
            )
        )

        interfaceObject.put(
            "locale",
            getPrefStringValue(
                "selectedlangKey",
                "en"
            )
        )

        // Its own row now. This and card_direction were both reading the card direction,
        // so the two payload fields could never hold different values
        interfaceObject.put(
            "direction",
            getPrefStringValue(
                "selecteddirectionKey",
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
                // Was "coloured", which is not one of the values the list offers
                "colored"
            )
        )

        interfaceObject.put(
            "loader",
            getPrefBooleanValue(
                "loaderKey",
                true
            )
        )

        interfaceObject.put(
            "powered",
            getPrefBooleanValue(
                "poweredKey",
                true
            )
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
            getPrefBooleanValue(
                "checkoutAutoKey",
                true
            )
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

        return jsonObject
    }

    /**
     * The payload every caller uses.
     *
     * Whatever was saved in the json editor wins, so what the editor shows is exactly what
     * goes on to create the intent. With nothing saved it is the payload the settings screen
     * describes
     */
    private fun currentIntentJson(): JSONObject = intentJsonOverride ?: buildIntentJson()

    /**
     * Hands the button the payload and lets the sdk create the intent from it
     * @param intentId An intent the app made itself, or null to have the sdk create one
     */
    fun configureSdk(intentId: String?) {

        // Deema and tamara live on keys of their own, see examplePublicKey
        val publicKey = examplePublicKey()

        if (intentId == null) {

            val intentObjc: HashMap<String, Any> = Gson().fromJson(
                currentIntentJson().toString(),
                object : TypeToken<HashMap<String?, Any?>?>() {}.type
            )

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

    /*
     * Every callback the sdk fires, recorded into the same log the iOS example keeps.
     *
     * They used to replace the text view, so an event was readable only until the next one
     * arrived .. which for a payment that reports charge created, then 3ds, then success
     * meant the interesting part was already gone by the time you looked.
     */

    override fun onPayButtonReady() {
        appendEvent("onReady")
        // A button that reports no height, ex the redirect based ones, arrives here instead
        showTheButton()
    }

    override fun onPayButtonClick() {
        appendEvent("onClicked")
    }

    override fun onPayButtonSuccess(data: String) {
        Log.i("onSuccess", data)
        appendEvent("onSuccess", data)

        // Kept from before: the payload is usually wanted somewhere else
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Pay Button Success JSON", prettyJson(data))
        )
        Toast.makeText(this, "Success JSON copied to clipboard", Toast.LENGTH_SHORT).show()
        offerToStartOver()
    }

    override fun onPayButtonChargeCreated(data: String) {
        Log.i("onChargeCreated", data)
        appendEvent("onChargeCreated", data)
    }

    override fun onPayButtonOrderCreated(data: String) {
        Log.i("onOrderCreated", data)
        appendEvent("onOrderCreated", data)
    }

    override fun onPayButtoncancel() {
        appendEvent("onCanceled")
        offerToStartOver()
    }

    override fun onPayButtonError(error: String) {
        Log.e("onError", error)
        appendEvent("onError", error)
        // The intent may have failed before the button ever rendered. Leaving it hidden would
        // leave the screen empty with no way to try again
        showTheButton()
        offerToStartOver()
    }

    override fun onPayButtonThreeDSRedirect(data: String) {
        Log.i("onThreeDSRedirect", data)
        appendEvent("onThreeDSRedirect", data)
    }

    override fun onPayButtonScannerClick() {
        // The card form asks, the host app owns the camera
        appendEvent("onScannerClick")
    }

    override fun onPayButtonNfcClick() {
        // The card form asks, the host app owns the nfc reader
        appendEvent("onNfcClick")
    }

    override fun onPayButtonBindIdentification(data: String) {
        appendEvent("onBinIdentification", data)
    }

    override fun onPayButtonHeightChange(heightChange: String) {
        // The first height report is the page telling us it has painted something .. its own
        // loading skeleton, at the minimum height. That is the moment iOS has the button on
        // screen, so it is the moment to show it here. Waiting for onReady instead hid the whole
        // page load, and the payer watched an empty gap where iOS shows a shimmering placeholder
        showTheButton()

        // The button resizes itself. This is only worth implementing when the host layout
        // pins it to a fixed height, which this one does not
        appendEvent("onHeightChange", heightChange)
    }






}