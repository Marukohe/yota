package io.github.clixyz.yota.events

import io.github.clixyz.yota.utils.Logger

open class YotaSleepEvent(val ms: Long) : YotaEvent {

    override fun inject(): Int {
        return try {
            Thread.sleep(ms)
            YotaEvent.INJECT_SUCCEEDED
        } catch (e: InterruptedException) {
            Logger.e("Thread was interrupted while sleeping")
            YotaEvent.INJECT_FAILED
        }
    }
}