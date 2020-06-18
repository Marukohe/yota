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
                override fun doFetch(): AmsDelegate {
                    return try {
                        AmsDelegate(ActivityManagerNative.getDefault())
                    } catch (t: Throwable) {
                        throw DroidDelegate.UnableToFetchException("activity manager service")
                    }
                }
            }
    }

    val topActivityName: String?
    get() {
        val cn = topActivity ?: return null
        return "${cn.packageName}/${cn.className}"
    }

    val foregroundAppName: String?
    get() {
        return topActivity?.packageName
    }

    val topActivity: ComponentName?
    get() {
        return try {
            val tasks = am.getTasks(2, 0)
            if (tasks.size < 1) {
                null
            } else {
                tasks[0].topActivity
            }
        } catch (e: RemoteException) {
            null
        }
    }
}