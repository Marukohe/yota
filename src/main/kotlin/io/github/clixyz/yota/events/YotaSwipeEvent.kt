package io.github.clixyz.yota.events

import io.github.clixyz.yota.droid.Droid

open class YotaSwipeEvent(
    val fromX: Float,
    val fromY: Float,
    val toX: Float,
    val toY: Float,
    val duration: Long
) : YotaEvent {

    companion object {
        fun mustFailEvent() = YotaSwipeEvent(-1f, -1f, -1f, -1f, -1)
    }

    override fun inject(): Int = if (fromX < 0 || fromY < 0 || toX < 0 || toY < 0 || duration < 0) {
        YotaEvent.INJECT_FAILED
    } else if (Droid.exec { it.im.swipe(fromX, fromY, toX, toY, duration) }) {
        YotaEvent.INJECT_SUCCEEDED
    } else {
        YotaEvent.INJECT_FAILED
    }
}