package io.github.clixyz.yota.events

import io.github.clixyz.yota.droid.Droid

open class YotaSwipeEvent(
    val fromX: Int,
    val fromY: Int,
    val toX: Int,
    val toY: Int,
    val steps: Int
) : YotaEvent {

    companion object {
        fun mustFailEvent() = YotaSwipeEvent(-1, -1, -1, -1, -1)
    }

    override fun inject(): Int = if (fromX < 0 || fromY < 0 || toX < 0 || toY < 0 || steps < 0) {
        YotaEvent.INJECT_FAILED
    } else if (Droid.exec { it.im.swipe(fromX, fromY, toX, toY, steps) }) {
        YotaEvent.INJECT_SUCCEEDED
    } else {
        YotaEvent.INJECT_FAILED
    }
}