package company.tap.tappaybutton.threeDsWebview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import company.tap.tappaybutton.TapBrandView
import company.tap.tappaybutton.getDimensionsInDp

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

        // The page fills the sheet and scrolls itself, so the sheet is sized rather than fitted
        // to a content height it no longer has one of

        // The payer must finish or back out deliberately, a half dismissed sheet leaves the
        // authentication running with nothing on screen
        isCancelable = false

        tapBrandView.backButtonLinearLayout.setOnClickListener {
            dialog?.dismiss()
            onCancel.invoke()
        }
    }

    /**
     * Sizes the sheet once its window exists.
     *
     * It has to be here, not in onViewCreated .. the view being sized is not ours.
     * BottomSheetDialog wraps whatever it is given in a container of its own, and that container
     * is wrap_content, so a root of match_parent inside it resolves against nothing and the sheet
     * ends up as tall as the page happened to have painted.
     *
     * All four settings work as a set. Turning fitToContents off without the rest leaves the
     * behaviour with a peek height and a half expanded state at half the screen, and it opens at
     * whichever it likes .. which is one gateway looking half height and another looking like an
     * empty strip, from the same cause.
     */
    override fun onStart() {
        super.onStart()

        val container: View = dialog?.findViewById(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        container.layoutParams = container.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = false
            // Stops short of the top, so it reads as a sheet over the app rather than a screen
            // that replaced it. Same idea as a page sheet on iOS leaving the presenting screen
            // showing above it. In dp, so the gap is the same size on any density
            expandedOffset = requireContext().getDimensionsInDp(TOP_GAP_DP)
            skipCollapsed = true
            // The payer finishes or backs out with the button, the same as iOS being modal in
            // presentation. Dragging could otherwise settle it half way down mid authentication
            isDraggable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        // Measured after a layout pass. Whichever of these comes back short is where the height
        // stops flowing down
        container.post {
            Log.i(
                TAG,
                "screen ${resources.displayMetrics.heightPixels} | " +
                        "container ${container.height} | " +
                        "root ${view?.height ?: -1} | " +
                        "webLinear ${view?.findViewById<View>(R.id.webLinear)?.height ?: -1} | " +
                        "webView ${webView?.height ?: -1}"
            )
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

        /** How much of the app stays visible above the page, in dp */
        private const val TOP_GAP_DP = 56
    }
}
