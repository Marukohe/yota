package io.github.clixyz.yota.droid.delegates

import android.content.Context
import android.content.pm.IPackageManager
import android.hardware.display.DisplayManagerGlobal
import android.os.ServiceManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.IWindowManager
import android.view.WindowManager
import io.github.clixyz.yota.droid.DroidDelegate

class WmsDelegate(private val wm: IWindowManager)
    : DroidDelegate, IWindowManager by wm {

    companion object {
        val FETCHER: DroidDelegate.SingletonFetcher<WmsDelegate>
            get() = object : DroidDelegate.SingletonFetcher<WmsDelegate>() {

                @Throws(DroidDelegate.UnableToFetchException::class)
                override fun doFetch(): WmsDelegate = try {
                    WmsDelegate(IWindowManager.Stub.asInterface(
                            ServiceManager.getService(Context.WINDOW_SERVICE)))
                } catch (t: Throwable) {
                    throw DroidDelegate.UnableToFetchException("window manager service")
                }
            }
    }

    val displayMetrics: DisplayMetrics
    get() {
        val d = DisplayMetrics()
        DisplayManagerGlobal.getInstance()
                .getRealDisplay(Display.DEFAULT_DISPLAY).getMetrics(d)
        return d
    }
}