package company.tap.tappaybutton


import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
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
import company.tap.tappaybutton.models.ThreeDsResponse
import company.tap.tappaybutton.models.ThreeDsResponseCardPayButtons
import company.tap.tappaybutton.popup_window.WebChrome
import company.tap.tappaybutton.threeDsWebview.ThreeDsWebViewActivityButton
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

@SuppressLint("ViewConstructor")
class PayButton : LinearLayout , ApplicationLifecycle {
    lateinit var webviewStarterUrl: String
    private var isBenefitPayUrlIntercepted =false
    // lateinit var webViewScheme: String
    var webViewScheme: String = "tapbuttonsdk://"
    private lateinit var webChrome: WebChrome

    lateinit var webViewFrame: FrameLayout
    lateinit var urlToBeloaded: String
    var firstTimeOnReadyCallback = true
    lateinit var linearLayout: LinearLayout
    lateinit var dialog: Dialog
    lateinit var redirectConfiguration: java.util.HashMap<String, Any>
    lateinit var headersVal: Headers
    lateinit var publickKeyVal: String
    lateinit var intentVal: String
    var iSAppInForeground = true
    var onSuccessCalled = false
    var pair =  Pair("",false)
    private  val SAMSUNG_PAY_URL_PREFIX: String = "samsungpay"
    private  val SAMSUNG_APP_STORE_URL: String = "samsungapps://ProductDetail/com.samsung.android.spay"
    private var paymentResultReceived = false
    companion object {
        lateinit var threeDsResponse: ThreeDsResponse
        lateinit var threeDsResponseCardPayButtons: ThreeDsResponseCardPayButtons

        private lateinit var redirectWebView: WebView

        lateinit var buttonTypeConfigured: ThreeDsPayButtonType
        fun cancel() {
            redirectWebView.loadUrl("javascript:window.cancel()")
        }

        fun generateTapAuthenticate(authIdPayerUrl: String) {
            redirectWebView.loadUrl("javascript:window.loadAuthentication('$authIdPayerUrl')")
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
                javaScriptCanOpenWindowsAutomatically = true
                allowContentAccess = true
                setSupportMultipleWindows(true)
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true

            }
        }
        redirectWebView.setBackgroundColor(Color.TRANSPARENT)
        redirectWebView.setLayerType(LAYER_TYPE_SOFTWARE, null)
        webChrome = WebChrome(context)
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
        else if (!configuraton.isNullOrEmpty() ){
            callIntentAPI(configuraton,headers)

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


        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            webView: WebView?,
            request: WebResourceRequest?
        ): Boolean {

            /**
             * main checker if url start with "tapCardWebSDK://"
             */
            Log.e("url Here>>>>", request?.url.toString())

            if (request?.url.toString().startsWith(SAMSUNG_PAY_URL_PREFIX, true) ||
                request?.url.toString().startsWith(SAMSUNG_APP_STORE_URL, true)) {

                // Stop the WebView from continuing to load this URL
                webView?.post {
                    webView.stopLoading()
                    webView?.visibility = View.GONE

                }

                try {
                    val intent = Intent.parseUri(request?.url.toString(), Intent.URI_INTENT_SCHEME)
                   // samsungCheckoutStarted= true
                    paymentResultReceived = false
                    onSuccessCalled = false
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val installIntent = Intent.parseUri(
                        "samsungapps://ProductDetail/com.samsung.android.spay",
                        Intent.URI_INTENT_SCHEME
                    )
                    installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(installIntent)
                }

                return true // ensures WebView does not handle the URL further
            }
          /*  if (request?.url.toString().contains(TapRedirectStatusDelegate.onHeightChange.name)) {
                val newHeight = request?.url?.getQueryParameter(keyValueName)
                val params: ViewGroup.LayoutParams? = webViewFrame.layoutParams
                params?.height = webViewFrame.context.getDimensionsInDp(newHeight?.toInt()?.plus(15) ?: 95)
                webViewFrame.layoutParams = params

                PayButtonDataConfiguration.getTapKnetListener()
                    ?.onPayButtonHeightChange(newHeight.toString())


            }*/
            if (request?.url.toString().contains(TapRedirectStatusDelegate.onHeightChange.name)) {

                val height = request?.url?.getQueryParameter(keyValueName)?.toIntOrNull()

                if (height != null) {
                    webViewFrame.post {
                        webViewFrame.layoutParams =
                            webViewFrame.layoutParams.apply {
                                this.height =
                                    webViewFrame.context.getDimensionsInDp(height)
                            }

                        webViewFrame.requestLayout()
                    }
                    PayButtonDataConfiguration.getTapKnetListener()
                        ?.onPayButtonHeightChange(height.toString())

                }


                return true
            }
            if (request?.url.toString().contains(TapRedirectStatusDelegate.onBinIdentification.name)) {
                PayButtonDataConfiguration.getTapKnetListener()
                    ?.onPayButtonBindIdentification(
                        request?.url?.getQueryParameterFromUri(keyValueName).toString()
                    )
                var datafromUrl = request?.url?.getQueryParameter(keyValueName).toString()
                PayButtonDataConfiguration.getTapKnetListener()
                    ?.onPayButtonBindIdentification(datafromUrl)

                return true
            }
            if (request?.url.toString().startsWith(careemPayUrlHandler)) {
                webViewFrame.layoutParams =
                    LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                threeDsResponse = ThreeDsResponse(
                    id = "",
                    url = request?.url.toString(),
                    powered = true,
                    stopRedirection = false
                )
                navigateTo3dsActivity(PaymentFlow.PAYMENTBUTTON.name)
                return true
            } else {
                if (request?.url.toString().startsWith(webViewScheme, ignoreCase = true)) {
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onReady.name)) {


                        /* if (buttonTypeConfigured == ThreeDsPayButtonType.CARD) {
                             if (firstTimeOnReadyCallback) {
                                 Thread.sleep(1500)
                                 firstTimeOnReadyCallback = false
                             }
                             *//**
                         *
                         *  todo enhance in a better way
                         *//*


                        }*/


                        PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonReady()

                    }
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onSuccess.name)) {
                        onSuccessCalled = true
                        var datafromUrl = request?.url?.getQueryParameter(keyValueName).toString()
                        println("datafromUrl>>"+datafromUrl)
                        var decoded = decodeBase64(datafromUrl)
                        println("decoded>>"+decoded)
                        if (decoded != null) {
                            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonSuccess(
                                decoded
                            )

                        }
                        pair = Pair(request?.url?.getQueryParameterFromUri(keyValueName).toString(),true)

                        when(iSAppInForeground) {

                            true ->{//closePayment()
                                dismissDialog()
                                Log.e("success","one")
                            }
                            false ->{}
                        }
                    }

                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onChargeCreated.name)) {

                        val data = decodeBase64(request?.url?.getQueryParameter(keyValueName).toString())
                        Log.e("chargedData", data.toString())
                        val jsonObject = JSONObject(data);
                        var jsonObject1 = JSONObject()
                        if(jsonObject.has("gateway_response")){
                            jsonObject1 = jsonObject.getJSONObject("gateway_response")
                            // println("jsonObject1"+jsonObject1.get("name"))
                        }
                        val gson = Gson()
                        /**Check added for benefitpay ***/
                        if(jsonObject1!=null && jsonObject1.has("name") &&jsonObject1.get("name").toString().equals("BENEFITPAY")){

                        }else {
                            threeDsResponse = gson.fromJson(data, ThreeDsResponse::class.java)
                            when (threeDsResponse.stopRedirection) {
                                false -> navigateTo3dsActivity(PaymentFlow.PAYMENTBUTTON.name)
                                else -> {}
                            }
                        }
                        PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonChargeCreated(
                            request?.url?.getQueryParameterFromUri(keyValueName).toString()
                        )
                    }
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onOrderCreated.name)) {
                        val orderResponse = request?.url?.getQueryParameter(keyValueName).toString()
                        println("orderResponse>>"+orderResponse)
                        //TODO check if decode required
                        PayButtonDataConfiguration.getTapKnetListener()
                            ?.onPayButtonOrderCreated(
                                orderResponse
                            )



                    }
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onClick.name)) {
                        isBenefitPayUrlIntercepted=false
                        onSuccessCalled = false
                        pair = Pair("",false)
                        PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonClick()

                    }
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.cancel.name)) {

                        PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()



                    }
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onCancel.name)) {
                        android.os.Handler(Looper.getMainLooper()).postDelayed(3000) {
                            if(!onSuccessCalled){
                                PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()
                            }


                        }

                        if (!(pair.first.isNotEmpty() and pair.second)) {
                            dismissDialog()
                        }

                    }

                    if (request?.url.toString().contains(TapRedirectStatusDelegate.on3dsRedirect.name)) {
                        /**
                         * navigate to 3ds Activity
                         */
                        val queryParams =
                            request?.url?.getQueryParameterFromUri(keyValueName).toString()
                        Log.e("data card", queryParams.toString())

                        threeDsResponseCardPayButtons = queryParams.getModelFromJson()
                        navigateTo3dsActivity(PaymentFlow.CARDPAY.name)
                        Log.e("data card", threeDsResponseCardPayButtons.toString())


                    }
                    /**
                     * for google button specifically
                     */
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onClosePopup.name)) {
                        webChrome.getdialog()?.dismiss()

                    }

                    /* if (request?.url.toString().contains(KnetStatusDelegate.onError.name)) {

                         RedirectDataConfiguration.getTapKnetListener()
                             ?.onPayButtonError(
                                 request?.url?.getQueryParameterFromUri(keyValueName).toString()
                             )
                     }*/
                    if (request?.url.toString().contains(TapRedirectStatusDelegate.onError.name)) {
                        decodeBase64(request?.url?.getQueryParameter(keyValueName).toString())?.let {
                            PayButtonDataConfiguration.getTapKnetListener()
                                ?.onPayButtonError(
                                    it
                                )
                        }
                        pair = Pair(request?.url?.getQueryParameterFromUri(keyValueName).toString(),true)

                    }
                    if (request?.url.toString().startsWith("intent://")) {
                        try {
                            val context: Context = context
                            val intent: Intent = Intent.parseUri(request?.url.toString(), Intent.URI_INTENT_SCHEME)
                            if (intent != null) {
//                            view.stopLoading()
                                val packageManager: PackageManager = context.packageManager
                                val info: ResolveInfo? = packageManager.resolveActivity(
                                    intent,
                                    PackageManager.MATCH_DEFAULT_ONLY
                                )
                                if (info != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent)
                                } else {
                                    return false
                                }
                                return true
                            }
                        } catch (e: URISyntaxException) {
                            Log.e("error", "Can't resolve intent://", e)

                        }
                        //   progressBar.visibility = GONE
                    }
                    if (request?.url.toString().startsWith("intent://")) {
                        try {
                            val context: Context = context
                            val intent: Intent = Intent.parseUri(request?.url.toString(), Intent.URI_INTENT_SCHEME)
                            if (intent != null) {
//                            view.stopLoading()
                                val packageManager: PackageManager = context.packageManager
                                val info: ResolveInfo? = packageManager.resolveActivity(
                                    intent,
                                    PackageManager.MATCH_DEFAULT_ONLY
                                )
                                if (info != null) {
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent)
                                } else {
                                    return false
                                }
                                return true
                            }
                        } catch (e: URISyntaxException) {
                            Log.e("error", "Can't resolve intent://", e)

                        }
                        //   progressBar.visibility = GONE
                    }

                    return true
                }

                else {

                    return false
                }
            }
        }




        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)


        }

        fun navigateTo3dsActivity(paymentbutton: String) {
            val intent = Intent(context, ThreeDsWebViewActivityButton()::class.java)
            ThreeDsWebViewActivityButton.payButton = this@PayButton
            intent.putExtra("flow", paymentbutton)
            (context).startActivity(intent)
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
                else -> {}
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
    private fun dismissDialog() {
        if (::dialog.isInitialized) {
            linearLayout.removeView(redirectWebView)
            dialog.dismiss()
            if (redirectWebView.parent == null){
                (webViewFrame as ViewGroup).addView(redirectWebView)
            }
        }
    }

    private fun closePayment() {

        if (pair.second) {
            Log.e("app","one")
            dismissDialog()

            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtonSuccess(pair.first)

        }
    }
    override fun onEnterForeground() {
        iSAppInForeground = true
        Log.e("applifeCycle","onEnterForeground")
        //  closePayment()





    }
    override fun onEnterBackground() {
        iSAppInForeground = false
        Log.e("applifeCycle","onEnterBackground")

    }
}
enum class KnetConfiguration() {
    MapConfigruation
}

enum class PaymentFlow {
    CARDPAY, PAYMENTBUTTON
}




