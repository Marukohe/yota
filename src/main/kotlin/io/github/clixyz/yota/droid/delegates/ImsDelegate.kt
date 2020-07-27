package io.github.clixyz.yota.droid.delegates

import android.content.Context
import android.hardware.input.IInputManager
import android.hardware.input.InputManager
import android.os.ServiceManager
import android.os.SystemClock
import android.view.*
import io.github.clixyz.yota.droid.DroidDelegate

class ImsDelegate(private val im: IInputManager)
    : DroidDelegate, IInputManager by im {

    companion object {
        val FETCHER = object : DroidDelegate.SingletonFetcher<ImsDelegate>() {

            @Throws(DroidDelegate.UnableToFetchException::class)
            override fun doFetch(): ImsDelegate = try {
                ImsDelegate(IInputManager.Stub.asInterface(
                        ServiceManager.getService(Context.INPUT_SERVICE)))
            } catch (t: Throwable) {
                throw DroidDelegate.UnableToFetchException("input manager service")
            }
        }

        val LONG_TAP_TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()
    }

    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    // tap

    fun tap(x: Float, y: Float): Boolean {
        ViewConfiguration.getLongPressTimeout()
        val downAt = SystemClock.uptimeMillis()
        if (tapDown(x, y, downAt)) {
            val upAt = SystemClock.uptimeMillis()
            return tapUp(x, y, downAt, upAt)
        }
        return false
    }

    fun tapDown(x: Float, y: Float, downAt: Long): Boolean {
        val event = MotionEvent.obtain(downAt, downAt,
                MotionEvent.ACTION_DOWN, x, y, 1)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return injectInputEventWaitForFinish(event)
    }

    fun tapUp(x: Float, y: Float, downAt: Long, upAt: Long): Boolean {
        val event = MotionEvent.obtain(downAt, upAt,
                MotionEvent.ACTION_UP, x, y, 1)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return injectInputEventWaitForFinish(event)
    }

    fun tapMove(x: Float, y: Float, downAt: Long, moveAt: Long): Boolean {
        val event = MotionEvent.obtain(downAt, moveAt,
                MotionEvent.ACTION_MOVE, x, y, 1)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        return injectInputEventWaitForFinish(event)
    }

    // long tap

    fun longTap(x: Float, y: Float): Boolean {
        val downAt = SystemClock.uptimeMillis()
        if (tapDown(x, y, downAt)) {
            SystemClock.sleep(LONG_TAP_TIMEOUT)
            val upAt = System.currentTimeMillis()
            return tapUp(x, y, downAt, upAt)
        }
        return false
    }

    // swipe

    fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMillis: Long = 300L): Boolean {
        var ret: Boolean

        val duration = if (durationMillis < 0) 300 else durationMillis
        val downAt = SystemClock.uptimeMillis()
        ret = tapDown(fromX, fromY, downAt)
        var now = downAt
        val startTime = now
        val endTime = startTime + duration
        while (now < endTime) {
            val elapsedTime = now - startTime
            val alpha = elapsedTime.toFloat() / duration
            ret = ret && tapMove(lerp(fromX, toX, alpha), lerp(fromY, toY, alpha), downAt, now)
            now = SystemClock.uptimeMillis()
        }

        return ret && tapUp(toX, toY, downAt, now)
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

    private fun lerp(a: Float, b: Float, alpha: Float): Float {
        return (b - a) * alpha + a
    }

    private fun injectInputEventWaitForFinish(event: InputEvent): Boolean {
        return try {
            im.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
        } finally {
            event.recycle()
        }
    }
}