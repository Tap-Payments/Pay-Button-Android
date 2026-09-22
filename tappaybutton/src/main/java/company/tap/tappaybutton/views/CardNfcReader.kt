package company.tap.tappaybutton.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import com.example.tappaybutton.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import company.tap.nfcreader.open.reader.TapEmvCard
import company.tap.nfcreader.open.reader.TapNfcCardReader
import company.tap.nfcreader.open.utils.TapNfcUtils
import company.tap.taplocalizationkit.LocalizationManager
import company.tap.tappaybutton.PayButtonDataConfiguration
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import company.tap.tappaybutton.TapBrandView
import company.tap.tappaybutton.ThemeManager
import company.tap.tappaybutton.utils.tapHostActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.Locale
import java.util.concurrent.TimeUnit

/*
 * CardNfcReader.kt
 *
 * Reading a contactless card without leaving the screen the payer is on.
 *
 * The prompt is a bottom sheet over the merchant's own activity. There is no activity of ours in
 * the way, so no second window, no task, and no transition .. tapping the nfc icon just raises a
 * sheet, the way it should have from the start.
 *
 * What made that awkward before is how the tag arrives. Foreground dispatch delivers it as an
 * Intent to `onNewIntent`, which means owning an Activity, and the sdk does not own the one the
 * button is in. Reader mode hands the tag straight to a callback instead, so nothing has to be
 * routed through an activity at all. The reader kit only wants `EXTRA_TAG` out of the intent it
 * is given, so the tag from the callback is wrapped in one and handed over as if it had been
 * dispatched.
 */
internal object CardNfcReader {

    private const val TAG = "CardNfc"

    /** How long the platform waits between checking the card is still there, in milliseconds */
    private const val PRESENCE_CHECK_DELAY = 5000

    /** How long a single exchange with the card may take, in milliseconds */
    private const val TRANSCEIVE_TIMEOUT = 3000

    /**
     * How many times the kit is asked again before the payer is told it did not work.
     *
     * TapNFCCardReaderKit 0.0.5 has a race in `readCardBlocking`: it starts an AsyncTask to do
     * the reading, does not wait for it, and then dereferences the field that task is going to
     * fill .. so the very first call throws a NullPointerException from inside the library
     * essentially every time, in a few milliseconds, before the card has been talked to at all.
     *
     * Asking again after a pause is what gets the card out. By the second call the first call's
     * background read has finished and filled the field, so the answer is sitting there waiting.
     * That is also why the sdk looked like it worked one time in three or four .. that was the
     * race being won by chance rather than anything about the card or how it was held.
     */
    private const val READ_ATTEMPTS = 6

    /**
     * How long to wait before asking the kit again, in milliseconds.
     *
     * Long enough for the background read the previous call kicked off to finish. Retrying
     * immediately just loses the same race again
     */
    private const val READ_RETRY_DELAY_MS = 350L

    /** How long one attempt may run before it is given up on, in seconds */
    private const val READ_TIMEOUT_SECONDS = 6L

    /** Everything a running read is holding, so ending one lets go of all of it */
    private class Session(
        val activity: Activity,
        val adapter: NfcAdapter,
        val reader: TapNfcCardReader,
        val listener: NfcListener
    ) {
        var sheet: BottomSheetDialog? = null
        var progress: View? = null
        var animation: ImageView? = null
        var title: TextView? = null
        var description: TextView? = null
        var read: Disposable = Disposable.empty()
        var finished: Boolean = false
        /**
         * Set while a read is actually running.
         *
         * A card held against a phone is not held still, and reader mode reports the tag again
         * on every wobble. Each report used to start another read over the same card while the
         * one before it was still exchanging .. they fight over the same IsoDep channel and none
         * of them finishes, which is what makes a card that is sitting right there feel like it
         * needs waving around
         */
        @Volatile
        var reading: Boolean = false
    }

    @Volatile
    private var session: Session? = null

    /** Receives what was read */
    internal interface NfcListener {
        fun cardRead(cardNumber: String, cardHolderName: String, expiryDate: String)
        fun nfcCanceled()
        fun nfcFailed(error: String)
    }

    /** True when this device can read a card at all */
    internal fun isAvailable(context: Context): Boolean = TapNfcUtils.isNfcAvailable(context)

    /**
     * Raises the prompt and starts listening for a card
     * @param context The context the button lives in
     * @param listener Told what was read, or why nothing was
     */
    internal fun start(context: Context, listener: NfcListener) {
        if (session != null) {
            Log.i(TAG, "a read is already running, ignoring this one")
            return
        }

        val activity: Activity? = context.tapHostActivity()
        if (activity == null) {
            Log.e(TAG, "no activity behind the button to read from")
            listener.nfcFailed(
                "{\"error\":\"Could not show the NFC reader: the button has no activity behind it\"}"
            )
            return
        }

        val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null || !TapNfcUtils.isNfcAvailable(activity)) {
            Log.e(TAG, "this device has no nfc")
            listener.nfcFailed("{\"error\":\"NFC is not supported on this device\"}")
            return
        }

