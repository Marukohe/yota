package io.github.clixyz.yota.droid.delegates

import android.content.Context
import android.hardware.input.IInputManager
import android.hardware.input.InputManager
import android.os.ServiceManager
import android.os.SystemClock
import android.view.*
import io.github.clixyz.yota.droid.DroidDelegate
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowInputManager

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

//    fun tap(view: View): Boolean {
//        val shadowInputManager = Shadow.extract<ShadowInputManager>(InputManager.getInstance())
//        return shadowInputManager.tap(view)
//    }

    fun tap(x: Float, y: Float): Boolean {
        val downAt = SystemClock.uptimeMillis()
        if (tapDown(x, y, downAt)) {
            val upAt = SystemClock.uptimeMillis()
            return tapUp(x, y, downAt, upAt)
        }
        return false
    }

    fun tapDown(x: Float, y: Float, downAt: Long): Boolean {
        return injectMotionEvent(MotionEvent.ACTION_DOWN, downAt, downAt, x, y, 1.0f)
    }

    fun tapUp(x: Float, y: Float, downAt: Long, upAt: Long): Boolean {
        return injectMotionEvent(MotionEvent.ACTION_UP, upAt, downAt, x, y, 0.0f)
    }

    fun tapMove(x: Float, y: Float, downAt: Long, moveAt: Long): Boolean {
        return injectMotionEvent(MotionEvent.ACTION_MOVE, moveAt, downAt, x, y, 1.0f)
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

    private fun injectMotionEvent(action: Int, eventTime: Long, downAt: Long, x: Float, y: Float, pressure: Float): Boolean {
        val DEFAULT_SIZE = 1.0f;
        val DEFAULT_META_STATE = 0;
        val DEFAULT_PRECISION_X = 1.0f;
        val DEFAULT_PRECISION_Y = 1.0f;
        val DEFAULT_EDGE_FLAGS = 0;
        val DEFAULT_DEVICE = InputDevice.SOURCE_TOUCHSCREEN
        val event = MotionEvent.obtain(downAt, eventTime, action, x, y, pressure,
                DEFAULT_SIZE, DEFAULT_META_STATE,
                DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y,
                getInputDeviceId(DEFAULT_DEVICE), DEFAULT_EDGE_FLAGS)
        event.source = DEFAULT_DEVICE
        return injectInputEventWaitForFinish(event)
    }

    private fun injectInputEventWaitForFinish(event: InputEvent): Boolean {
        return try {
            InputManager.getInstance().injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
        } finally {
            event.recycle()
        }
    }

    private fun getInputDeviceId(inputSource: Int): Int {
        val DEFAULT_DEVICE_ID = 0
        val devIds = InputDevice.getDeviceIds()
        for (devId in devIds) {
            val inputDev = InputDevice.getDevice(devId)
            if (inputDev.supportsSource(inputSource)) {
                return devId
            }
        }
        return DEFAULT_DEVICE_ID
    }
}