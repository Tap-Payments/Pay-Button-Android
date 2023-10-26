package company.tap.paybutton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import company.tap.tappaybuttons.PayButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /**
         * operator
         */
        val operator = HashMap<String,Any>()

        operator.put("publicKey","pk_test_6jdl4Qo0FYOSXmrZTR1U5EHp")
        operator.put("hashString","")

        /**
         * metadata
         */
        val metada = HashMap<String,Any>()
        metada.put("id","")

        /**
         * order
         */

        val order = HashMap<String,Any>()
        order.put("id","")
        order.put("amount",  "1")
        order.put("currency","KWD")
        order.put("description", "")
        order.put("reference", "")
        order.put("metadata",metada)

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


        val interfacee = HashMap<String,Any>()
        interfacee.put("locale", "en")
        interfacee.put("theme", "light")
        interfacee.put("edges", "curved")
        interfacee.put("colorStyle","colored")
        interfacee.put("loader",true)


        val post = HashMap<String,Any>()
        post.put("url", "")

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


        findViewById<PayButton>(R.id.paybutton).initPayButton(this,configuration)


    }
}