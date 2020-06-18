package io.github.clixyz.yota.cmds.mnky

import android.view.KeyEvent
import io.github.clixyz.yota.droid.delegates.AmsDelegate
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.events.YotaEvent
import io.github.clixyz.yota.events.YotaKeyEvent
import io.github.clixyz.yota.events.YotaTapEvent
import io.github.clixyz.yota.events.YotaViewCompoundEvent
import io.github.clixyz.yota.ui.YotaView
import io.github.clixyz.yota.ui.accessors.YotaViewFilter
import io.github.clixyz.yota.ui.accessors.YotaViewOrder
import io.github.clixyz.yota.ui.accessors.accept
import java.util.*

class MnkyEventSourceDfs(
    private val ua: UiAutoDelegate,
    private val am: AmsDelegate
) : MnkyEventSource {

    private val stackMap = mutableMapOf<String, Stack<YotaView>>() // activity -> stack
    private val visitedViews = mutableSetOf<Long>()

    override fun getNextEvent(): YotaEvent? {
        val activityName: String = am.topActivityName ?: return null
        val rootView = ua.rootView ?: return null

        val clickable = YotaViewFilter { v -> v.visible && v.clickable }
        rootView.accept(YotaViewOrder.DFS, clickable)

        if (!stackMap.containsKey(activityName)) {
            stackMap[activityName] = Stack()
        }

        val stack = stackMap[activityName]!!

        for (v in clickable.reversed()) {
            if (!visitedViews.contains(v.srcNodeId) && !stack.contains(v)) {
                stack.push(v)
            }
        }

        if (stack.size == 0) {
            return YotaKeyEvent(KeyEvent.KEYCODE_BACK)
        }

        val v = stack.pop()
        if (!v.visible) {
            return null
        }
        val bounds = v.bounds ?: return null
        visitedViews.add(v.srcNodeId)
        return YotaViewCompoundEvent(v, YotaTapEvent(bounds.centerX(), bounds.centerY()))
    }
}