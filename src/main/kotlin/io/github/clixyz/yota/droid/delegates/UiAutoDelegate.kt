package io.github.clixyz.yota.droid.delegates

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.graphics.Bitmap
import android.os.HandlerThread
import android.view.accessibility.AccessibilityNodeInfo
import com.android.uiautomator.core.AccessibilityNodeInfoHelper
import io.github.clixyz.yota.ui.YotaView
import io.github.clixyz.yota.ui.YotaViewDumper
import io.github.clixyz.yota.ui.accessors.YotaViewFilter
import io.github.clixyz.yota.ui.accessors.YotaViewOrder
import io.github.clixyz.yota.ui.accessors.accept
import java.io.*

object UiAutoDelegate {

    const val NOT_SYSTEM_DIALOG = -1
    const val SYSTEM_DIALOG_NORMAL = 0
    const val SYSTEM_DIALOG_CRASH = 1
    const val SYSTEM_DIALOG_ANR = 2

    private lateinit var ht: HandlerThread
    private lateinit var ua: UiAutomation

    val connected: Boolean
        get() = this::ht.isInitialized && ht.isAlive

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
            mustConnected()
            val root = ua.rootInActiveWindow
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
        if (connected) {
            ua.disconnect()
            ht.quit()
        }
    }

    @Throws(
        HasNotConnectedException::class,
        NullRootException::class,
        IOException::class
    )
    fun dump(path: String) {
        dump(path, mapOf())
    }

    @Throws(
        HasNotConnectedException::class,
        NullRootException::class,
        IOException::class
    )
    fun dump(path: String, extra: Map<String, String>) {
        mustConnected()
        val root = ua.rootInActiveWindow ?: throw NullRootException()
        with (PrintStream(FileOutputStream(path))) {
            YotaViewDumper.dump(YotaView(root), this, extra)
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
        val root: AccessibilityNodeInfo = ua.rootInActiveWindow ?: return SYSTEM_DIALOG_NORMAL
        if ("android" != charSeqToStr(root.packageName)) {
            return NOT_SYSTEM_DIALOG
        }

        val rootView = YotaView(root)
        val filter = YotaViewFilter { view ->
            "android.widget.TextView" == view.cls || "android.widget.Button" == view.cls
        }
        rootView.accept(YotaViewOrder.DFS, filter)

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