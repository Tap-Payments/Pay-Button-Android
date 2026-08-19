package company.tap.tappaybutton.views

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import company.tap.tappaybutton.utils.tapStartSdkActivity
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tappaybutton.R
import company.tap.cardscanner.CameraFragment
import company.tap.cardscanner.TapCard
import company.tap.cardscanner.TapScannerCallback
import company.tap.cardscanner.TapTextRecognitionCallBack
import company.tap.cardscanner.TapTextRecognitionML

/*
 * CardScannerActivity.kt
 *
 * Android mirror of Pay-Button-iOS
 * Logic/Shared/Private/views/CardScannerViewController.swift
 *
 * The camera the card form asks for. Neither side reads the card itself .. iOS hands the frames
 * to TapCardScannerWebWrapper, this hands them to TapCardScannerKit, and both are only the screen
 * around it plus a way back out.
 *
 * The camera permission is asked for here rather than before presenting, which is where the two
 * sides differ. iOS asks first because a view controller has to be presented from something; on
 * Android the screen that needs the camera is the one that should ask for it, and it is already
 * an Activity, so there is nothing to hand the request to.
 */
internal class CardScannerActivity : AppCompatActivity(),
    TapTextRecognitionCallBack, TapScannerCallback {

    /** Set once a card has been delivered, so a second read cannot deliver twice */
    private var hasDelivered: Boolean = false
    private var recognition: TapTextRecognitionML? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_scanner)

        current = this

        findViewById<ImageButton>(R.id.scanner_close).setOnClickListener {
            Log.i(TAG, "the payer closed the scanner")
            deliverCancel()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != CAMERA_REQUEST) return

        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startScanning()
            return
        }

        Log.e(TAG, "the payer did not allow the camera, there is nothing to scan with")
        listener?.scannerFailed("{\"error\":\"The user didn't approve accessing the camera.\"}")
        deliverCancel(tellDelegate = false)
    }

    /** Puts the camera on screen and starts reading frames */
    private fun startScanning() {
        // The recognizer keeps its listener statically, so it is registered before the camera
        // fragment goes up and consults it
        recognition = TapTextRecognitionML(this).apply { addTapScannerCallback(this@CardScannerActivity) }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.scanner_container, CameraFragment())
            .commit()
    }

    //MARK: - What the recognizer reports

    /** A full card was read. This is the one that ends the scan */
    override fun onReadSuccess(card: TapCard?) {
        val number: String? = card?.cardNumber
        if (number.isNullOrBlank()) return
        deliver(card)
    }

    override fun onReadFailure(error: String?) {
        Log.e(TAG, "the scanner could not read the card, ${error.orEmpty()}")
    }

    /**
     * A partial read. The recognizer reports these while it is still working, so they are logged
     * and nothing else .. delivering one would fill the form with half a card
     */
    override fun onRecognitionSuccess(card: TapCard?) {
        Log.i(TAG, "recognized so far: ${card?.cardNumber.orEmpty()}")
    }

    override fun onRecognitionFailure(error: String?) {
        Log.i(TAG, "nothing recognized yet, ${error.orEmpty()}")
    }

    //MARK: - Getting out

    private fun deliver(card: TapCard) {
        if (hasDelivered) return
        hasDelivered = true

        Log.i(TAG, "a card was read")
        val chosen: ScanListener? = listener
        clearCurrent()
        finish()
        chosen?.cardScanned(
            cardNumber = card.cardNumber.orEmpty(),
            cardHolderName = card.cardHolder.orEmpty(),
            expiryDate = card.expirationDate.orEmpty()
        )
    }

    private fun deliverCancel(tellDelegate: Boolean = true) {
        if (hasDelivered) return
        hasDelivered = true

        val chosen: ScanListener? = listener
        clearCurrent()
        finish()
        if (tellDelegate) chosen?.scannerCanceled()
    }

    override fun onBackPressed() {
        deliverCancel()
        super.onBackPressed()
    }

    override fun onDestroy() {
        clearCurrent()
        recognition = null
        super.onDestroy()
    }

    private fun clearCurrent() {
        synchronized(CardScannerActivity::class.java) {
            if (current === this) current = null
        }
    }

    /** Receives what the scanner read */
    internal interface ScanListener {
        fun cardScanned(cardNumber: String, cardHolderName: String, expiryDate: String)
        fun scannerCanceled()
        fun scannerFailed(error: String)
    }

    internal companion object {

        private const val TAG = "CardScanner"
        private const val CAMERA_REQUEST = 4711

        /**
         * Told what was read. The button is a view and cannot register for an activity result,
         * so the answer comes back the same way the passkey callback does.
         * Not named `delegate` .. AppCompatActivity already has one of those
         */
        @Volatile
        internal var listener: ScanListener? = null

        /** The scanner currently on screen, so a payment ending can take it down */
        @Volatile
        internal var current: CardScannerActivity? = null

        /** Opens the scanner */
        internal fun start(context: Context, scanListener: ScanListener) {
            listener = scanListener
            context.tapStartSdkActivity(Intent(context, CardScannerActivity::class.java))
        }

        /** Takes the scanner down without telling anyone, ex the payment it belonged to is over */
        internal fun dismiss() {
            current?.let {
                it.hasDelivered = true
                it.finish()
            }
            current = null
            listener = null
        }
    }
}
