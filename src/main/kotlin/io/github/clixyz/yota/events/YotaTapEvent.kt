package io.github.clixyz.yota.events

import android.view.View
import io.github.clixyz.yota.droid.Droid

open class YotaTapEvent : YotaEvent {
    val view: View?
    val x: Float
    val y: Float

    constructor(view: View?) {
        this.view = view
        this.x = 0f
        this.y = 0f
    }

    constructor(x: Float, y: Float) {
        this.view = null
        this.x = x
        this.y = y
    }


    companion object {
        fun mustFailEvent() = YotaTapEvent(-1f, -1f)
    }

    override fun inject(): Int = if (x < 0 || y < 0) {
        YotaEvent.INJECT_FAILED
    } else if (Droid.exec{ it.im.tap(x, y) }) {
        YotaEvent.INJECT_SUCCEEDED
    } else {
        YotaEvent.INJECT_FAILED
    }
}