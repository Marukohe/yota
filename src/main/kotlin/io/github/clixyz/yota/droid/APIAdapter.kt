package io.github.clixyz.yota.droid

import android.app.ActivityManager
import android.app.IActivityManager
import java.lang.reflect.Method
import kotlin.system.exitProcess

fun findMethod(clazz: Class<*>, name: String, vararg types: Class<*>): Method? {
    var method: Method? = null
    try {
        method = clazz.getMethod(name, *types)
        method.isAccessible = true
    } catch (ignored: NoSuchMethodException) {
    } catch (ignored: NoSuchMethodError) {
    } catch (e: SecurityException) {
        e.printStackTrace()
        exitProcess(1)
    }
    return method
}

fun invoke(method: Method, reciver: Any?, vararg args: Any?): Any? {
    return try {
        method.invoke(reciver, *args)
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(1)
    }
}

fun getTasks(mAm: IActivityManager, maxNum: Int): List<ActivityManager.RunningTaskInfo> {
    val clazz = mAm.javaClass
    val name = "getTasks"
    var method = findMethod(clazz, name, Int.Companion::class.java, Int.Companion::class.java)
    if (method != null) {
        return invoke(method, mAm, maxNum, 0) as List<ActivityManager.RunningTaskInfo>
    }

    method = findMethod(clazz, name, Int.Companion::class.java)
    if (method != null) {
        return invoke(method, mAm, maxNum) as List<ActivityManager.RunningTaskInfo>
    }

    System.err.println("Cannot resolve method: $name")
    exitProcess(1)
}