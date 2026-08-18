package company.tap.tappaybutton

import android.app.Activity
import android.os.Bundle
import android.util.Log

class PasskeyCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent()

        finish()
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)

        handleIntent()

        finish()
    }

    private fun handleIntent() {

        val uri = intent?.data ?: return

        Log.d("PasskeyCallback", "Callback URI: $uri")

        val authPayer = uri.getQueryParameter("auth_payer")

        if (authPayer.isNullOrEmpty()) {
            Log.e("PasskeyCallback", "auth_payer is missing")
            return
        }

        val authUrl =
            "https://sdk.dev.tap.company/?auth_payer=$authPayer"

        Log.d("PasskeyCallback", "Authentication URL: $authUrl")

        PasskeyManager.onAuthenticationCompleted(authUrl)
    }
}