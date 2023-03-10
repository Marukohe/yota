package io.github.clixyz.yota.droid.delegates

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.graphics.Bitmap
import android.os.HandlerThread
import android.support.test.uiautomator.ByMatcher
import android.support.test.uiautomator.BySelector
import android.support.test.uiautomator.UiDevice
import android.view.accessibility.AccessibilityNodeInfo
import com.android.uiautomator.core.AccessibilityNodeInfoHelper
import io.github.clixyz.yota.utils.accessors.nextInt
import io.github.clixyz.yota.view.YotaView
import io.github.clixyz.yota.view.YotaViewDumper
import io.github.clixyz.yota.view.accessors.YotaViewFilter
import io.github.clixyz.yota.view.accessors.YotaViewOrder
import io.github.clixyz.yota.view.accessors.accept
import org.json.simple.JSONObject
import org.robolectric.server.wm.WindowManagerServiceDelegate
import java.io.*
import java.util.*

object UiAutoDelegate {

    const val NOT_SYSTEM_DIALOG = -1
    const val SYSTEM_DIALOG_NORMAL = 0
    const val SYSTEM_DIALOG_CRASH = 1
    const val SYSTEM_DIALOG_ANR = 2

    private lateinit var ht: HandlerThread
    private lateinit var ua: UiAutomation

    val connected: Boolean
        get() = true
//        get() = this::ht.isInitialized && ht.isAlive

    var compressed: Boolean = false
        set(value) {
            mustConnected()
            if (value != field) {
                field = value
                val info = ua.serviceInfo
                if (value) {
                    info.flags = info.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
                } else {
                    info.flags = info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                }
                ua.serviceInfo = info
            }
        }

    val rootView: YotaView?
        get() {
            val root =  WindowManagerServiceDelegate.getInstance().disPlayView
//            mustConnected()
//            val root = ua.rootInActiveWindow
            return if (root == null) {
                null
            } else {
                YotaView(root)
            }
        }

    fun connect() {
        if (connected) {
            return
        }

        ht = HandlerThread("yota.uiagent").apply { start() }
        ua = UiAutomation(ht.looper, UiAutomationConnection()).apply { connect() }

        compressed = false
    }

    fun disconnect() {
        return
//        if (connected) {
//            ua.disconnect()
//            ht.quit()
//        }
    }

    @Throws(
        HasNotConnectedException::class,
        NullRootException::class
    )
    fun findView(selector: BySelector): YotaView? {
        return null
//        mustConnected()
//        val root: AccessibilityNodeInfo = ua.rootInActiveWindow ?: throw NullRootException()
//        val node = ByMatcher.findMatch(UiDevice.getInstance(), selector, root)
//        return node?.let { YotaView(it) }
    }

    @Throws(
        HasNotConnectedException::class,
        NullRootException::class
    )
    fun findViews(selector: BySelector): List<YotaView> {
        return arrayListOf()
//        mustConnected()
//        val root: AccessibilityNodeInfo = ua.rootInActiveWindow ?: throw NullRootException()
//        val nodes = ByMatcher.findMatches(UiDevice.getInstance(), selector, root)
//        return if (nodes.isEmpty()) {
//            arrayListOf()
//        } else {
//            nodes.map { n -> YotaView(n) }
//        }
    }

    @Throws(
        HasNotConnectedException::class,
        NullRootException::class,
        IOException::class
    )
    fun dump(path: String, extra: Map<String, String> = mapOf(), bg: Boolean = false) {
        dump(PrintStream(FileOutputStream(path)), extra, bg)
    }

    @Throws(
        HasNotConnectedException::class,
        NullRootException::class,
        IOException::class
    )
    fun dump(out: PrintStream, extra: Map<String, String> = mapOf(), bg: Boolean = false) {
        mustConnected()
        val root = rootView ?: throw NullRootException()
        val bm = takeScreenshot() ?: throw NullRootException()
        val rm = YotaViewDumper.hierarchyToMap(root)
        if (bg) {
            addBackground(rm, bm)
        }
        with (out) {
            print(JSONObject(mapOf(
                "extra" to extra,
                "hierarchy" to rm
            )).toJSONString())
            close()
        }
    }

    @Throws(HasNotConnectedException::class)
    fun takeScreenshot(): Bitmap? {
        mustConnected()
        return ua.takeScreenshot()
    }

