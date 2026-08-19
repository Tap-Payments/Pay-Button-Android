package company.tap.tappaybutton

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import company.tap.tappaybutton.views.ThreeDSPasskeySession

/**
 * Watches the app going to the background and coming back.
 *
 * Two things need it. The button tracks it so a success arriving while the app was away is
 * finished when it returns, and a passkey needs it because the browser it runs in reports
 * nothing at all .. the app coming back is the only sign the payer left it.
 *
 * The session is told directly rather than through the registered listener. It has to be
 * told whether or not an integrator wired one up, and for a long time nobody did: nothing
 * ever called `addAppLifeCycle`, so `getAppLifeCycle()` was null and every one of these
 * events was delivered to nobody.
 */
class AppLifecycleObserver : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        ThreeDSPasskeySession.hostResumed()
        PayButtonDataConfiguration.getAppLifeCycle()?.onEnterForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        PayButtonDataConfiguration.getAppLifeCycle()?.onEnterBackground()
    }
}
