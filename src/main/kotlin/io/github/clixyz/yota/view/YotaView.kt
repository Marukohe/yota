package io.github.clixyz.yota.view

import android.graphics.Rect
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Checkable
import android.widget.EditText
import android.widget.TextView
import com.android.uiautomator.core.AccessibilityNodeInfoHelper
import io.github.clixyz.yota.droid.Droid
import org.robolectric.util.ReflectionHelpers

open class YotaView(val node: View, val idx: Int = 0) {

    private lateinit var childrenInternal: MutableList<YotaView>
    private lateinit var apInternal: MutableList<Attr>

    /* properties */

    val children: Array<YotaView>
        get() {
            if (!this::childrenInternal.isInitialized) {
                childrenInternal = mutableListOf()
                if (node is ViewGroup) {
                    for (i in 0 until node.childCount) {
                        val c = node.getChildAt(i)
                        if (c != null) {
                            childrenInternal.add(YotaView(c, i))
                        }
                    }
                }
            }
            return childrenInternal.toTypedArray()
        }

    val srcNodeId: Long
        get() {
            cannotRecycled()
            return AccessibilityNodeInfo.UNDEFINED_NODE_ID
//        return node!!.sourceNodeId
        }

    val pkg: String
        get() {
            cannotRecycled()
            return AccessibilityNodeInfoHelper.safeCharSequenceToString(node.context.packageName)
        }

    val cls: String
        get() {
            cannotRecycled()
            return AccessibilityNodeInfoHelper.safeCharSequenceToString(node.accessibilityClassName)
        }

    val text: String
        get() {
            cannotRecycled()
            return AccessibilityNodeInfoHelper.safeCharSequenceToString(
                when (node) {
                    is Button -> node.text
                    is TextView -> node.text
                    else -> "no text"
                }
            )
        }

    val desc: String
        get() {
            cannotRecycled()
            return AccessibilityNodeInfoHelper.safeCharSequenceToString(node.contentDescription)
        }


    val resId: String
        get() {
            cannotRecycled()
            return node.id.toString()
//            return AccessibilityNodeInfoHelper.safeCharSequenceToString(node!!.viewIdResourceName)
        }

    val bounds: Rect
        get() {
            cannotRecycled()
            val rect = Rect()
            node.getBoundsOnScreen(rect)
            return rect
        }


    val enabled: Boolean
        get() {
            cannotRecycled()
            return node.isEnabled
        }

    val clickable: Boolean
        get() {
            cannotRecycled()
            return node.isClickable
        }


    val longClickable: Boolean
        get() {
            cannotRecycled()
            return node.isLongClickable
        }

    val contextClickable: Boolean
        get() {
            cannotRecycled()
            return node.isContextClickable
        }

    val scrollable: Boolean
        get() {
            cannotRecycled()
            return node.canScrollHorizontally(-1)
                    || node.canScrollHorizontally(1)
                    || node.canScrollVertically(-1)
                    || node.canScrollVertically(1)
        }


    val checkable: Boolean
        get() {
            cannotRecycled()
            return node is Checkable
        }

    val checked: Boolean
        get() {
            cannotRecycled()
            return (node is Checkable) and (node as Checkable).isChecked
        }

    val editable: Boolean
        get() {
            cannotRecycled()
            return node is EditText
        }


    val focusable: Boolean
        get() {
            cannotRecycled()
            return node.isFocusable
        }

    val focused: Boolean
        get() {
            cannotRecycled()
            return node.isFocused
        }

    val password: Boolean
        get() {
            cannotRecycled()
            if (node is EditText) {
                val type = node.inputType
                return (type and InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            return false
        }

    val selected: Boolean
        get() {
            cannotRecycled()
            return node.isSelected
        }

    val visible: Boolean
        get() {
            cannotRecycled()
            return ReflectionHelpers.callInstanceMethod(View::class.java, node, "isVisibleToUser")
        }


    val importantForA11y: Boolean
        get() {
            cannotRecycled()
            return node.isImportantForAccessibility
        }

    val contentInvalid: Boolean
        get() {
            cannotRecycled()
            return false
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
        return Droid.exec {
            it.im.tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    fun tap(offX: Float, offY: Float): Boolean {
        cannotRecycled()

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
        return Droid.exec {
            it.im.longTap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    fun longTap(offX: Float, offY: Float): Boolean {
        cannotRecycled()

        val x = bounds.left + offX
        val y = bounds.top + offY

        return when {
            bounds.left < x || bounds.right > x -> false
            bounds.top < y || bounds.bottom > y -> false
            else -> Droid.exec { it.im.longTap(x, y) }
        }
    }

    fun swipe(dX: Float, dY: Float, duration: Long): Boolean {
        cannotRecycled()
        val fromX = bounds.centerX().toFloat()
        val fromY = bounds.centerY().toFloat()
        val toX = fromX + dX
        val toY = fromY + dY
        return Droid.exec { it.im.swipe(fromX, fromY, toX, toY, duration) }
    }

    fun swipe(offX: Float, offY: Float, dX: Float, dY: Float, duration: Long): Boolean {
        cannotRecycled()
        val fromX = bounds.left + offX
        val fromY = bounds.top + offY
        val toX = fromX + dX
        val toY = fromY + dY
        return when {
            bounds.left < fromX || bounds.right > fromX -> false
            bounds.top < fromY || bounds.bottom > fromY -> false
            else -> Droid.exec { it.im.swipe(fromX, fromY, toX, toY, duration) }
        }
    }

//    fun recycle() {
//        node!!.recycle()
//        node = null
//    }

    private fun calcAttrPath() {
        apInternal = mutableListOf()
        var cur = node
        while (true) {
            apInternal.add(
                Attr(
                    cur.context.packageName,
                    idx
                )
            )
            cur = cur.parent as View
            if (cur == cur.parent) {
                break
            }
        }
    }

    @Throws(AlreadyRecycledException::class)
    private fun cannotRecycled() {
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
