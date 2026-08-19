package company.tap.tappaybutton.threeDsWebview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import company.tap.tappaybutton.PayButton
import company.tap.tappaybutton.TapBrandView
import company.tap.tappaybutton.ThreeDSPasskeySession
import company.tap.tappaybutton.doAfterSpecificTime
import company.tap.tappaybutton.getDeviceSpecs
import org.json.JSONObject

class ThreeDsBottomSheetFragmentButton(
    var webView: WebView?,
    var onCancel: () -> Unit
) : BottomSheetDialogFragment() {

    /*
     * Prevent injecting the same Passkey authentication URL
     * more than once into the WebView.
     */
    private var passkeyAuthenticationInjected = false

    @Nullable
    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(
            R.layout.bottom_sheet_dialog_button,
            container,
            false
        )

        val linearLayout =
            view.findViewById<LinearLayout>(R.id.webLinear)

        /*
         * IMPORTANT:
         *
         * The WebView may already have a parent when the
         * BottomSheet is recreated/resumed.
         *
         * Remove it from its CURRENT parent before adding it
         * to this LinearLayout.
         */
        webView?.let { currentWebView ->

            val currentParent =
                currentWebView.parent as? ViewGroup

            if (currentParent != null) {

                Log.d(
                    "3DS_BOTTOM_SHEET",
                    "Removing WebView from existing parent: " +
                            currentParent.javaClass.simpleName
                )

                currentParent.removeView(currentWebView)
            }

            /*
             * Now it is safe to attach the WebView.
             */
            linearLayout.addView(
                currentWebView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        val tapBrandView =
            view.findViewById<TapBrandView>(
                R.id.tab_brand_view
            )

        /*
         * ---------------------------------------------------------
         * POWERED BY
         * ---------------------------------------------------------
         *
         * Keep existing behavior.
         */
        try {

            val powered =
                PayButton.threeDsResponse.powered

            when (powered) {

                false -> {
                    tapBrandView
                        .poweredByImage
                        .visibility = View.INVISIBLE
                }

                else -> {
                    // Keep existing behavior
                }
            }

        } catch (e: Exception) {

            Log.e(
                "3DS_BOTTOM_SHEET",
                "Error reading powered state",
                e
            )
        }

        /*
         * ---------------------------------------------------------
         * PASSKEY RETURN
         * ---------------------------------------------------------
         *
         * When Passkey returns to the application, the returned
         * authentication URL is stored inside:
         *
         * ThreeDSPasskeySession
         *
         * Example:
         *
         * https://sdk.dev.tap.company/?auth_payer=XXXX
         *
         * IMPORTANT:
         *
         * Do NOT use last3DsUrl here.
         *
         * Do NOT load the Passkey URL directly into the WebView.
         *
         * The WebView should already contain the ORIGINAL 3DS page.
         *
         * We only call:
         *
         * window.loadAuthernticate(authenticationUrl)
         */
        injectPasskeyAuthenticationIfAvailable()

        /*
         * ---------------------------------------------------------
         * BOTTOM SHEET CONFIGURATION
         * ---------------------------------------------------------
         */
        val bottomSheetDialog =
            dialog as? BottomSheetDialog

        bottomSheetDialog?.behavior?.isFitToContents = true

        bottomSheetDialog?.behavior?.peekHeight =
            (context?.getDeviceSpecs()?.first ?: 950) - 250

        /*
         * Existing dismiss behavior.
         */
        dialog?.setOnDismissListener {

            if (isAdded) {

                doAfterSpecificTime {

                    if (!isFinishingOrDestroyed()) {
                        requireActivity().finish()
                    }
                }
            }
        }

        isCancelable = false

        /*
         * ---------------------------------------------------------
         * BACK BUTTON
         * ---------------------------------------------------------
         */
        tapBrandView
            .backButtonLinearLayout
            .setOnClickListener {

                dialog?.dismiss()

                webView?.post {

                    try {

                        webView?.evaluateJavascript(
                            "window.cancel();",
                            null
                        )

                    } catch (e: Exception) {

                        Log.e(
                            "3DS_BOTTOM_SHEET",
                            "Unable to call window.cancel()",
                            e
                        )

                        /*
                         * Fallback to the existing behavior.
                         */
                        webView?.loadUrl(
                            "javascript:window.cancel()"
                        )
                    }
                }

                onCancel.invoke()
            }
    }

    /**
     * Reads the authentication URL returned by Passkey and
     * injects it into the existing 3DS WebView.
     *
     * Flow:
     *
     * Passkey
     *    ↓
     * ThreeDSPasskeySession
     *    ↓
     * getLastAuthenticatedUrl()
     *    ↓
     * ORIGINAL 3DS WebView
     *    ↓
     * window.loadAuthernticate(url)
     *
     * The Passkey URL itself is NEVER loaded directly.
     */
    private fun injectPasskeyAuthenticationIfAvailable() {

        if (passkeyAuthenticationInjected) {

            Log.d(
                "3DS_BOTTOM_SHEET",
                "Passkey authentication already injected"
            )

            return
        }

        val authenticationUrl =
            ThreeDSPasskeySession.getLastAuthenticatedUrl()

        if (authenticationUrl.isNullOrBlank()) {

            Log.d(
                "3DS_BOTTOM_SHEET",
                "No Passkey authentication URL available"
            )

            return
        }

        val currentWebView =
            webView

        if (currentWebView == null) {

            Log.e(
                "3DS_BOTTOM_SHEET",
                "WebView is null. Cannot inject Passkey authentication"
            )

            return
        }

        Log.d(
            "3DS_BOTTOM_SHEET",
            "Passkey authentication URL received = $authenticationUrl"
        )

        /*
         * IMPORTANT:
         *
         * Do not load:
         *
         * webView.loadUrl(authenticationUrl)
         *
         * Instead execute the JavaScript function provided by
         * the original 3DS page.
         */
        val javascript =
            "window.loadAuthernticate(" +
                    JSONObject.quote(authenticationUrl) +
                    ");"

        /*
         * Make sure this executes on the WebView's UI thread.
         */
        currentWebView.post {

            if (isFinishingOrDestroyed()) {

                Log.d(
                    "3DS_BOTTOM_SHEET",
                    "Activity is finishing/destroyed. Skip Passkey injection"
                )

                return@post
            }

            try {

                passkeyAuthenticationInjected = true

                Log.d(
                    "3DS_BOTTOM_SHEET",
                    "Calling window.loadAuthernticate()"
                )

                currentWebView.evaluateJavascript(
                    javascript
                ) { result ->

                    Log.d(
                        "3DS_BOTTOM_SHEET",
                        "loadAuthernticate result = $result"
                    )

                    /*
                     * The Passkey URL has now been consumed.
                     *
                     * Clear it so that reopening the BottomSheet
                     * does not authenticate using the old Passkey
                     * session again.
                     */
                    ThreeDSPasskeySession
                        .clearLastAuthenticatedUrl()

                    Log.d(
                        "3DS_BOTTOM_SHEET",
                        "Passkey authentication URL cleared"
                    )
                }

            } catch (e: Exception) {

                /*
                 * Allow another attempt if the JavaScript call
                 * failed.
                 */
                passkeyAuthenticationInjected = false

                Log.e(
                    "3DS_BOTTOM_SHEET",
                    "Failed to inject Passkey authentication URL",
                    e
                )
            }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        dialog?.window?.attributes?.windowAnimations =
            R.style.DialogAnimations

        setStyle(
            STYLE_NORMAL,
            R.style.CustomBottomSheetDialogFragment
        )
    }

    override fun onDestroyView() {

        /*
         * IMPORTANT:
         *
         * Don't destroy the WebView here.
         *
         * We want to keep the SAME WebView instance so that
         * its current 3DS page/history can be reused when
         * the BottomSheet is shown again.
         *
         * Just detach it from the BottomSheet's container.
         */
        webView?.let { currentWebView ->

            val parent =
                currentWebView.parent as? ViewGroup

            if (parent != null) {

                Log.d(
                    "3DS_BOTTOM_SHEET",
                    "Detaching WebView from BottomSheet"
                )

                parent.removeView(currentWebView)
            }
        }

        super.onDestroyView()
    }

    override fun getTheme(): Int =
        R.style.CustomBottomSheetDialogFragment

    private fun isFinishingOrDestroyed(): Boolean {

        val activity = activity
            ?: return true

        return activity.isFinishing ||
                activity.isDestroyed
    }
}