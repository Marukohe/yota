package io.github.clixyz.yota.droid.delegates

import android.content.Context
import android.hardware.input.IInputManager
import android.os.ServiceManager
import android.os.SystemClock
import android.view.*
import io.github.clixyz.yota.droid.DroidDelegate

class ImsDelegate(private val im: IInputManager)
    : DroidDelegate, IInputManager by im {

    companion object {
        val FETCHER = object : DroidDelegate.SingletonFetcher<ImsDelegate>() {

            @Throws(DroidDelegate.UnableToFetchException::class)
            override fun doFetch(): ImsDelegate {
                val im = try {
                    IInputManager.Stub.asInterface(ServiceManager.getService(Context.INPUT_SERVICE))
                } catch (t: Throwable) {
                    throw DroidDelegate.UnableToFetchException("input manager service")
                }
                return ImsDelegate(im)
            }
        }

        const val INJECT_EVENT_ASYNC = 0
        const val INJECT_EVENT_WAIT_FOR_RESULT = 1
        const val INJECT_EVENT_WAIT_FOR_FINISH = 2
    }

    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    // tap

    fun tap(x: Int, y: Int): Boolean {
        val downAt = SystemClock.uptimeMillis()
        if (tapDown(x, y, downAt)) {
            val upAt = SystemClock.uptimeMillis()
            return tapUp(x, y, downAt, upAt)
        }
        return false
    }

    fun tapDown(x: Int, y: Int, downAt: Long): Boolean {
        val event = MotionEvent.obtain(downAt, downAt,
                MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat(), 1)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return injectInputEventWaitForFinish(event)
    }

    fun tapUp(x: Int, y: Int, downAt: Long, upAt: Long): Boolean {
        val event = MotionEvent.obtain(downAt, upAt,
                MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), 1)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return injectInputEventWaitForFinish(event)
    }

    fun tapMove(x: Int, y: Int, downAt: Long, moveAt: Long): Boolean {
        val event = MotionEvent.obtain(downAt, moveAt,
                MotionEvent.ACTION_MOVE, x.toFloat(), y.toFloat(), 1)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return injectInputEventWaitForFinish(event)
    }

    // swipe

    fun swipe(fromX: Int, fromY: Int, toX: Int, toY: Int, steps: Int): Boolean {
        var ret: Boolean

        val downAt = SystemClock.uptimeMillis()
        val swipeSteps = if (steps != 0) steps else 1
        val xStep = (toX - fromX).toDouble() / swipeSteps
        val yStep = (toY - fromY).toDouble() / swipeSteps

        // first tap starts exactly at the point requested
        ret = tapDown(fromX, fromY, downAt)
        for (i in 1 until swipeSteps) {
            ret = ret && tapMove(fromX + (xStep * i).toInt(), fromY + (yStep * i).toInt(),
                    downAt, SystemClock.uptimeMillis())
            if (!ret) {
                break
            }
            // set some known constant delay between steps as without it this
            // become completely dependent on the speed of the system and results
            // may vary on different devices. This guarantees at minimum we have
            // a preset delay.
            SystemClock.sleep(5)
        }

        return ret && tapUp(toX, toY, downAt, SystemClock.uptimeMillis())
    }

    // key

    fun key(keyCode: Int): Boolean {
        val eventTime = SystemClock.uptimeMillis()
        return keyDown(keyCode, eventTime) && keyUp(keyCode, eventTime, eventTime)
    }

    fun keyDown(keyCode: Int, downAt: Long): Boolean {
        val downEvent = KeyEvent.obtain(downAt, downAt,
                KeyEvent.ACTION_DOWN, keyCode,
                0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD, null)
        return injectInputEventWaitForFinish(downEvent)
    }

    fun keyUp(keyCode: Int, downAt: Long, upAt: Long): Boolean {
        val upEvent = KeyEvent.obtain(downAt, upAt,
                KeyEvent.ACTION_UP, keyCode,
                0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_KEYBOARD, null)
        return injectInputEventWaitForFinish(upEvent)
    }

    // text

    fun text(text: String): Boolean {
        // text are sent one by one using KeyEvent, hence unicode will take no effect
        val events = keyCharacterMap.getEvents(text.toCharArray())
        if (events != null) {
            for (event in events) {
                // Authors of UiAutomator say system rejects too old events.
                // Hence, it is possible for an event to become stale before
                // it is injected if it takes too long to inject the preceding
                // ones. Update them here
                if (!injectInputEventWaitForFinish(KeyEvent.changeTimeRepeat(event,
                                SystemClock.uptimeMillis(), 0))) {
                    return false
                }
            }
        }
        return true
    }

    private fun injectInputEventWaitForFinish(event: InputEvent): Boolean {
        return try {
            im.injectInputEvent(event, INJECT_EVENT_WAIT_FOR_FINISH) // 2 for
        } finally {
            event.recycle()
        }
    }
}