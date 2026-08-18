package company.tap.tappaybutton

object PasskeyManager {

    private var authenticationCallback: ((String) -> Unit)? = null

    fun setAuthenticationCallback(callback: (String) -> Unit) {
        authenticationCallback = callback
    }

    fun onAuthenticationCompleted(url: String) {
        authenticationCallback?.invoke(url)
        authenticationCallback = null
    }

    fun clear() {
        authenticationCallback = null
    }
}