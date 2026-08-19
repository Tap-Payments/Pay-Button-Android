
package company.tap.tappaybutton


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.os.postDelayed
import com.example.tappaybutton.R
import com.google.gson.Gson
import company.tap.tappaybutton.ApiService.BASE_URL_1
import company.tap.tappaybutton.PayButtonConfiguration.Companion.payButonurlFormat
import company.tap.tappaybutton.enums.SCHEMES
import company.tap.tappaybutton.enums.TapRedirectStatusDelegate
import company.tap.tappaybutton.enums.ThreeDsPayButtonType
import company.tap.tappaybutton.enums.careemPayUrlHandler
import company.tap.tappaybutton.enums.intentKey
import company.tap.tappaybutton.enums.keyValueName
import company.tap.tappaybutton.enums.operatorKey
import company.tap.tappaybutton.enums.publicKeyToGet
import company.tap.tappaybutton.models.CardRedirection
import company.tap.tappaybutton.models.Redirection
import company.tap.tappaybutton.paybuttonsdk.PayButtonPopupChromeClient
import company.tap.tappaybutton.paybuttonsdk.decidePolicyFor
import company.tap.tappaybutton.paybuttonsdk.isPasskeyNavigation
import company.tap.tappaybutton.paybuttonsdk.startFidoAuthentication
import company.tap.tappaybutton.threeDsWebview.ThreeDsWebViewActivityButton
import company.tap.tappaybutton.views.ThreeDSPasskeySession
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import kotlin.collections.HashMap
import android.os.Message


@SuppressLint("ViewConstructor")
class PayButton : LinearLayout , ApplicationLifecycle {
    lateinit var webviewStarterUrl: String
    internal var isBenefitPayUrlIntercepted = false
    // lateinit var webViewScheme: String
    var webViewScheme: String = "tapbuttonsdk://"
    // private lateinit var webChrome: WebChrome
    internal lateinit var webChrome: PayButtonPopupChromeClient

    private var popupWebView: WebView? = null
    private var popupDialog: Dialog? = null
    private var popupContainer: FrameLayout? = null
    lateinit var webViewFrame: FrameLayout
    lateinit var urlToBeloaded: String
    var firstTimeOnReadyCallback = true
    lateinit var linearLayout: LinearLayout
    lateinit var dialog: Dialog
    lateinit var redirectConfiguration: java.util.HashMap<String, Any>
    lateinit var headersVal: Headers
    lateinit var publickKeyVal: String
    lateinit var intentVal: String
    internal var iSAppInForeground = true
    internal var onSuccessCalled = false

    /**
     * The payload of the last success, and whether one landed.
     *
     * A cancel can arrive right behind a success in some flows, and this is how the button
     * tells the two apart. Was `pair`
     */
    internal var successPayload = Pair("", false)

    /**
     * The last redirection the card form announced, kept for its return url. A passkey
     * challenge that arrives as a plain navigation carries no details of its own.
     * Mirrors `lastCardRedirection`
     */
    internal var lastCardRedirection: CardRedirection? = null

    /**
     * Runs a passkey authentication in the system browser. Held for the lifetime of the
     * process so a second announcement of the same challenge does not start a second browser
     */
    internal var threeDSPasskeySession: ThreeDSPasskeySession? = null
    private  val SAMSUNG_PAY_URL_PREFIX: String = "samsungpay"
    private  val SAMSUNG_APP_STORE_URL: String = "samsungapps://ProductDetail/com.samsung.android.spay"
    private var paymentResultReceived = false
    private var passkeyBrowserOpened = false
    companion object {
        /**
         * The redirection the shared buttons are currently authenticating for.
         *
         * Optional, the way the Swift models are. `lateinit` turned "no redirection is
         * running" into a crash at the point of reading rather than a value to check
         */
        @JvmStatic
        var threeDsResponse: Redirection? = null

        /** The redirection the card form is currently authenticating for */
        @JvmStatic
        var threeDsResponseCardPayButtons: CardRedirection? = null

        internal lateinit var redirectWebView: WebView

        lateinit var buttonTypeConfigured: ThreeDsPayButtonType
        fun cancel() {
            redirectWebView.loadUrl("javascript:window.cancel()")
        }

        fun generateTapAuthenticate(authIdPayerUrl: String) {
            redirectWebView.loadUrl("javascript:window.loadAuthentication('$authIdPayerUrl')")
        } fun generateTapAuthenticater(authIdPayerUrl: String) {
            redirectWebView.loadUrl("javascript:window.loadAuthernticate('$authIdPayerUrl')")
        }

        fun retrieve(value: String) {
            redirectWebView.loadUrl("javascript:window.retrieve('$value')")
        }


    }

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
        LayoutInflater.from(context).inflate(R.layout.activity_button_web_wrapper, this)
        initWebView()

    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        redirectWebView = findViewById(R.id.webview)
        webViewFrame = findViewById(R.id.webViewFrame)

