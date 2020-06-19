package io.github.clixyz.yota.events

import io.github.clixyz.yota.droid.Droid

open class YotaKeyEvent(val key: Int) : YotaEvent {

    override fun inject(): Int = if (Droid.exec { it.im.key(key) }) {
        YotaEvent.INJECT_SUCCEEDED
    } else {
        YotaEvent.INJECT_FAILED
    }
}