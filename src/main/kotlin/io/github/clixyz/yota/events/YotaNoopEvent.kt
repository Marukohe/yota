package io.github.clixyz.yota.events

open class YotaNoopEvent : YotaEvent {

    override fun inject(): Int = YotaEvent.INJECT_SUCCEEDED
}