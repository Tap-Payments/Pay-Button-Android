package company.tap.tappaybutton.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/*
 * The redirection models as they were before the sdk was lined up with Pay-Button-iOS.
 *
 * Kept so an integrator compiling against them still compiles. Nothing inside the sdk reads
 * them any more .. they declared every field non null, which only told Gson to lie about a
 * payload that was missing one, and the null surfaced later as a crash somewhere else.
 * TapRedirection.kt is what replaced them.
 */

@Deprecated("Use Redirection, whose fields are nullable the way the payload actually is",
    ReplaceWith("Redirection"))
@Parcelize
data class ThreeDsResponse(
    var id: String,
    var url: String,
    var powered: Boolean,
    var stopRedirection: Boolean = false
) : Parcelable

@Deprecated("Use CardRedirection, whose fields are nullable the way the payload actually is",
    ReplaceWith("CardRedirection"))
@Parcelize
data class ThreeDsResponseCardPayButtons(
    var threeDsUrl: String,
    var redirectUrl: String,
    var powered: Boolean,
    var keyword: String
) : Parcelable
