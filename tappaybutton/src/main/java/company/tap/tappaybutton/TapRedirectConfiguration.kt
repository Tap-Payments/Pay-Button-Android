package company.tap.tappaybutton


import android.content.Context
import android.util.Log
import com.example.tappaybutton.R

import company.tap.tapnetworkkit.connection.NetworkApp
import company.tap.tapnetworkkit.utils.CryptoUtil
import company.tap.tappaybutton.ApiService.BASE_URL_1
import company.tap.tappaybutton.RedirectDataConfiguration.configurationsAsHashMap
import company.tap.tappaybutton.enums.HeadersApplication
import company.tap.tappaybutton.enums.HeadersMdn
import company.tap.tappaybutton.enums.headersKey
import company.tap.tappaybutton.enums.operatorKey
import company.tap.tappaybutton.enums.publicKeyToGet
import company.tap.tappaybutton.enums.urlWebStarter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class TapRedirectConfiguration {

    companion object {
        private val retrofit = ApiService.RetrofitClient.getClient1()
        private val tapSDKConfigsUrl = retrofit.create(ApiService.TapButtonSDKConfigUrls::class.java)
        private var testEncKey: String? = null
        private var prodEncKey: String? = null
        private var  encodedeky: String? = null
        private var   headers: Headers? = null

        fun configureWithRedirectDictionary(
            context: Context,
            tapRedirectViewWeb: TapRedirectPay?,
            tapMapConfiguration: java.util.HashMap<String, Any>,
            redirectPayStatusDelegate: RedirectPayStatusDelegate? = null

        ) {
//ToDO test when cdn url ready
            MainScope().launch {
                getTapButtonSDKConfigUrls(
                    tapMapConfiguration,
                    tapRedirectViewWeb,
                    context,
                    redirectPayStatusDelegate

                )
            }

        }

        private suspend fun getTapButtonSDKConfigUrls(tapMapConfiguration: HashMap<String, Any>, tapCardInputViewWeb: TapRedirectPay?, context: Context, redirectPayStatusDelegate: RedirectPayStatusDelegate?) {
            try {
                /**
                 * request to get Tap configs
                 */

                val tapButtonSDKConfigUrlResponse = tapSDKConfigsUrl.getButtonSDKConfigUrl()
                println("tapButtonSDKConfigUrlResponse>>>>"+tapButtonSDKConfigUrlResponse)

                BASE_URL_1 = tapButtonSDKConfigUrlResponse.baseURL
                prodEncKey = tapButtonSDKConfigUrlResponse.prodEncKey
                //testEncKey = tapButtonSDKConfigUrlResponse.testEncKey
                testEncKey = context.resources.getString(R.string.enryptkeyTest)
                //  urlWebStarter = tapButtonSDKConfigUrlResponse.baseURL


                startWithSDKConfigs(
                    context,
                    tapCardInputViewWeb,
                    tapMapConfiguration,
                    redirectPayStatusDelegate

                )

            } catch (e: Exception) {
                BASE_URL_1 = urlWebStarter //Todo what should be here
                testEncKey =  tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyTest)
                prodEncKey = tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyProduction)

                startWithSDKConfigs(
                    context,
                    tapCardInputViewWeb,
                    tapMapConfiguration,
                    redirectPayStatusDelegate

                )
                Log.e("error Config", e.message.toString())
                e.printStackTrace()
            }
        }

        fun addOperatorHeaderField(
            tapCardInputViewWeb: TapRedirectPay?,
            context: Context,
            modelConfiguration: KnetConfiguration,
            publicKey: String?
        ) {
            encodedeky = when(publicKey.toString().contains("test")){
                true->{
                    // context.resources?.getString(R.string.enryptkeyTest)
                    testEncKey
                }
                false->{
                    //  tapCardInputViewWeb?.context?.resources?.getString(R.string.enryptkeyProduction)
                    prodEncKey

                }
            }

            Log.e("packagedname",context.packageName.toString())
            Log.e("encodedeky",encodedeky.toString())
            NetworkApp.initNetwork(
                tapCardInputViewWeb?.context ,
                publicKey ?: "",
                // context.packageName,
                "demo.tap.PayButtonSDK",
                ApiService.BASE_URL,
                "android-knet",
                true,
                /*   "-----BEGIN PUBLIC KEY-----\n" +
                           "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC8AX++RtxPZFtns4XzXFlDIxPB\n" +
                           "h0umN4qRXZaKDIlb6a3MknaB7psJWmf2l+e4Cfh9b5tey/+rZqpQ065eXTZfGCAu\n" +
                           "BLt+fYLQBhLfjRpk8S6hlIzc1Kdjg65uqzMwcTd0p7I4KLwHk1I0oXzuEu53fU1L\n" +
                           "SZhWp4Mnd6wjVgXAsQIDAQAB\n" +
                           "-----END PUBLIC KEY-----",*/encodedeky,
                null
            )
            headers = Headers(
                application = NetworkApp.getApplicationInfo(),
                mdn = CryptoUtil.encryptJsonString(
                    // context.packageName.toString(),
                    "demo.tap.PayButtonSDK",
                    /*"-----BEGIN PUBLIC KEY-----\n" +
                            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC8AX++RtxPZFtns4XzXFlDIxPB\n" +
                            "h0umN4qRXZaKDIlb6a3MknaB7psJWmf2l+e4Cfh9b5tey/+rZqpQ065eXTZfGCAu\n" +
                            "BLt+fYLQBhLfjRpk8S6hlIzc1Kdjg65uqzMwcTd0p7I4KLwHk1I0oXzuEu53fU1L\n" +
                            "SZhWp4Mnd6wjVgXAsQIDAQAB\n" +
                            "-----END PUBLIC KEY-----",*/encodedeky
                )
            )


            when (modelConfiguration) {
                KnetConfiguration.MapConfigruation -> {
                    val hashMapHeader = HashMap<String, Any>()
                    hashMapHeader[HeadersMdn] = headers?.mdn.toString()
                    hashMapHeader[HeadersApplication] = headers?.application.toString()
                    //val redirect = HashMap<String,Any>()
                    // redirect.put(urlKey, redirectValue)
                    configurationsAsHashMap?.put(headersKey, hashMapHeader)
                    //  configurationsAsHashMap?.put(redirectKey, redirect)


                }
                else -> {}
            }


        }
        private fun startWithSDKConfigs(context: Context,
                                        tapRedirectViewWeb: TapRedirectPay?,
                                        tapMapConfiguration: java.util.HashMap<String, Any>,
                                        redirectPayStatusDelegate: RedirectPayStatusDelegate? = null){
            with(tapMapConfiguration) {
                android.util.Log.e("map", tapMapConfiguration.toString())
               RedirectDataConfiguration.configurationsAsHashMap = tapMapConfiguration
                val operator = RedirectDataConfiguration.configurationsAsHashMap?.get(
                    operatorKey
                ) as HashMap<*, *>
                val publickKey = operator.get(publicKeyToGet)

                val appLifecycleObserver = AppLifecycleObserver()
                androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

                addOperatorHeaderField(
                    tapRedirectViewWeb,
                    context,
                    KnetConfiguration.MapConfigruation,
                    publickKey.toString()
                )

                RedirectDataConfiguration.addTapBenefitPayStatusDelegate(redirectPayStatusDelegate)

                headers?.let { tapRedirectViewWeb?.init(tapMapConfiguration, it) }

            }
        }
    }


}


