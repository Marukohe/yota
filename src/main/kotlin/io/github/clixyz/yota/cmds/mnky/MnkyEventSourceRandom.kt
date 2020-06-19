package io.github.clixyz.yota.cmds.mnky

import android.graphics.Point
import android.graphics.Rect
import android.view.KeyEvent
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.events.*
import io.github.clixyz.yota.view.YotaView
import io.github.clixyz.yota.view.accessors.YotaViewFilter
import io.github.clixyz.yota.view.accessors.YotaViewOrder
import io.github.clixyz.yota.view.accessors.accept
import io.github.clixyz.yota.utils.Logger
import io.github.clixyz.yota.utils.TextFuzzer

class MnkyEventSourceRandom(
    private val random: MnkyRandom,
    private val ua: UiAutoDelegate,
    private val proba: Proba
) : MnkyEventSource {

    private val textFuzzer = TextFuzzer(random)
    private val clickable = YotaViewFilter { v ->
        v.visible && v.clickable
    }
    private val editable = YotaViewFilter { v ->
        v.visible && v.editable
    }
    private val scrollable = YotaViewFilter { v ->
        v.visible && v.scrollable
    }

    private var lastEvent: YotaEvent? = null

    override fun getNextEvent(): YotaEvent? {
        val e = try {
            doGetNextEvent()
        } catch (t: Throwable) {
            t.message?.let { s: String -> Logger.w(s)}
            null
        }
        lastEvent = e ?: lastEvent
        return e
    }

    private fun doGetNextEvent(): YotaEvent? {
        clickable.clear()
        editable.clear()
        scrollable.clear()

        if (lastEvent != null && lastEvent is YotaTextEvent) {
            // if a text is sent, then an ENTER is sent by proba
            if (random.nextDouble() < proba.enterAfterText) {
                return keyEvent(KeyEvent.KEYCODE_BACK)
            }
        }

        // sometimes, an app will fall into a dialog, or some
        // pages, it is difficult for themselves to jump out
        // of it, so probabilistically actively jump out of it
        // using a back event
        // TODO add detector to detect such circumstances, e.g.,
        //      check activity name,
        //      check if there are views never leaved
        //      ...
        if (random.nextDouble() < proba.lastPage) {
            return keyEvent(KeyEvent.KEYCODE_BACK)
        }

        // root is null, then drop current event
        val rootView = ua.rootView ?: return null

        val CLICKABLE_FILTER_INDEX = 0
        val SCROLLABLE_FILETER_INDEX = 1
        val EDITABLE_FILTER_INDEX = 2
        val filters = listOf(clickable, scrollable, editable)
        rootView.accept(YotaViewOrder.DFS) { view, meta ->
            filters.forEach { filter ->
                filter(view, meta)
            }
        }

        // no nodes found, maybe some task is running,
        // probabilistically jump out of this page, or
        // quickly check it a second time, or
        // sleep to wait it for while
        if (isAllEmpty(filters)) {
            val p = random.nextDouble()
            return when {
                p < 0.1 -> keyEvent(KeyEvent.KEYCODE_BACK)
                p < 0.5 -> noopEvent()
                else -> sleepEvent(100)
            }
        }

        var event: YotaEvent? = null
        while (event == null) {
            val p = random.nextDouble()
            val filterIndex = when {
                p <= 0.5 -> CLICKABLE_FILTER_INDEX
                p <= 0.8 -> SCROLLABLE_FILETER_INDEX
                else -> EDITABLE_FILTER_INDEX
            }
            val filter = filters[filterIndex]
            if (filter.isEmpty()) {
                continue
            }
            event = when (filterIndex) {
                CLICKABLE_FILTER_INDEX -> tapEvent(filter)
                SCROLLABLE_FILETER_INDEX -> swipeEvent(filter)
                EDITABLE_FILTER_INDEX -> textEvent(filter)
                else -> throw ArrayIndexOutOfBoundsException()
            }
        }

        return event
    }

    private fun tapEvent(views: List<YotaView>): YotaViewCompoundEvent {
        if (views.isEmpty()) {
            throw EmptyNodeListException()
        }

        // randomly select one type, and then randomly select one node of that type.
        // because most types of widgets are clickable, and if there exists a list,
        // randomly select a node will cause some types of widgets to become too
        // hungary to die.
        // For example, if a page is composed of a search button (typed ImageButton),
        // and a list of texts (typed clickable TextViews), then selecting theses
        // nodes fully randomly will make the search button very hungary.
        val textViews = mutableListOf<YotaView>()
        val inputViews = mutableListOf<YotaView>()
        val gnrlBtnViews = mutableListOf<YotaView>()
        val imgBtnViews = mutableListOf<YotaView>()
        val otherViews = mutableListOf<YotaView>()

        views.forEach { v ->
            when {
                "EditText" in v.cls || "AutoCompleteTextView" in v.cls ||
                        "SearchView" in v.cls || "CheckBox" in v.cls ||
                        "Radio" in v.cls || "ToggleButton" in v.cls ||
                        "Switch" in v.cls -> inputViews.add(v)
                "TextView" in v.cls -> textViews.add(v)
                "ImageButton" in v.cls || "ImageView" in v.cls -> imgBtnViews.add(v)
                "Button" in v.cls -> gnrlBtnViews.add(v)
                else -> otherViews.add(v)
            }
        }

        // select a type, make the ratio to be,
        //   text  : input : general_button : image_button : other
        // = 0.175 :  0.3  :      0.175     :     0.175    : 0.175
        var typeViews: List<YotaView>

        do {
            val p = random.nextDouble()
            typeViews = when {
                p <= 0.3 -> textViews
                p <= 0.475 -> inputViews
                p <= 0.65 -> imgBtnViews
                p <= 0.825 -> gnrlBtnViews
                else -> otherViews
            }
        } while (typeViews.isEmpty())

        return tapEvent(typeViews[random.nextInt(typeViews.size)])
    }

    private fun tapEvent(v: YotaView): YotaViewCompoundEvent {
        val rect = v.bounds ?: return YotaViewCompoundEvent(v, YotaTapEvent.mustFailEvent())

        // sometimes, outer layout element is clickable
        // as proxy of its inner ones, so init the click
        // point to center with a proba 0.25 if layout,
        // or 0.75
        val point = if (v.cls.endsWith("ViewGroup") ||
                v.cls.endsWith("Layout") ||
                v.cls.endsWith("LayoutCompat") ||
                v.cls.endsWith("RecyclerView") ||
                v.cls.endsWith("ListView") ||
                v.cls.endsWith("GridView")) {
            getCenterXYWithProbability(rect, 0.25)
        } else {
            getCenterXYWithProbability(rect, 0.75)
        }

        return YotaViewCompoundEvent(v, YotaTapEvent(point.x, point.y))
    }

    private fun swipeEvent(views: List<YotaView>): YotaViewCompoundEvent {
        if (views.isEmpty()) {
            throw EmptyNodeListException()
        }
        // randomly select one to send text event
        return swipeEvent(views[random.nextInt(views.size)])
    }

    private fun swipeEvent(v: YotaView): YotaViewCompoundEvent {
        val rect = v.bounds ?: return YotaViewCompoundEvent(v, YotaSwipeEvent.mustFailEvent())

        val w = rect.width()
        val h = rect.height()
        if (w <= 0 || h <= 0) {
            return YotaViewCompoundEvent(v, YotaSwipeEvent.mustFailEvent())
        }

        val fromX = rect.left + random.nextInt(w)
        val fromY = rect.top + random.nextInt(h)
        val stepX = if (random.nextDouble() > 0.5) random.nextInt(w) else -random.nextInt(w)
        val stepY = if (random.nextDouble() > 0.5) random.nextInt(h) else -random.nextInt(h)
        var toX = fromX + stepX
        var toY = fromY + stepY
        if (toX < rect.left) {
            toX = rect.left
        } else if (toX > rect.right) {
            toX = rect.right
        }
        if (toY < rect.top) {
            toY = rect.top
        } else if (toY > rect.bottom) {
            toY = rect.bottom
        }
        val steps = random.nextInt(10)

        return YotaViewCompoundEvent(v, YotaSwipeEvent(fromX, fromY, toX, toY, steps))
    }

    private fun textEvent(views: List<YotaView>): YotaViewCompoundEvent {
        if (views.isEmpty()) {
            throw EmptyNodeListException()
        }
        // randomly select one to send text event
        return textEvent(views[random.nextInt(views.size)])
    }

    private fun textEvent(v: YotaView): YotaViewCompoundEvent {
        return YotaViewCompoundEvent(v, YotaTextEvent(textFuzzer.nextText()))
    }

    private fun keyEvent(key: Int): YotaKeyEvent = YotaKeyEvent(key)

    private fun noopEvent(): YotaNoopEvent = YotaNoopEvent()

    private fun sleepEvent(ms: Long): YotaSleepEvent = YotaSleepEvent(ms)

    private fun <E, T : Collection<E>> isAllEmpty(arr: Collection<T>): Boolean {
        for (c in arr) {
            if (!c.isEmpty()) {
                return false
            }
        }
        return true
    }

    private fun getCenterXYWithProbability(rect: Rect, p: Double): Point {
        val x: Int
        val y: Int
        if (random.nextDouble() < p) {
            x = rect.centerX()
            y = rect.centerY()
        } else {
            val w = rect.width()
            val h = rect.height()
            x = rect.left + if (w > 0) random.nextInt(w) else 0
            y = rect.top + if (h > 0) random.nextInt(h) else 0
        }
        return Point(x, y)
    }

    data class Proba(
        val lastPage: Double = 1.0,
        val enterAfterText: Double = 1.0
    )

    class EmptyNodeListException : Exception("Node list is empty")
}