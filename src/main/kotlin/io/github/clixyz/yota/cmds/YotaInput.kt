package io.github.clixyz.yota.cmds

import android.view.KeyEvent
import io.github.clixyz.yota.utils.*
import android.view.KeyEvent.keyCodeFromString
import io.github.clixyz.yota.events.*
import io.github.clixyz.yota.utils.OptParser
import io.github.clixyz.yota.utils.accessors.getTyped
import io.github.clixyz.yota.utils.accessors.sliceArray
import org.json.simple.JSONObject

class YotaInput : Command {
    override val name: String
        get() = "input"
    override val usage: String by lazy {
        "input: an adb-input alike tool\n" +
        "\n" +
        "usage: \n" +
        "  yota input tap -x <x> -y <y>\n" +
        "  yota input swipe --from-x <from-x> \n" +
        "                   --from-y <from-y> \n" +
        "                   --to-x <to-x> \n" +
        "                   --to-y <to-y> \n" +
        "                   --steps <steps>\n" +
        "  yota input key <key>\n" +
        "  yota input text <text>"
    }

    companion object {
        val SUCCEEDED = Command.Status(-0, "succeeded")
        val FAILED_INSUFFICIENT_ARGS = Command.Status(-1, "insufficient arguments")
        val FAILED_CANNOT_PARSE_EVENT = Command.Status(-2, "failed to parse event")
        val FAILED_CANNOT_INJECT_EVENT = Command.Status(-3, "failed to inject event")
        val FAILED_NO_SUCH_EVENT = Command.Status(-4, "no such event")
    }

    override fun exec(args: Array<String>): Command.Status {
        if (args.size < 2) {
            return FAILED_INSUFFICIENT_ARGS.apply {
                Logger.e(msg)
            }
        }

        return injectEvent(parse(args[0], args.sliceArray(1)))
    }

    private fun parse(type: String, args: Array<String>): YotaEvent? {
        return when (type) {
            "TAP", "tap" -> toTapEvent(args)
            "SWIPE", "swipe" -> toSwipeEvent(args)
            "KEY", "key" -> toKeyEvent(args)
            "TEXT", "text" -> toTextEvent(args)
            else -> NotYotaEvent
        }
    }

    private fun injectEvent(event: YotaEvent?): Command.Status {
        if (event == null) {
            return FAILED_CANNOT_PARSE_EVENT
        } else if (event === NotYotaEvent) {
            return FAILED_NO_SUCH_EVENT
        }
        return try {
            if (event.inject() == YotaEvent.INJECT_SUCCEEDED) {
                SUCCEEDED
            } else {
                FAILED_CANNOT_INJECT_EVENT
            }
        } catch (t: Throwable) {
            if (t.message != null) {
                FAILED_CANNOT_INJECT_EVENT.copy(msg = t.message!!)
            } else {
                FAILED_CANNOT_INJECT_EVENT
            }
        }
    }

    private fun toTapEvent(args: Array<String>): YotaEvent? {
        val parser = OptParser(args)
        var x: Int? = null
        var y: Int? = null

        for (opt in parser) {
            if (opt == "-x") {
                x = parser.getTyped(opt, Integer::parseInt)
            } else if (opt == "-y") {
                y = parser.getTyped(opt, Integer::parseInt)
            }
        }

        return when {
            x == null -> {
                Logger.e("-x is not provided, use -x to provide an x coordinate")
                null
            }

            y == null -> {
                Logger.e("-y is not provided, use -y to provide a y coordinate")
                null
            }

            else -> return YotaTapEvent(x, y)
        }
    }

    private fun toSwipeEvent(args: Array<String>): YotaEvent? {
        val parser = OptParser(args)
        var fromX: Int? = null
        var fromY: Int? = null
        var toX: Int? = null
        var toY: Int? = null
        var steps: Int? = null

        for (opt in parser) {
            when (opt) {
                "--from-x" -> fromX = parser.getTyped(opt, Integer::parseInt)
                "--from-y" -> fromY = parser.getTyped(opt, Integer::parseInt)
                "--to-x" -> toX = parser.getTyped(opt, Integer::parseInt)
                "--to-y" -> toY = parser.getTyped(opt, Integer::parseInt)
                "--steps" -> steps = parser.getTyped(opt, Integer::parseInt)
            }
        }

        return when {
            fromX == null -> {
                Logger.e("--from-x is not provided, use --from-x to provide an from x coordinate")
                null
            }

            fromY == null -> {
                Logger.e("--from-y is not provided, use --from-y to provide a from y coordinate")
                null
            }

            toX == null -> {
                Logger.e("--to-x is not provided, use --to-x to provide a to x coordinate")
                null
            }

            toY == null -> {
                Logger.e("--to-y is not provided, use --to-y to provide a to y coordinate")
                null
            }

            steps == null -> {
                Logger.e("--steps is not provided, use --steps to provide a steps")
                null
            }

            else -> YotaSwipeEvent(fromX, fromY, toX, toY, steps)
        }
    }

    private fun toKeyEvent(args: Array<String>): YotaEvent? {
        val key = args[0]
        val keyCode = keyCodeFromString(key)
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            Logger.e("unknow key: $key")
            return null
        }
        return YotaKeyEvent(keyCode)
    }

    private fun toTextEvent(args: Array<String>): YotaEvent? {
        return YotaTextEvent(args.joinToString(" "))
    }

    private object NotYotaEvent : YotaEvent {
        override fun inject(): Int = 1
    }
}