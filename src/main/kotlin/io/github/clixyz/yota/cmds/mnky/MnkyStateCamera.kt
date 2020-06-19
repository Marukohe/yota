package io.github.clixyz.yota.cmds.mnky

import android.graphics.Bitmap
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.IOException


class MnkyStateCamera {

    companion object {
        const val CAPTURE_SUCCEEDED = 0
        const val CAPTURE_FAILED = 1
        const val CAPTURE_FAILED_IO_EXCEPTION = 2
        const val CAPTURE_FAILED_NULL_ROOT_EXCEPTION = 3

        const val SCREENSHOT_SUCCEEDED = 0
        const val SCREENSHOT_FAILED = 1
        const val SCREENSHOT_FAILED_IO_EXCEPTION = 2
    }

    fun capture(ua: UiAutoDelegate, to: String): Int = try {
        ua.dump(to)
        CAPTURE_SUCCEEDED
    } catch (e: UiAutoDelegate.NullRootException) {
        CAPTURE_FAILED_NULL_ROOT_EXCEPTION
    } catch (e: IOException) {
        CAPTURE_FAILED_IO_EXCEPTION
    } catch (e: Exception) {
        CAPTURE_FAILED
    }

    fun takeScreenshot(ua: UiAutoDelegate, to: String): Int {
        val screenshot: Bitmap = ua.takeScreenshot() ?: return SCREENSHOT_FAILED
        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(to))
            screenshot.compress(Bitmap.CompressFormat.PNG, 50, bos)
            bos.flush()
        } catch (ioe: IOException) {
            return SCREENSHOT_FAILED_IO_EXCEPTION
        } finally {
            if (bos != null) {
                try {
                    bos.close()
                } catch (ioe: IOException) {
                    /* ignore */
                }
            }
            screenshot.recycle()
        }
        return SCREENSHOT_SUCCEEDED
    }
}