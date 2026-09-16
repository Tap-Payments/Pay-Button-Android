package company.tap.tappaybutton.utils

import android.net.Uri
import android.util.Base64
import android.util.Log

/*
 * WebUrlUtils.kt
 *
 * Android mirror of SharedDataModels-iOS/Utils/WebUrlUtils.swift, the helper every
 * Pay-Button-iOS call site reads its url payloads through.
 *
 * The contract is the iOS one: never throw. A missing key, an opaque url or a payload
 * that is not base64 all give back an empty string, and the caller decides what an empty
 * payload means. The old Android helper base64 decoded unconditionally and threw straight
 * out of shouldOverrideUrlLoading, which crashed the host app on a malformed callback.
 */

private const val TAG = "WebUrlUtils"

/**
 * The query of a url as a map, mirroring `tap_getQueryItems`.
 *
 * Uri.getQueryParameter refuses to work on an opaque url, ex `tapbuttonsdk:onSuccess?data=x`
 * with no `//` after the scheme, so the raw query is parsed by hand in that case.
 */
internal fun tapGetQueryItems(url: Uri?): Map<String, String> {
    if (url == null) return emptyMap()

    val query: String = (if (url.isOpaque) url.encodedSchemeSpecificPart?.substringAfter('?', "")
    else url.encodedQuery).orEmpty()

    if (query.isEmpty()) return emptyMap()

    val items: MutableMap<String, String> = LinkedHashMap()
    for (pair in query.split('&')) {
        if (pair.isEmpty()) continue
        val separator: Int = pair.indexOf('=')
        val name: String = if (separator < 0) pair else pair.substring(0, separator)
        val value: String = if (separator < 0) "" else pair.substring(separator + 1)
        if (name.isEmpty()) continue
        items[Uri.decode(name)] = Uri.decode(value)
    }
    return items
}

/**
 * The value of a query parameter, base64 decoded when the caller asks for it.
 *
 * Mirrors `tap_extractDataFromUrl(_:for:shouldBase64Decode:)`.
 * @param url The url the web sdk tried to navigate to
 * @param key The query parameter to read, `data` for every payload the web sdk sends
 * @param shouldBase64Decode Whether the value travels base64 encoded. The web sdk sends
 * json payloads encoded and plain values, ex the reported height, as they are
 * @return The value, or an empty string when there is nothing to read
 */
internal fun tapExtractDataFromUrl(
    url: Uri?,
    key: String = "data",
    shouldBase64Decode: Boolean = true
): String {
    val value: String = tapGetQueryItems(url)[key] ?: return ""
    if (!shouldBase64Decode) return value
    return tapBase64Decoded(value) ?: run {
        Log.w(TAG, "the $key parameter is not base64, handing back nothing")
        ""
    }
}

/** Same as above for a url that has not been parsed yet */
internal fun tapExtractDataFromUrl(
    url: String?,
    key: String = "data",
    shouldBase64Decode: Boolean = true
): String {
    if (url.isNullOrEmpty()) return ""
    return tapExtractDataFromUrl(runCatching { Uri.parse(url) }.getOrNull(), key, shouldBase64Decode)
}

/**
 * Decodes a base64 string, or null when it is not one.
 *
 * Url safe base64 travels in query strings, so it is put back to standard base64 and
 * padded before decoding, the same way the iOS session does it.
 */
internal fun tapBase64Decoded(value: String?): String? {
    if (value.isNullOrEmpty()) return null

    var padded: String = value.replace('-', '+').replace('_', '/')
    val remainder: Int = padded.length % 4
    if (remainder != 0) padded += "=".repeat(4 - remainder)

    return try {
        String(Base64.decode(padded, Base64.DEFAULT), Charsets.UTF_8)
    } catch (error: IllegalArgumentException) {
        null
    }
}

/** True when the string names an http or https url, which is what the card form can finish on */
internal fun tapIsHttpUrl(value: String?): Boolean {
    if (value.isNullOrEmpty()) return false
    return value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)
}
