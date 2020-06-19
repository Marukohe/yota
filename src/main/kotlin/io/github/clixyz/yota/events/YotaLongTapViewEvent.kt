package io.github.clixyz.yota.events

import android.support.test.uiautomator.BySelector
import io.github.clixyz.yota.view.YotaView

open class YotaLongTapViewEvent(selector: BySelector) : YotaViewEvent(selector) {

    override fun injectInner(view: YotaView): Int = if (view.longTap()) {
        YotaEvent.INJECT_SUCCEEDED
    } else {
        YotaEvent.INJECT_FAILED
    }
}