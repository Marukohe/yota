package io.github.clixyz.yota.droid.delegates

import android.content.pm.IPackageManager
import android.os.ServiceManager
import io.github.clixyz.yota.droid.DroidDelegate

class PmsDelegate(private val pm: IPackageManager)
    : DroidDelegate, IPackageManager by pm {

    companion object {
        val FETCHER: DroidDelegate.SingletonFetcher<PmsDelegate>
            get() = object : DroidDelegate.SingletonFetcher<PmsDelegate>() {

                @Throws(DroidDelegate.UnableToFetchException::class)
                override fun doFetch(): PmsDelegate {
                    return try {
                        PmsDelegate(IPackageManager.Stub.asInterface(ServiceManager.getService("package")))
                    } catch (t: Throwable) {
                        throw DroidDelegate.UnableToFetchException("package manager service")
                    }
                }
            }
    }
}