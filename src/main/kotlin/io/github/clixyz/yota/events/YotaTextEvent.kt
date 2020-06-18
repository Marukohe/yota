package io.github.clixyz.yota.events

import io.github.clixyz.yota.droid.Droid

open class YotaTextEvent(val text: String) : YotaEvent {

    override fun inject(): Int {
        return if (Droid.exec { it.im.text(text) }) {
            YotaEvent.INJECT_SUCCEEDED
        } else {
            YotaEvent.INJECT_FAILED
        }
    }
}