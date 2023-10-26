package company.tap.paybutton

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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


        findViewById<PayButton>(R.id.paybutton).initPayButton(this, configuration,
            PayButtonType.BENEFIT_PAY,object :PayButtonStatusDelegate{
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

    override fun onBackPressed() {
        super.onBackPressed()
        //   val intent = Intent(this, SettingsActivity::class.java)

        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        finish()
        startActivity(intent)

    }

}