        with(redirectWebView) {

            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true

                // Required for window.open()
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)

                allowContentAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
            }
        }

        redirectWebView.setBackgroundColor(Color.TRANSPARENT)
        redirectWebView.setLayerType(LAYER_TYPE_SOFTWARE, null)

        webChrome = PayButtonPopupChromeClient(this)
        redirectWebView.webChromeClient = webChrome
        redirectWebView.webViewClient = MyWebViewClient()
    }


    private fun callIntentRetereiveAPI(
        configuraton: java.util.HashMap<String, Any>,
        headers: Headers
    ) {
        try {
            val intentObj = configuraton["intent"] as HashMap<*, *>
            val intentID = intentObj["intent"]

            val baseURL = BASE_URL_1 + "intent/" + intentID + "/sdk"

            val builder = OkHttpClient.Builder()

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(interceptor)

            val operator = configuraton[operatorKey] as HashMap<*, *>
            val publickKey = operator[publicKeyToGet]?.toString()

            println("publickKey>>$publickKey")

            /*
             * Build sdk_info exactly according to the cURL structure:
             *
             * {
             *   "sdk_info": {
             *      "type": "button",
             *      "authorization": "...",
             *      "version": "2.2.0",
             *      "mdn": "...",
             *      "application": "..."
             *   }
             * }
             */
            val sdkInfo = JSONObject()

            sdkInfo.put("type", "button")
            sdkInfo.put("authorization", publickKey)
            sdkInfo.put("version", "2.2.0")
            sdkInfo.put("mdn", headers.mdn.toString())
            sdkInfo.put("application", headers.application.toString())

            /*
             * Root request object
             */
            val jsonObject = JSONObject()
            jsonObject.put("sdk_info", sdkInfo)

            println("Intent SDK Request >> $jsonObject")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonObject.toString().toRequestBody(mediaType)

            val okHttpClient = builder.build()

            val request = Request.Builder()
                .url(baseURL)
                .put(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", publickKey.toString())
                .addHeader("mdn", headers.mdn.toString().trim())
                .addHeader("application", headers.application.toString())
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    try {

                        val responseString = response.body?.string()

                        println("Intent SDK Response >> $responseString")

                        val responseBody = responseString?.let {
                            JSONObject(it)
                        }

                        if (responseBody != null && !responseBody.toString().contains("errors")) {

                            val intentIdResponse = responseBody.optString("id", null)

                            if (!intentIdResponse.isNullOrEmpty()) {

                                val payBtnUrl = payButonurlFormat?.replace("%@", "%s")

                                urlToBeloaded = payBtnUrl?.let {
                                    String.format(
                                        it,
                                        intentIdResponse,
                                        publickKey.toString(),
                                        toBase64(headers.mdn.toString())
                                    )
                                }.orEmpty()

                                Handler(Looper.getMainLooper()).post {
                                    redirectWebView.loadUrl(urlToBeloaded)
                                }
                            }

                            println("ButtonURL >> $urlToBeloaded")

                        } else {
                            println("Intent SDK API returned errors >> $responseBody")
                        }

                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }
            })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun callIntentAPI(configuraton: java.util.HashMap<String, Any>, headers: Headers) {
        try {
            // Convert the HashMap to a JSON string using Gson
            val gson = Gson()
            val jsonString = gson.toJson(configuraton)
            val mediaType = "application/json; charset=utf-8".toMediaType()
// Create the RequestBody with the JSON string
            val requestBody = jsonString.toRequestBody(mediaType)
            val operator = HashMap<String, String>()
            operator["publicKey"] = publickKeyVal
            val builder: OkHttpClient.Builder = OkHttpClient().newBuilder()
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            builder.addInterceptor(interceptor)
            val okHttpClient: OkHttpClient = builder.build()
            val request: Request = Request.Builder()
                .url(BASE_URL_1+"intent")
                .method("POST", requestBody)
                .addHeader("Content-Type", "application/json")
                // .addHeader("Authorization", "pk_test_ohzQrUWRnTkCLD1cqMeudyjX")
                .addHeader("Authorization", publickKeyVal)
                .addHeader("mdn", headers.mdn.toString().trim())
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


                            //  val operator = configuraton?.get(operatorKey) as HashMap<*, *>
                            //  val publickKey = operator.get(publicKeyToGet)

                            /**
                             * intent
                             */
                            val intentObj = HashMap<String,Any>()
                            intentID = responseBody?.getString("id")
                            if (intentID != null) {
                                intentObj.put("intent",intentID)
                            }
                            /**
                             * configuration
                             */

                            val configuration = LinkedHashMap<String,Any>()

                            configuration.put("operator",operator)
                            configuration.put("intent",intentObj)

                            callIntentRetereiveAPI(configuration,headers)


                        }else{


                            val errorObject = responseBody?.getJSONArray("errors")

                            for(j in 0 until errorObject?.length()!!){
                                val mediaEntryObj = errorObject.getJSONObject(j)
                                val description = mediaEntryObj.getString("description")
                                Handler(Looper.getMainLooper()).post {
                                    // write your code here
                                    PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonError(description)
                                }



                            }
                        }

                    } catch (ex: JSONException) {
                        throw RuntimeException(ex)
                    }
                }

            })
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    fun init(configuraton: java.util.HashMap<String, Any>?, headers: Headers,_intentId : String?, _publickey:String?) {

        if (configuraton != null) {
            redirectConfiguration = configuraton
        }
        headersVal = Headers(headers.mdn,headers.application)
        if (_intentId != null) {
            intentVal = _intentId
        }
        if (_publickey != null) {
            publickKeyVal = _publickey
        }
        //  initializePaymentData(buttonType)
        /**
         * Check for data in configuration has operator and intent id
         * else sends error
         * */

        // val intentObj = configuraton?.get(intentKey) as HashMap<*, *>
        // val intentID = intentObj?.get(intentKey)
        val intentID = _intentId

        // val operator = configuraton?.get(operatorKey) as HashMap<*, *>
        // val publickKey = operator.get(publicKeyToGet)
        val publickKey = _publickey

        if (intentID.toString().isNullOrBlank() || publickKey.toString().isNullOrBlank()) {
            PayButtonDataConfiguration.getTapKnetListener()
                ?.onPayButtonError("public key and intent id are required")
        }
        else if (intentID==null && intentID==""  && configuraton.isNullOrEmpty()){
            PayButtonDataConfiguration.getTapKnetListener()
                ?.onPayButtonError("Whether intent id or body is required")
        }
        else if (!configuraton.isNullOrEmpty()) {

            val sdkInfo = hashMapOf<String, Any>(
                "type" to "button",
                "authorization" to publickKeyVal,
                "version" to "2.2.0",
                "mdn" to headersVal.mdn.toString(),
                "application" to headersVal.application.toString()
            )

            configuraton["sdk_info"] = sdkInfo

            callIntentAPI(configuraton, headers)
        }
        else if(intentID!=null && intentID!="" && configuraton.isNullOrEmpty()) {
            val operator = HashMap<String, String>()
            operator["publicKey"] = publickKeyVal
            /**
             * intent
             */
            val intentObj = HashMap<String,Any>()

            if (intentID != null) {
                intentObj.put("intent",intentID)
            }
            /**
             * configuration
             */

            val configurations = LinkedHashMap<String,Any>()

            configurations.put("operator",operator)
            configurations.put("intent",intentObj)
            callIntentRetereiveAPI(configurations, headers)

        }




        when (configuraton) {

            // KnetConfiguration.MapConfigruation -> {

            /* urlToBeloaded =
                    "${webviewStarterUrl}${encodeConfigurationMapToUrl(KnetDataConfiguration.configurationsAsHashMap)}"*/
            // knetWebView.loadUrl(urlToBeloaded)
            // }


        }
        //    Log.e("urlToBeloaded",urlToBeloaded)

    }

    private fun initializePaymentData(buttonType: ThreeDsPayButtonType?) {
        when (buttonType) {
            ThreeDsPayButtonType.KNET -> applySchemes(SCHEMES.KNET)
            ThreeDsPayButtonType.BENEFIT -> applySchemes(SCHEMES.BENEFIT)
            ThreeDsPayButtonType.FAWRY -> applySchemes(SCHEMES.FAWRY)
            ThreeDsPayButtonType.PAYPAL -> applySchemes(SCHEMES.PAYPAL)
            ThreeDsPayButtonType.TABBY -> applySchemes(SCHEMES.TABBY)
            ThreeDsPayButtonType.GOOGLEPAY -> applySchemes(SCHEMES.GOOGLE)
            ThreeDsPayButtonType.CAREEMPAY -> applySchemes(SCHEMES.CAREEMPAY)
            ThreeDsPayButtonType.SAMSUNGPAY -> applySchemes(SCHEMES.SAMSUNGPAY)
            ThreeDsPayButtonType.VISA -> applySchemes(SCHEMES.VISA)
            ThreeDsPayButtonType.AMERICANEXPRESS -> applySchemes(SCHEMES.AMERICANEXPRESS)
            ThreeDsPayButtonType.MADA -> applySchemes(SCHEMES.MADA)
            ThreeDsPayButtonType.MASTERCARD -> applySchemes(SCHEMES.MASTERCARD)
            ThreeDsPayButtonType.CARD -> applySchemes(SCHEMES.CARD)


            else -> {}
        }
    }

    private fun applySchemes(scheme: SCHEMES) {
        webviewStarterUrl = scheme.value.first
        webViewScheme = scheme.value.second
    }

    inner class MyWebViewClient : WebViewClient() {


        /**
         * Every navigation the web sdk attempts lands here.
         *
         * What it means is worked out in PayButtonSdkNavigationPolicy, which is the mirror
         * of the iOS decidePolicyFor. Keeping it out of the web view client is what lets
         * the popup's client route the same way rather than reimplementing it, and what
         * ended the duplicated on3dsRedirect blocks that used to live in this method
         */
        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            webView: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url ?: return false
            return decidePolicyFor(url, webView ?: redirectWebView)
        }




        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)


        }


        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            Log.e("intercepted",request?.url.toString())

            when(request?.url?.toString()?.contains("https://benefit-checkout")?.and((!isBenefitPayUrlIntercepted))) {

                true ->{
                    view?.post{
                        (webViewFrame as ViewGroup).removeView(redirectWebView)


                        dialog= Dialog(context,android.R.style.Theme_Translucent_NoTitleBar)
                        //Create LinearLayout Dynamically
                        linearLayout = LinearLayout(context)
                        //Setup Layout Attributes
                        val params = LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        linearLayout.layoutParams = params
                        linearLayout.orientation = VERTICAL

                        /**
                         * onBackPressed in Dialog
                         */
                        dialog.setOnKeyListener { view, keyCode, keyEvent ->
                            if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){
                                dismissDialog()
                                init(redirectConfiguration,headersVal, intentVal ,publickKeyVal)
                                return@setOnKeyListener  true
                            }
                            return@setOnKeyListener false
                        }


                        if (redirectWebView.parent == null){
                            linearLayout.addView(redirectWebView)
                        }

                        dialog.setContentView(linearLayout)
                        dialog.show()
                    }

                    isBenefitPayUrlIntercepted = true
                }
                else -> {


                    Log.e(
                        "intercepted",
                        request?.url.toString()
                    )

                    /*
                     * A passkey can navigate inside an iframe or a new browsing context, and
                     * shouldOverrideUrlLoading is not called for those. This is the second
                     * place it can be caught, and it must be caught .. a WebView has no
                     * navigator.credentials, so letting the request through only ends in the
                     * acs page failing in a way the payer can not act on
                     */
                    if (isPasskeyNavigation(request?.url.toString())) {
                        val passkeyUrl: String = request?.url.toString()
                        Log.d("PayButton", "a passkey request arrived, $passkeyUrl")

                        view?.post {
                            view.stopLoading()
                            startFidoAuthentication(
                                threeDsUrl = passkeyUrl,
                                redirectUrl = lastCardRedirection?.redirectUrl
                            )
                        }

                        // Answering it ourselves is what stops it loading in the web view
                        return WebResourceResponse("text/plain", "UTF-8", null)
                    }



                }
            }


            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            Log.e("error code",error.errorCode.toString())
            Log.e("error description ",error.description.toString())

            Log.e("request header ",request.requestHeaders.toString())
            super.onReceivedError(view, request, error)

        }
    }

    override fun onDetachedFromWindow() {

        try {
            if (::webChrome.isInitialized) {
                webChrome.closePopupWebView()
            }
        } catch (e: Exception) {
            Log.e(
                "PayButton",
                "Error closing popup WebView",
                e
            )
        }

        redirectWebView.destroy()

        super.onDetachedFromWindow()
    }


    fun toBase64(value: String?): String? {
        var value = value
        if (value == null) value = ""
        return Base64.encodeToString(value.trim { it <= ' ' }.toByteArray(), Base64.DEFAULT)
    }
    fun decodeBase64(base64String: String): String? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8) // Convert bytes to string using UTF-8
        } catch (e: IllegalArgumentException) {
            println("Invalid Base64 input: ${e.message}")
            null
        }
    }
    /** Takes the 3ds dialog down and puts the button page back where it belongs */
    internal fun dismissDialog() {
        if (::dialog.isInitialized) {
            linearLayout.removeView(redirectWebView)
            dialog.dismiss()
            if (redirectWebView.parent == null) {
                (webViewFrame as ViewGroup).addView(redirectWebView)
            }
        }
    }

    private fun closePayment() {
        if (successPayload.second) {
            dismissDialog()
            PayButtonDataConfiguration.getTapKnetListener()
                ?.onPayButtonSuccess(successPayload.first)
        }
    }

    //MARK: - Talking to the web sdk

    /**
     * Runs javascript in the button page.
     *
     * Mirrors `webView.evaluateJavaScript`. Kept in one place so every caller gets the same
     * main thread hop .. a WebView may only be touched from the thread it was made on, and
     * the callbacks that finish an authentication arrive from wherever the browser left them
     * @param javaScript The script to run
     * @param callback What to do with what it returned, if the caller cares
     */
    internal fun evaluateOnWebSdk(javaScript: String, callback: ((String) -> Unit)?) {
        if (!::webViewFrame.isInitialized) return
        redirectWebView.post {
            redirectWebView.evaluateJavascript(javaScript) { result ->
                callback?.invoke(result ?: "null")
            }
        }
    }

    /** Opens the 3ds page for the flow that asked for it */
    internal fun navigateTo3dsActivity(paymentbutton: String) {
        val intent = Intent(context, ThreeDsWebViewActivityButton::class.java)
        ThreeDsWebViewActivityButton.payButton = this@PayButton
        intent.putExtra("flow", paymentbutton)
        context.startActivity(intent)
    }

    //MARK: - Sizing

    /**
     * Grows or shrinks the button to the height the web sdk asks for.
     *
     * The card based buttons render a form that resizes while the customer types. Mirrors
     * `updateHeight(to:)`, minus the animation .. a layout pass on Android is already what
     * the constraint animation is doing on the other side
     * @param height The height in dp the web sdk reported
     */
    internal fun updateHeight(height: Int) {
        if (!::webViewFrame.isInitialized) return
        webViewFrame.post {
            webViewFrame.layoutParams = webViewFrame.layoutParams.apply {
                this.height = webViewFrame.context.getDimensionsInDp(height)
            }
            webViewFrame.requestLayout()
        }
    }

    /** Gives the page the whole frame, for a flow that is a page rather than a button */
    internal fun expandToFullScreen() {
        if (!::webViewFrame.isInitialized) return
        webViewFrame.post {
            webViewFrame.layoutParams = LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            webViewFrame.requestLayout()
        }
    }

    /** Closes a window the page opened with `window.open`, if one is up */
    internal fun closePopupWebView() {
        if (::webChrome.isInitialized) webChrome.closePopupWebView()
    }

    //MARK: - Reset

    /**
     * Takes down everything the running payment put on screen and forgets what it left
     * behind, without touching the button page itself.
     *
     * Mirrors `teardown()`. A payment that ended, however it ended, leaves things that must
     * not be inherited by the next one .. a 3ds page still up, a popup window, a passkey
     * running in the browser, and the redirection details a later challenge would read the
     * return url out of
     */
    internal fun teardown() {
        post {
            dismissDialog()
            closePopupWebView()

            // Closes the browser without telling the delegate, the payment it belonged to is over
            threeDSPasskeySession?.cancel()
            threeDSPasskeySession = null

            lastCardRedirection = null
            threeDsResponse = null
            threeDsResponseCardPayButtons = null
        }
    }

    /**
     * Puts the button back to how it started .. nothing of the last payment on screen,
     * nothing of it remembered, and the page loaded again from scratch. Mirrors `reset()`
     */
    internal fun reset() {
        teardown()
        if (::urlToBeloaded.isInitialized && urlToBeloaded.isNotEmpty()) {
            Log.i("PayButton", "resetting, loading the button page again")
            redirectWebView.post { redirectWebView.loadUrl(urlToBeloaded) }
        }
    }

    override fun onEnterForeground() {
        iSAppInForeground = true
        Log.e("applifeCycle", "onEnterForeground")

        // The browser a passkey runs in reports nothing at all, so coming back to the
        // foreground is the only sign the payer left it. The session decides what that means
        ThreeDSPasskeySession.hostResumed()
    }

    override fun onEnterBackground() {
        iSAppInForeground = false
        Log.e("applifeCycle", "onEnterBackground")
    }
}
enum class KnetConfiguration() {
    MapConfigruation
}

enum class PaymentFlow {
    CARDPAY, PAYMENTBUTTON
}





