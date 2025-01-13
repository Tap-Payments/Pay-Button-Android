package company.tap.tappaybutton.threeDsWebview

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import company.tap.tappaybutton.doAfterSpecificTime
import company.tap.tappaybutton.getDeviceSpecs

import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tappaybutton.PaymentFlow
import company.tap.tappaybutton.PayButtonDataConfiguration
import company.tap.tappaybutton.PayButton
import java.util.*

const val delayTime = 5000L

class ThreeDsWebViewActivityButton : AppCompatActivity() {
    lateinit var threeDsBottomsheet: BottomSheetDialogFragment
    lateinit var paymentFlow: String
    var loadedBottomSheet = false

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var payButton: PayButton
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_three_ds_web_view)
        LocalizationManager.setLocale(this, Locale(PayButtonDataConfiguration.lanuage.toString()))
        val webView = WebView(this)
        val linearLayout : LinearLayout = findViewById(R.id.linear)
        webView.layoutParams = this.getDeviceSpecs().first.let {
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                it
            )
        }
        //linearLayout.addView(we)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true

        }

        // webView.isVerticalScrollBarEnabled = true
        webView.requestFocus()
        webView.webViewClient = threeDsWebViewClient()
        val data = intent.extras
        paymentFlow = data?.getString("flow") ?: PaymentFlow.PAYMENTBUTTON.name
        when (paymentFlow) {
            PaymentFlow.PAYMENTBUTTON.name -> {
                println("threeDsResponse>>"+PayButton.threeDsResponse.url)
                webView.loadUrl(PayButton.threeDsResponse.url)
            }

            PaymentFlow.CARDPAY.name -> {
                webView.loadUrl(PayButton.threeDsResponseCardPayButtons.threeDsUrl)

            }
        }

        threeDsBottomsheet = ThreeDsBottomSheetFragmentButton(webView, onCancel = {
            when (paymentFlow) {
                PaymentFlow.PAYMENTBUTTON.name -> {
                    PayButton.cancel()
                }

                PaymentFlow.CARDPAY.name -> {
                    PayButton.cancel()
                    /*  tapKnetPay.init(
                          KnetConfiguration.MapConfigruation,
                          TapKnetPay.buttonTypeConfigured
                      )*/ //SToped cardpay for now

                }
            }
            PayButtonDataConfiguration.getTapKnetListener()?.onPayButtoncancel()
        })

    }

    inner class threeDsWebViewClient : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun shouldOverrideUrlLoading(
            webView: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            Log.e("3dsUrl View", request?.url.toString())
            when (paymentFlow) {
                PaymentFlow.PAYMENTBUTTON.name -> {
                    webView?.loadUrl(request?.url.toString())
                    //   val Redirect = RedirectDataConfiguration.configurationsAsHashMap?.get(redirectKey) as HashMap<*, *>
                    //  val redirect = Redirect.get(urlKey)
                    when (request?.url?.toString()?.contains("tap_id", ignoreCase = true)) {
                        true -> {
                            threeDsBottomsheet.dialog?.dismiss()
                            val splittiedString = request.url.toString().split("?", ignoreCase = true)
                            if(splittiedString!=null)Log.e("splittedString", splittiedString.toString())
                            try {
                                PayButton.retrieve(splittiedString[1])
                            } catch (e: Exception) {
                                PayButtonDataConfiguration.getTapKnetListener()
                                    ?.onPayButtonError(e.message.toString())
                            }
                        }

                        false -> {}
                        else -> {}
                    }

                }

                PaymentFlow.CARDPAY.name -> {
                    when (request?.url?.toString()
                        ?.contains(PayButton.threeDsResponseCardPayButtons.keyword)) {
                        true -> {
                            threeDsBottomsheet.dialog?.dismiss()
                            val splittiedString = request.url.toString().split("?")

                            Log.e("splitted", splittiedString.toString())
                            PayButton.generateTapAuthenticate(request.url?.toString() ?: "")
                        }

                        false -> {}
                        else -> {}
                    }
                }
            }

            return true

        }

        override fun onPageFinished(view: WebView, url: String) {
            if (loadedBottomSheet) {
                return
            } else {
                doAfterSpecificTime(time = delayTime) {
                    if (!supportFragmentManager.isDestroyed){
                        threeDsBottomsheet.show(supportFragmentManager, "")
                    }
                }
            }
            loadedBottomSheet = true
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            super.onReceivedError(view, request, error)
        }
    }


}