    @Throws(HasNotConnectedException::class)
    fun checkSystemDialog(): Int {
        mustConnected()
        // TODO crash/anr checking is not sound
//        val root: AccessibilityNodeInfo = ua.rootInActiveWindow ?: return SYSTEM_DIALOG_NORMAL
//        if ("android" != charSeqToStr(root.packageName)) {
//            return NOT_SYSTEM_DIALOG
//        }

//        val rootView = YotaView(root)
//        val rootView = rootView
        val filter = YotaViewFilter { view ->
            "android.widget.TextView" == view.cls || "android.widget.Button" == view.cls
        }
        rootView?.accept(YotaViewOrder.DFS, filter)

        for (view in filter) {
            val text = view.text.toLowerCase()
            when (view.cls) {
                "android.widget.TextView" -> when {
                    text.endsWith("has stopped") -> {
                        return SYSTEM_DIALOG_CRASH
                    }
                    text.endsWith("isn't responding") -> {
                        return SYSTEM_DIALOG_ANR
                    }
                }
                "android.widget.Button" -> when {
                    text.endsWith("open app again") -> {
                        return SYSTEM_DIALOG_CRASH
                    }
                    text.endsWith("close app") -> {
                        return SYSTEM_DIALOG_ANR
                    }
                    text.endsWith("wait") -> {
                        return SYSTEM_DIALOG_ANR
                    }
                }
            }
        }

        return SYSTEM_DIALOG_NORMAL
    }

    @Suppress("MapGetWithNotNullAssertionOperator", "UNCHECKED_CAST")
    private fun addBackground(rm: Map<String, Any>, bm: Bitmap) {
        val defaultBackground = -1

        val q = LinkedList<Map<String, Any>>()
        q.push(rm)
        while (!q.isEmpty()) {
            val m = q.poll() as MutableMap<String, Any>
            val bd = m["bounds"] as Map<String, Int>
            var l = bd["left"]!!
            var r = bd["right"]!!
            var t = bd["top"]!!
            var b = bd["bottom"]!!
            var w = r - l
            var h = b - t

            // FIX: sometimes the left-right, top-bottom is in reversed order
            l = Math.min(l, r)
            r = Math.max(l, r)
            t = Math.min(t, b)
            b = Math.max(t, b)
            w = Math.abs(w)
            h = Math.abs(h)

            if (r <= 0 || b <= 0 || w == 0 || h == 0) {
                // cannot happen, directly skip
                m["background"] = defaultBackground
            } else {
                l = Math.max(0, l)
                t = Math.max(0, t)

                // select sample area
                // +-+---------+-+ <- t
                // | |    1    | |
                // +-+---------+-+ <- t+h*10%
                // | |         | |
                // |2|         |3|
                // | |         | |
                // +-+---------+-+ <- b-h*10%
                // | |    4    | |
                // +-+---------+-+ <- b
                // v |         | v
                // l v         v r
                //   l+w*10%   r-w*10%

                val p = 0.05
                val box = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
                box.add((l to r) to (t to (t+(h*p).toInt()))) // 1
                box.add((l to (l+(w*p).toInt())) to (t to b)) // 2
                box.add(((r-(w*p).toInt()) to r) to (t to b)) // 3
                box.add((l to r) to ((b-(h*p).toInt()) to b)) // 4

                // sample 10 pixels in each area
                val pm = mutableMapOf<Int, Int>() // px -> votes
                val rd = Random(0)
                for (a in box) {
                    val xs = a.first
                    val ys = a.second
                    var x = if (xs.first == xs.second) xs.first else rd.nextInt(xs.first, xs.second)
                    var y = if (ys.first == ys.second) ys.first else rd.nextInt(ys.first, ys.second)
                    x = when {
                        x <= 0 -> {
                            1
                        }
                        x >= bm.width -> {
                            bm.width - 1
                        }
                        else -> {
                            x
                        }
                    }
                    y = when {
                        y <= 0 -> {
                            1
                        }
                        y >= bm.height -> {
                            bm.height - 1
                        }
                        else -> {
                            y
                        }
                    }
                    val px = bm.getPixel(x, y)
                    pm[px] = pm.getOrDefault(px, 1) + 1
                }

                // elect the pixel with most votes
                var max = -1
                var maxPx = -1
                for (e in pm.entries) {
                    if (e.value > max) {
                        max = e.value
                        maxPx = e.key
                    }
                }
                m["background"] = maxPx
            }

            for (c in (m["children"] as List<Map<String, Any>>)) {
                q.push(c)
            }
        }
    }

    @Throws(HasNotConnectedException::class)
    private fun mustConnected() {
        if (!connected) {
            throw HasNotConnectedException()
        }
    }

    private fun charSeqToStr(cs: CharSequence): String {
        return AccessibilityNodeInfoHelper.safeCharSequenceToString(cs)
    }

    class NullRootException : Exception("Root is null")
    class HasNotConnectedException : IllegalStateException("UiAutoDelegate has not connected yet")
}