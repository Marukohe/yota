package io.github.clixyz.yota.droid.delegates

import android.app.ActivityManagerNative
import android.app.IActivityManager
import android.content.ComponentName
import android.os.RemoteException
import io.github.clixyz.yota.droid.DroidDelegate

class AmsDelegate(private val am: IActivityManager)
    : DroidDelegate, IActivityManager by am {

    companion object {
        val FETCHER: DroidDelegate.SingletonFetcher<AmsDelegate>
            get() = object : DroidDelegate.SingletonFetcher<AmsDelegate>() {

                @Throws(DroidDelegate.UnableToFetchException::class)
                override fun doFetch(): AmsDelegate = try {
                    AmsDelegate(ActivityManagerNative.getDefault())
                } catch (t: Throwable) {
                    throw DroidDelegate.UnableToFetchException("activity manager service")
                }
            }
    }

    val topActivityName: String?
    get() {
        val cn = topActivity ?: return null
        return "${cn.packageName}/${cn.className}"
    }

    val foregroundAppName: String?
    get() = topActivity?.packageName

    val topActivity: ComponentName?
    get() = try {
        val tasks = io.github.clixyz.yota.droid.getTasks(am, 2)
        if (tasks.size < 1) {
            null
        } else {
            tasks[0].topActivity
        }
    } catch (e: RemoteException) {
        null
    }
}