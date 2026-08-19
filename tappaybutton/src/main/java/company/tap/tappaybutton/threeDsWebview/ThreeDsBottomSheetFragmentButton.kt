package company.tap.tappaybutton.threeDsWebview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import company.tap.tappaybutton.TapBrandView
import company.tap.tappaybutton.getDeviceSpecs

/*
 * ThreeDsBottomSheetFragmentButton.kt
 *
 * Android counterpart of what Pay-Button-iOS gets from presenting ThreeDSView as a sheet,
 * plus PoweredByTapView, which is the bar with the way back out.
 *
 * It shows the web view the activity has been loading out of sight, and reports the back
 * button. It decides nothing about the authentication itself .. the passkey injection that
 * used to live here is gone, since a passkey now runs in the browser and its answer goes
 * straight to the card form through ThreeDSPasskeySession, the way it does on iOS.
 */
class ThreeDsBottomSheetFragmentButton(
    private val webView: WebView?,
    private val powered: Boolean,
    private val onCancel: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimations
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_dialog_button, container, false)
        val linearLayout = view.findViewById<LinearLayout>(R.id.webLinear)

        // The web view has been loading elsewhere, and may still be attached where it was
        webView?.let { currentWebView ->
            (currentWebView.parent as? ViewGroup)?.let { parent ->
                Log.d(TAG, "removing the web view from ${parent.javaClass.simpleName}")
                parent.removeView(currentWebView)
            }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tapBrandView = view.findViewById<TapBrandView>(R.id.tab_brand_view)

        // Hide or show the powered by tap based on the coming parameter
        if (!powered) {
            tapBrandView.poweredByImage.visibility = View.INVISIBLE
        }

        val bottomSheetDialog = dialog as? BottomSheetDialog
        bottomSheetDialog?.behavior?.isFitToContents = true
        bottomSheetDialog?.behavior?.peekHeight =
            (context?.getDeviceSpecs()?.first ?: 950) - 250

        // The payer must finish or back out deliberately, a half dismissed sheet leaves the
        // authentication running with nothing on screen
        isCancelable = false

        tapBrandView.backButtonLinearLayout.setOnClickListener {
            dialog?.dismiss()
            onCancel.invoke()
        }
    }

    override fun onDestroyView() {
        // The web view belongs to the activity, this only lets go of it
        webView?.let { currentWebView ->
            (currentWebView.parent as? ViewGroup)?.removeView(currentWebView)
        }
        super.onDestroyView()
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogFragment

    private companion object {
        private const val TAG = "3DS_BOTTOM_SHEET"
    }
}
