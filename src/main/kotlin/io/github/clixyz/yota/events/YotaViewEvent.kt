package io.github.clixyz.yota.events

import android.support.test.uiautomator.BySelector
import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.utils.Logger
import io.github.clixyz.yota.view.YotaView

abstract class YotaViewEvent(val selector: BySelector) : YotaEvent {
    override fun inject(): Int = try {
        val view = Droid.exec { it.ua.findView(selector) }
        if (view == null) {
            Logger.e("No such view found")
            YotaEvent.INJECT_FAILED_NO_SUCH_VIEW
        } else {
            this.injectInner(view)
        }
    } catch (x: UiAutoDelegate.NullRootException) {
        Logger.e("Root is null at present")
        YotaEvent.INJECT_FAILED_NULL_ROOT
    }

    abstract fun injectInner(view: YotaView): Int
}