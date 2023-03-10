package io.github.clixyz.yota.utils

import android.view.View
import android.view.ViewGroup
import android.view.WindowManagerGlobal
import org.robolectric.server.wm.WindowManagerServiceDelegate
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowView
import org.robolectric.util.ReflectionHelpers
import java.io.File

val path = "${System.getProperty("user.dir")}/results/dumpui"
var count = 1

fun dump() {
    return
//    val iBinder = WindowManagerServiceDelegate.getInstance().displayWindow
//    val views: List<View> = ReflectionHelpers.getField<Any>(WindowManagerGlobal.getInstance(), "mViews") as List<View>
//
//    for (i in views.indices.reversed()) {
//        val view = views[i]
//        if (view.windowToken == iBinder) {
//            dump(view)
//            return
//        }
//    }
}

fun dump(view: View) {
//    val shadowView = Shadow.extract<ShadowView>(view)
//    shadowView.dump()
    val list = ArrayList<FloatArray>()
    list.add(floatArrayOf(0f, 0f, 1440f, 2560f))
    dumpView(list, view)
    writeInfile(view.toString(), list)
}

fun dumpView(list: ArrayList<FloatArray>, view: View) {
    val point = IntArray(2)
    view.getLocationOnScreen(point)

    list.add(floatArrayOf(point[0].toFloat(), point[1].toFloat(), view.width.toFloat(), view.height.toFloat()))
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val childView = view.getChildAt(i)
            dumpView(list, childView)
        }
    }
}

fun writeInfile(className: String, list: ArrayList<FloatArray>) {
    if (!File(path).exists()) {
        File(path).mkdirs()
    }
    val file = File("$path/$count.txt")
    file.bufferedWriter().use{ writer ->
        writer.write(className)
        writer.newLine()
        list.forEach { entry ->
            writer.write("${entry[0].toDouble()},${entry[1].toDouble()},${entry[2].toDouble()},${entry[3].toDouble()}")
            writer.newLine()
        }
    }
    count++
}