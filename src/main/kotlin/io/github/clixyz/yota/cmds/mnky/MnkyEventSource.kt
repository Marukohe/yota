package io.github.clixyz.yota.cmds.mnky

import io.github.clixyz.yota.events.YotaEvent

interface MnkyEventSource {

    fun getNextEvent(): YotaEvent?
}