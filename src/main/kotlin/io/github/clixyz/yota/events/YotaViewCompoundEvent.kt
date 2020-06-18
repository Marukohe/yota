package io.github.clixyz.yota.events

import io.github.clixyz.yota.ui.YotaView

open class YotaViewCompoundEvent(
        val view: YotaView,
        event: YotaEvent
) : YotaCompoundEvent(event) {

    override fun inject(): Int = internalEvent.inject()
}