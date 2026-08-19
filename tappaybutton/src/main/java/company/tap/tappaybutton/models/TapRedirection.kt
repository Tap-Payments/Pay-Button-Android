package company.tap.tappaybutton.models

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.parcelize.Parcelize

/*
 * TapRedirection.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/Models/TapRedirection.swift
 *
 * Two shapes arrive from the web sdk. `Redirection` is what the shared buttons send with
 * onChargeCreated, `CardRedirection` is what the card form sends with on3dsRedirect.
 *
 * Every field is nullable, exactly as the Swift models are. The old Android models
 * declared them non null, which only told Gson to lie .. a payload missing `keyword` still
 * decoded, and the null surfaced later as a crash at the point of use rather than as a
 * failed decode here.
 */

/** What the shared buttons send when a redirection based payment has to be authenticated */
@Parcelize
data class Redirection(
    /** The 3DS/Otp page link to display */
    var url: String? = null,
    /** The id of the charge created */
    var id: String? = null,
    /** Whether the powered by tap bar is shown */
    var powered: Boolean? = null,
    /** Whether the redirection should be skipped */
    var stopRedirection: Boolean? = null
) : Parcelable {

    companion object {
        /**
         * Decodes the json the web sdk sent, or null when it is not this shape.
         *
         * Mirrors `try? Redirection(data)` .. a failed decode is an answer, not a crash
         */
        @JvmStatic
        fun fromJson(json: String?): Redirection? = decodeJson(json, Redirection::class.java)
    }
}

/**
 * What the card form sends with `on3dsRedirect`, when the payer has to be authenticated.
 * Same shape Card-iOS decodes, the card form behind the button is the same web sdk
 */
@Parcelize
data class CardRedirection(
    /** The 3DS/Otp page link to display */
    var threeDsUrl: String? = null,
    /** The url to listen for, to detect the end of the authentication process */
    var redirectUrl: String? = null,
    /** The query parameter watched for on the loaded pages to know the process is done */
    var keyword: String? = null,
    /** Whether the powered by tap bar is shown */
    var powered: Boolean? = null
) : Parcelable {

    companion object {
        /** Decodes the json the card form sent, or null when it is not this shape */
        @JvmStatic
        fun fromJson(json: String?): CardRedirection? = decodeJson(json, CardRedirection::class.java)
    }
}

/**
 * Gson, with a failed decode reported as null rather than thrown.
 *
 * Gson also hands back null for the string "null" and for a json literal that is not an
 * object, ex a bare number, so both are folded into the same answer
 */
private fun <T> decodeJson(json: String?, type: Class<T>): T? {
    if (json.isNullOrBlank()) return null
    return try {
        Gson().fromJson(json, type)
    } catch (error: JsonSyntaxException) {
        null
    } catch (error: IllegalStateException) {
        null
    }
}
