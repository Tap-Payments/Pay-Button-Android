package company.tap.tappaybutton

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent


class AppLifecycleObserver : LifecycleObserver {


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onEnterForeground() {
        RedirectDataConfiguration.getAppLifeCycle()?.onEnterForeground()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onEnterBackground() {
        RedirectDataConfiguration.getAppLifeCycle()?.onEnterBackground()
    }
}