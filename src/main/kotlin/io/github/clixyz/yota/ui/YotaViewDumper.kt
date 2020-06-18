package io.github.clixyz.yota.ui

import android.graphics.Rect
import org.json.simple.JSONObject
import java.io.PrintStream

object YotaViewDumper {

    fun viewToMap(view: YotaView): Map<String, Any> {
        val children = view.children.map { child -> viewToMap(child) }
        return mutableMapOf<String, Any>().apply {
            put("index", view.idx)
            put("package", view.pkg)
            put("class", view.cls)
            put("resource-id", view.resId)
            put("visible", view.visible)
            put("text", view.text)
            put("content-desc", view.desc)
            put("clickable", view.clickable)
            put("long-clickable", view.longClickable)
            put("context-clickable", view.contextClickable)
            put("scrollable", view.scrollable)
            put("editable", view.editable)
            put("checkable", view.checkable)
            put("checked", view.checked)
            put("focusable", view.focusable)
            put("focused", view.focused)
            put("selected", view.selected)
            put("password", view.password)
            put("enabled", view.enabled)
            put("bounds", boundsToMap(view.bounds))
            put("children", children)
        }
    }

    fun dump(view: YotaView,
             out: PrintStream = System.out,
             extra: Map<String, String> = mapOf()) {
        out.print(JSONObject(mapOf(
                "extra" to extra,
                "hierarchy" to viewToMap(view)
        )).toJSONString())
    }

    private fun boundsToMap(bounds: Rect?): Map<String, Int> {
        val map = mutableMapOf(
            "left" to -1,
            "right" to -1,
            "top" to -1,
            "bottom" to -1
        )
        if (bounds != null) {
            map["left"] = bounds.left
            map["right"] = bounds.right
            map["top"] = bounds.top
            map["bottom"] = bounds.bottom
        }
        return map
    }
}