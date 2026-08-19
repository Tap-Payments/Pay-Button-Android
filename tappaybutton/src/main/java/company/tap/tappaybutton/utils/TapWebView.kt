package company.tap.tappaybutton.utils

import android.webkit.WebSettings
import android.webkit.WebView

/*
 * TapWebView.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/extensions/TapPayButton+WKWebView.swift
 *
 * The web view settings every page the sdk shows is given.
 */

/**
 * Stops the payer zooming the page.
 *
 * A payment form that pinches out of shape, or that jumps when a field is double tapped, reads
 * as broken rather than as a feature. iOS needs two separate things for this, a scroll view
 * setting and an injected viewport, because the pinch and the double tap take different paths.
 * Android settles both from one place .. with zoom unsupported neither gesture does anything,
 * so there is no viewport to write into a page that is not ours to edit
 */
internal fun WebView.tapDisableZoom() {
    with(settings) {
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
    }
}

/** The settings shared by every page the sdk renders */
internal fun WebSettings.tapApplyPageDefaults() {
    javaScriptEnabled = true
    domStorageEnabled = true
    // The card form opens its identity flow, ex click to pay, with window.open
    javaScriptCanOpenWindowsAutomatically = true
    setSupportMultipleWindows(true)
    allowContentAccess = true
    cacheMode = WebSettings.LOAD_NO_CACHE
}
