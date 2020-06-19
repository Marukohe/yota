package io.github.clixyz.yota.events

import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.view.YotaView

open class YotaTapViewEvent(view: YotaView) : YotaViewEvent(view) {

    override fun inject(): Int = try {
        if (view.tap()) {
            YotaEvent.INJECT_SUCCEEDED
        } else {
            YotaEvent.INJECT_FAILED
        }
    } catch (x: UiAutoDelegate.NullRootException) {
        YotaEvent.INJECT_FAILED_NULL_ROOT
    }
}