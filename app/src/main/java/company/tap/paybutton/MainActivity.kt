package company.tap.paybutton

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.chillibits.simplesettings.tool.getPrefStringValue
import com.example.tappaybutton.PayButtonType
import company.tap.tappaybuttons.PayButton
import company.tap.tappaybuttons.PayButtonStatusDelegate

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /**
         * operator
         */
        val publicKey = intent.getStringExtra("publicKey")
        val hashStringKey = intent.getStringExtra("hashStringKey")
        val buttonKey = intent.getStringExtra("buttonKey")

        val operator = HashMap<String,Any>()

        operator.put("publicKey","pk_test_6jdl4Qo0FYOSXmrZTR1U5EHp")
        operator.put("hashString",hashStringKey.toString())
        Log.e("orderData","pbulc" + publicKey.toString() + " \nhash" + hashStringKey.toString())
        Log.e("buttonKey","buttonKey" + buttonKey.toString())

        /**
         * metadata
         */
        val metada = HashMap<String,Any>()
        metada.put("id","")

        /**
         * order
         */
        val ordrId =  intent.getStringExtra("orderIdKey")
        val orderDescription =  intent.getStringExtra("orderDescKey")
        val orderAmount =  intent.getStringExtra("amountKey")
        val orderRefrence =  intent.getStringExtra("orderTransactionRefrence")
        val selectedCurrency: String = intent.getStringExtra("selectedCurrencyKey").toString()


        val order = HashMap<String,Any>()
        order.put("id",ordrId.toString())
        order.put("amount",  if (orderAmount?.isEmpty() == true)"1" else orderAmount.toString() )
        order.put("currency",selectedCurrency)
        order.put("description",orderDescription ?: "")
        order.put("reference",orderRefrence ?: "")
        order.put("metadata",metada)
        Log.e("orderData","id" + ordrId.toString() + "  \n dest" + orderDescription.toString() +" \n orderamount " + orderAmount.toString() +"  \n orderRef" + orderRefrence.toString() + "  \n currency " + selectedCurrency.toString())


        /**
         * merchant
         */
        val merchant = HashMap<String,Any>()
        merchant.put("id", "")

        /**
         * invoice
         */
        val invoice = HashMap<String,Any>()
        invoice.put("id","")


        /**
         * phone
         */
        val phone = java.util.HashMap<String,Any>()
        phone.put("countryCode","+20")
        phone.put("number","011")


        /**
         * contact
         */
        val contact = java.util.HashMap<String,Any>()
        contact.put("email","test@gmail.com")
        contact.put("phone",phone)
        /**
         * name
         */
        val name = java.util.HashMap<String,Any>()
        name.put("lang","en")
        name.put("first", "first")
        name.put("middle", "middle")
        name.put("last","last")

        /**
         * customer
         */
        val customer = java.util.HashMap<String,Any>()
        customer.put("id", "")
        customer.put("contact",contact)
        customer.put("name", listOf(name))

        /**
         * interface
         */

        val selectedLanguage: String? =  intent.getStringExtra("selectedlangKey")
        val selectedTheme: String? = intent.getStringExtra("selectedthemeKey")
        val selectedCardEdge = intent.getStringExtra("selectedcardedgeKey")
        val selectedColorStylee = intent.getStringExtra("selectedcolorstyleKey")
        val loader = intent.getBooleanExtra("loaderKey",false)

        Log.e("interfaceData",selectedTheme.toString() + "language" + selectedLanguage.toString() + "cardedge " + selectedCardEdge.toString() +" loader" + loader.toString() + "selectedColorStylee " + selectedColorStylee.toString())
        val interfacee = HashMap<String,Any>()
        interfacee.put("locale",selectedLanguage ?: "en")
        interfacee.put("theme",selectedTheme ?: "light")
        interfacee.put("edges",selectedCardEdge ?: "curved")
        interfacee.put("colorStyle",selectedColorStylee ?:"colored")
        interfacee.put("loader",loader)

        val posturl =  intent.getStringExtra("posturlKey")
        val redirectUrl =  intent.getStringExtra("redirectUrlKey")


        val post = HashMap<String,Any>()
        post.put("url",posturl?: "")

        val redirect = HashMap<String,Any>()
        redirect.put("url","onTapKnetRedirect://")
        val configuration = LinkedHashMap<String,Any>()


        /**
         * transaction && scope
         */



        val authorize = HashMap<String,Any>()
        authorize.put("type","")
        authorize.put("time","")
        val transaction = HashMap<String,Any>()

        transaction.put("reference","")
        transaction.put("authorize","")


        /**
         * configuration
         */

        configuration.put("operator",operator)
        configuration.put("order",order)
        configuration.put("customer",customer)
        configuration.put("merchant",merchant)
        configuration.put("invoice",invoice)
        configuration.put("interface",interfacee)
        configuration.put("post",post)
        configuration.put("redirect",redirect)
        configuration.put("scope","charge")
        configuration.put("transaction",transaction)


        findViewById<PayButton>(R.id.paybutton).initPayButton(this, configuration,
            PayButtonType.valueOf(buttonKey.toString()),object :PayButtonStatusDelegate{
            override fun onSuccess(data: String) {
                Toast.makeText(this@MainActivity,"success ${data}",Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: String) {
                Toast.makeText(this@MainActivity,"error ${error}",Toast.LENGTH_SHORT).show()
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity,"cancel",Toast.LENGTH_SHORT).show()
            }

            override fun onChargeCreated(data: String) {
                Toast.makeText(this@MainActivity,"charge created ${data}",Toast.LENGTH_SHORT).show()

            }

            override fun onClick() {
                Toast.makeText(this@MainActivity,"click",Toast.LENGTH_SHORT).show()
            }

            override fun onReady() {
                Toast.makeText(this@MainActivity,"ready",Toast.LENGTH_SHORT).show()
            }

            override fun onOrderCreated(data: String) {
                Toast.makeText(this@MainActivity,"order created ${data}",Toast.LENGTH_SHORT).show()
            }

        })


    }

    private fun getPayButtonType(key: String): PayButtonType {

        return when (getPrefStringValue(key, PayButtonType.KNET.name)) {
            PayButtonType.KNET.name-> PayButtonType.KNET
            PayButtonType.BENEFIT_PAY.name -> PayButtonType.BENEFIT_PAY

            else -> PayButtonType.KNET
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        finish()
        startActivity(intent)

    }

}