        if (!TapNfcUtils.isNfcEnabled(activity)) {
            Log.e(TAG, "nfc is switched off")
            listener.nfcFailed("{\"error\":\"NFC is turned off on this device\"}")
            return
        }

        // TapNFCView reads its strings and colours out of these the moment it is inflated
        LocalizationManager.setLocale(activity, Locale(PayButtonDataConfiguration.lanuage.toString()))

        val running = Session(activity, adapter, TapNfcCardReader(activity), listener)
        session = running

        if (!showSheet(running)) {
            // Nothing on screen means nothing telling the payer to tap, and no way to back out
            session = null
            val reason: String = lastSheetFailure ?: "unknown"
            lastSheetFailure = null
            listener.nfcFailed("{\"error\":\"Could not show the NFC reader: $reason\"}")
            return
        }

        // Reader mode also silences the platform's own tag handling while the sheet is up, so a
        // card tapped here does not get picked up by whatever else handles nfc on the device
        // Reading an emv card is a long conversation, dozens of exchanges rather than one. The
        // platform checks the tag is still there between them, and at its default rate that check
        // keeps interrupting the conversation .. so the card has to be held perfectly still or the
        // read restarts. Slowing the check down is what lets a normal, slightly wobbly tap finish
        val readerOptions = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY)
        }

        adapter.enableReaderMode(
            activity,
            { tag -> onTagFound(running, tag) },
            // The platform's own detection beep is left on. It is the only thing telling the
            // payer the card was seen at all, and without it a read that is quietly under way
            // feels like nothing happening, which is what makes people start waving the card
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            readerOptions
        )
        Log.i(TAG, "reader mode on for ${activity.javaClass.simpleName}, waiting for a card")
    }

    /** Takes the prompt down without telling anyone, ex the payment it belonged to is over */
    internal fun dismiss() {
        val running: Session = session ?: return
        running.finished = true
        end(running)
    }

    //MARK: - Private

    /** The prompt, over whatever the payer was already looking at */
    private var lastSheetFailure: String? = null

    private fun showSheet(running: Session): Boolean = try {
        // The style matters .. a Material sheet built on the host activity's own theme finds
        // none of the attributes it needs. This is the style the 3ds sheet already runs on
        val dialog = BottomSheetDialog(running.activity, R.style.CustomBottomSheetDialogFragment)
        dialog.setContentView(R.layout.nfc_sheet_button)

        dialog.findViewById<TapBrandView>(R.id.tab_brand_view_nfc)
            ?.backButtonLinearLayout
            ?.setOnClickListener { dialog.dismiss() }

        running.progress = dialog.findViewById(R.id.nfc_progress)
        running.animation = dialog.findViewById(R.id.nfc_animation)
        running.title = dialog.findViewById(R.id.nfc_title)
        running.description = dialog.findViewById(R.id.nfc_description)

        loadTheAnimation(running, dialog)

        dialog.setOnDismissListener { onSheetDismissed(running) }
        dialog.show()
        running.sheet = dialog
        true
    } catch (error: Exception) {
        Log.e(TAG, "the nfc prompt could not be shown", error)
        lastSheetFailure = error.message ?: error.javaClass.simpleName
        false
    }

    /**
     * The card-against-the-phone animation, from the same place Card-Android takes it.
     *
     * Loaded rather than inflated, and separately from the rest, so a device that is offline gets
     * a prompt with the words and no picture instead of no prompt at all
     */
    private fun loadTheAnimation(running: Session, dialog: BottomSheetDialog) {
        val target: ImageView = dialog.findViewById(R.id.nfc_animation) ?: return
        val dark: Boolean = ThemeManager.currentTheme.contains("dark", ignoreCase = true)
        val url: String = if (dark) "https://tap-assets.b-cdn.net/card-sdk/nfc/nfcgif_dark.gif"
        else "https://tap-assets.b-cdn.net/card-sdk/nfc/nfcgif_light.gif"

        runCatching { Glide.with(running.activity).load(url).into(target) }
            .onFailure { Log.i(TAG, "the nfc animation could not be loaded, showing the text only") }
    }

    /** A card was held against the phone */
    private fun onTagFound(running: Session, tag: Tag?) {
        // Logged before anything is decided, so a tap that goes nowhere can be told apart from
        // a tap that never arrived
        Log.i(TAG, "reader mode saw a tag: ${tag?.techList?.joinToString() ?: "null"}")

        if (running.finished) {
            Log.i(TAG, "the read had already ended, ignoring this tag")
            return
        }
        if (tag == null) return

        // The kit reads a dispatched intent, so the tag is handed over in one. It takes nothing
        // else out of it, which is what makes reader mode usable with it at all
        val asIntent = Intent().putExtra(NfcAdapter.EXTRA_TAG, tag)

        val isoDep: IsoDep? = runCatching { IsoDep.get(tag) }.getOrNull()
        Log.i(TAG, "IsoDep on this tag: ${if (isoDep == null) "no" else "yes"}")

        if (!running.reader.isSuitableIntent(asIntent)) {
            Log.i(TAG, "the kit will not read this tag, it is not a card we can read")
            return
        }

        if (running.reading) {
            Log.i(TAG, "already reading this card, letting it finish")
            return
        }

        // The default transceive window is a few hundred milliseconds on most devices, and a
        // card that answers more slowly than that fails half way through every time. Set on the
        // tag before the kit connects, since the platform holds it per technology
        runCatching { isoDep?.timeout = TRANSCEIVE_TIMEOUT }
            .onFailure { Log.i(TAG, "could not lengthen the transceive timeout") }

        Log.i(TAG, "a card was tapped, reading it")
        running.reading = true
        showReading(running)

        running.read = running.reader.readCardRx2(asIntent)
            // The kit hands back whatever its background read has managed so far, which on the
            // first call is nothing. An empty card is not an answer, so it is treated as one
            // more "not yet" and goes round again
            .map { card ->
                if (card.cardNumber.isNullOrBlank()) {
                    throw IllegalStateException("the kit has not finished reading yet")
                }
                card
            }
            // Waiting between attempts is the whole trick, see READ_RETRY_DELAY_MS
            .retryWhen { errors ->
                errors.zipWith(Flowable.range(1, READ_ATTEMPTS)) { _, attempt -> attempt }
                    .flatMap { attempt ->
                        Log.i(TAG, "the kit was not ready, asking again ($attempt of $READ_ATTEMPTS)")
                        Flowable.timer(READ_RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                    }
            }
            // A card lifted mid exchange leaves the read waiting on an answer that is never
            // coming. Without a limit it waits forever, and the prompt is not cancellable while
            // it does .. so the payer would be stuck looking at a spinner with no way out
            .timeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { card ->
                    running.reading = false
                    Log.i(TAG, "the kit returned a card, number ${if (card?.cardNumber.isNullOrEmpty()) "missing" else "present"}")
                    if (card != null) deliver(running, card)
                },
                { error ->
                    // Left listening on purpose. A read that failed half way is what a card
                    // being moved looks like, and the next steady contact should just work
                    running.reading = false
                    Log.e(TAG, "the card could not be read, still listening", error)
                    showRetry(running)
                }
            )
    }

    /** The card is being read. Says so, because the exchange takes a couple of seconds */
    private fun showReading(running: Session) = onMain {
        running.animation?.visibility = View.INVISIBLE
        running.progress?.visibility = View.VISIBLE
        running.title?.setText(R.string.nfc_reading_title)
        running.description?.setText(R.string.nfc_reading_description)
        // Nothing good comes of the sheet sliding away mid exchange
        running.sheet?.setCancelable(false)
    }

    /** The read did not finish. Back to waiting, with a hint about holding it still */
    private fun showRetry(running: Session) = onMain {
        running.progress?.visibility = View.GONE
        running.animation?.visibility = View.VISIBLE
        running.title?.setText(R.string.nfc_title)
        running.description?.setText(R.string.nfc_retry_description)
        running.sheet?.setCancelable(true)
    }

    private fun deliver(running: Session, card: TapEmvCard) {
        if (running.finished) return
        running.finished = true

        // The reader reports the expiry as a date. The form wants two digits and two digits,
        // which is what formatting it straight to MM/yy gives, without picking the string apart
        val expiry: String = card.expireDate
            ?.let { DateFormat.format("MM/yy", it).toString() }
            .orEmpty()

        Log.i(TAG, "a card was read over nfc")
        val listener: NfcListener = running.listener
        end(running)

        listener.cardRead(
            cardNumber = card.cardNumber.orEmpty(),
            cardHolderName = card.holderFirstname.orEmpty(),
            expiryDate = expiry
        )
    }

    /** The payer swiped the sheet away or pressed back on it */
    private fun onSheetDismissed(running: Session) {
        if (running.finished) return
        running.finished = true

        Log.i(TAG, "the payer closed the nfc prompt")
        val listener: NfcListener = running.listener
        end(running)
        listener.nfcCanceled()
    }

    /** Stops listening and takes the prompt down, whichever way the read ended */
    private fun end(running: Session) {
        onMain {
            running.read.dispose()

            runCatching { running.adapter.disableReaderMode(running.activity) }
                .onFailure { Log.e(TAG, "could not turn reader mode off", it) }

            running.sheet?.let { if (it.isShowing) it.dismiss() }
            running.sheet = null

            if (session === running) session = null
        }
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else Handler(Looper.getMainLooper()).post(block)
    }
}
