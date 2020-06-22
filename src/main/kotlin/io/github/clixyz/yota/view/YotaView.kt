package io.github.clixyz.yota.view

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.android.uiautomator.core.AccessibilityNodeInfoHelper
import io.github.clixyz.yota.droid.Droid

open class YotaView(node: AccessibilityNodeInfo, val idx: Int = 0) {

    private var node: AccessibilityNodeInfo? = node
    private lateinit var childrenInternal: MutableList<YotaView>
    private lateinit var apInternal: MutableList<Attr>

    /* properties */

    val children: Array<YotaView>
		get() {
            cannotRecycled()
            if (!this::childrenInternal.isInitialized) {
                childrenInternal = mutableListOf()
                for (i in 0 until node!!.childCount) {
                    val c = node!!.getChild(i)
                    if (c != null) {
                        childrenInternal.add(YotaView(c, i))
                    }
                }
            }
            return childrenInternal.toTypedArray()
        }

    val srcNodeId: Long
    get() {
        cannotRecycled()
        return node!!.sourceNodeId
    }

    val pkg: String
    get() {
        cannotRecycled()
        return AccessibilityNodeInfoHelper.safeCharSequenceToString(node!!.packageName)
    }

    val cls: String
    get() {
        cannotRecycled()
        return AccessibilityNodeInfoHelper.safeCharSequenceToString(node!!.className)
    }

    val text: String
    get() {
        cannotRecycled()
        return AccessibilityNodeInfoHelper.safeCharSequenceToString(node!!.text)
    }

    val desc: String
    get() {
        cannotRecycled()
        return AccessibilityNodeInfoHelper.safeCharSequenceToString(node!!.contentDescription)
    }

    val resId: String
    get() {
        cannotRecycled()
        return AccessibilityNodeInfoHelper.safeCharSequenceToString(node!!.viewIdResourceName)
    }

    val bounds: Rect?
    get() {
        cannotRecycled()
        return AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(node)
    }

    val enabled: Boolean
    get() {
        cannotRecycled()
        return node!!.isEnabled
    }

    val clickable: Boolean
    get() {
        cannotRecycled()
        return node!!.isClickable
    }

    val longClickable: Boolean
    get() {
        cannotRecycled()
        return node!!.isLongClickable
    }

    val contextClickable: Boolean
    get() {
        cannotRecycled()
        return node!!.isContextClickable
    }

    val scrollable: Boolean
    get() {
        cannotRecycled()
        return node!!.isScrollable
    }

    val checkable: Boolean
    get() {
        cannotRecycled()
        return node!!.isCheckable
    }

    val checked: Boolean
    get() {
        cannotRecycled()
        return node!!.isChecked
    }

    val editable: Boolean
    get() {
        cannotRecycled()
        return node!!.isEditable
    }

    val focusable: Boolean
    get() {
        cannotRecycled()
        return node!!.isFocusable
    }

    val focused: Boolean
    get() {
        cannotRecycled()
        return node!!.isFocused
    }

    val password: Boolean
    get() {
        cannotRecycled()
        return node!!.isPassword
    }

    val selected: Boolean
    get() {
        cannotRecycled()
        return node!!.isSelected
    }

    val visible: Boolean
    get() {
        cannotRecycled()
        return node!!.isVisibleToUser
    }

    val importantForA11n: Boolean
    get() {
        cannotRecycled()
        return node!!.isImportantForAccessibility
    }

    val contentInvalid: Boolean
    get() {
        cannotRecycled()
        return node!!.isContentInvalid
    }

    val attrPath: AttrPath
    get() {
        cannotRecycled()
        return attrPath(-1)
    }

    fun attrPath(len: Int): AttrPath {
        cannotRecycled()
        if (!this::apInternal.isInitialized) {
            calcAttrPath()
        }
        return if (len > 0 || len >= apInternal.size) {
            AttrPath(this, apInternal)
        } else {
            AttrPath(this, apInternal.subList(0, len))
        }
    }

    /* actions */

    fun tap(): Boolean {
        cannotRecycled()
        val bounds = node!!.boundsInScreen ?: return false
        return Droid.exec {
            it.im.tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    fun tap(offX: Float, offY: Float): Boolean {
        cannotRecycled()

        val bounds = node!!.boundsInScreen ?: return false

        val x = bounds.left.toFloat() + offX
        val y = bounds.top.toFloat() + offY

        return when {
            bounds.left < x || bounds.right > x -> false
            bounds.top < y || bounds.bottom > y -> false
            else -> Droid.exec { it.im.tap(x, y) }
        }
    }

    fun longTap(): Boolean {
        cannotRecycled()
        val bounds = node!!.boundsInScreen ?: return false
        return Droid.exec {
            it.im.longTap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    fun longTap(offX: Float, offY: Float): Boolean {
        cannotRecycled()

        val bounds = node!!.boundsInScreen ?: return false

        val x = bounds.left + offX
        val y = bounds.top + offY

        return when {
            bounds.left < x || bounds.right > x -> false
            bounds.top < y || bounds.bottom > y -> false
            else -> Droid.exec { it.im.longTap(x, y) }
        }
    }

    fun swipe(dX: Float, dY: Float, steps: Int): Boolean {
        cannotRecycled()
        val bounds = node!!.boundsInScreen ?: return false
        val fromX = bounds.centerX().toFloat()
        val fromY = bounds.centerY().toFloat()
        val toX = fromX + dX
        val toY = fromY + dY
        return Droid.exec { it.im.swipe(fromX, fromY, toX, toY, steps) }
    }

    fun swipe(offX: Float, offY: Float, dX: Float, dY: Float, steps: Int): Boolean {
        cannotRecycled()
        val bounds = node!!.boundsInScreen ?: return false
        val fromX = bounds.left + offX
        val fromY = bounds.top + offY
        val toX = fromX + dX
        val toY = fromY + dY
        return when {
            bounds.left < fromX || bounds.right > fromX -> false
            bounds.top < fromY || bounds.bottom > fromY -> false
            else -> Droid.exec { it.im.swipe(fromX, fromY, toX, toY, steps) }
        }
    }

    fun recycle() {
        node!!.recycle()
        node = null
    }

    private fun calcAttrPath() {
        apInternal = mutableListOf()
        var cur = node
        while (cur != null) {
            apInternal.add(Attr(
                AccessibilityNodeInfoHelper.safeCharSequenceToString(cur.packageName),
                AccessibilityNodeInfoHelper.getIndex(cur)
            ))
            cur = cur.parent
        }
    }

    @Throws(AlreadyRecycledException::class)
    private fun cannotRecycled() {
        if (node == null) {
            throw AlreadyRecycledException()
        }
    }

    data class Attr(val cls: String, val idx: Int)

    class AttrPath(view: YotaView, path: List<Attr>) {
        val view = view
        val path = path.toTypedArray()
        val size = path.size

        fun get(i: Int): Attr? = if (i < size) path[i] else null

        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("[")
            for (i in 0 until size - 1) {
                builder.append("<")
                        .append(path[i].idx)
                        .append(",")
                        .append(path[i].cls)
                        .append(">,")
            }
            builder.append("<")
                    .append(path[path.size - 1].idx)
                    .append(",")
                    .append(path[path.size - 1].cls)
                    .append(">")
            builder.append("]")
            return builder.toString()
        }
    }

    class AlreadyRecycledException : IllegalStateException("Already recycled")
}