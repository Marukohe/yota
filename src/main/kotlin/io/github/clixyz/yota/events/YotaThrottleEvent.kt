package io.github.clixyz.yota.events

import android.os.SystemClock

open class YotaThrottleEvent(val ms: Long) : YotaEvent {

    override fun inject(): Int {
        SystemClock.sleep(ms)
        return YotaEvent.INJECT_SUCCEEDED
    }
}