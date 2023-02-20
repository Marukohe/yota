package io.github.clixyz.yota.droid.delegates

import android.app.ActivityThread
import android.content.pm.IPackageManager
import io.github.clixyz.yota.droid.DroidDelegate

class PmsDelegate(private val pm: IPackageManager)
    : DroidDelegate, IPackageManager by pm {

    companion object {
        val FETCHER: DroidDelegate.SingletonFetcher<PmsDelegate>
            get() = object : DroidDelegate.SingletonFetcher<PmsDelegate>() {

                @Throws(DroidDelegate.UnableToFetchException::class)
                override fun doFetch(): PmsDelegate = try {
                    // r9c shadowed ActivityThread#getPackageManager, this method will return a proxy
                    PmsDelegate(ActivityThread.getPackageManager())
                } catch (t: Throwable) {
                    throw DroidDelegate.UnableToFetchException("package manager service")
                }
            }
    }
}