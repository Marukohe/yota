package io.github.clixyz.yota.events

import android.support.test.uiautomator.BySelector
import io.github.clixyz.yota.view.YotaView

open class YotaSwipeViewEvent(
    selector: BySelector,
    val dx: Float,
    val dy: Float,
    val steps: Int
) : YotaViewEvent(selector) {

    override fun injectInner(view: YotaView): Int = if (view.swipe(dx, dy, steps)) {
        YotaEvent.INJECT_SUCCEEDED
    } else {
        YotaEvent.INJECT_FAILED
    }
}