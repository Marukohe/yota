package io.github.clixyz.yota.cmds

import android.support.test.uiautomator.By
import android.view.KeyEvent
import io.github.clixyz.yota.utils.*
import android.view.KeyEvent.keyCodeFromString
import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.events.*
import io.github.clixyz.yota.utils.OptParser
import io.github.clixyz.yota.utils.accessors.getTyped
import io.github.clixyz.yota.utils.accessors.sliceArray
import java.util.regex.Pattern

class YotaInput : Command {
    override val name: String = "input"
    override val usage: String by lazy {
        "input: an adb-input alike tool\n" +
        "\n" +
        "usage: \n" +
        "  yota input tap -x <x> -y <y>\n" +
        "  yota input longtap -x <x> -y <y>\n" +
        "  yota input swipe --from-x <from-x> \n" +
        "                   --from-y <from-y> \n" +
        "                   --to-x <to-x> \n" +
        "                   --to-y <to-y> \n" +
        "                   --steps <steps>\n" +
        "  yota input key <key>\n" +
        "  yota input text <text>\n" +
        "  yota input view --type <tap|longtap>\n" +
        "                  [--idx <index>]\n" +
        "                  [--cls <cls>] [--cls-matches <regexp>]\n" +
        "                  [--pkg <pkg>] [--pkg-matches <regexp>]\n" +
        "                  [--txt <text>] [--txt-matches <regexp>] [--txt-contains <str>]\n" +
        "                  [--txt-starts-with <str>] [--txt-ends-with <str>]\n" +
        "                  [--desc <desc>] [--desc-matches <regexp>] [--desc-contains <str>]\n" +
        "                  [--desc-starts-with <str>] [--desc-ends-with <str>]\n" +
        "                  [--res-id <id>] [--res-id-matches <regexp>]\n" +
        "                  [--clickable] [--long-clickable]\n" +
        "                  [--checkable] [--checked]\n" +
        "                  [--focusable] [--focused]\n" +
        "                  [--scrollable]\n" +
        "                  [--selected]\n" +
        "                  [--enabled]\n" +
        "                  [--dx <dx> --dy <dy> --steps <steps>]"
    }

    companion object {
        val SUCCEEDED = Command.Status(0, "succeeded")
        val FAILED_INSUFFICIENT_ARGS = Command.Status(1, "insufficient arguments")
        val FAILED_CANNOT_PARSE_EVENT = Command.Status(2, "failed to parse event")
        val FAILED_CANNOT_INJECT_EVENT = Command.Status(3, "failed to inject event")
        val FAILED_NO_SUCH_EVENT = Command.Status(4, "no such event")
        val FAILED_NO_SUCH_VIEW = Command.Status(5, "no such view")
        val FAILED_ROOT_IS_NULL = Command.Status(6, "root is null")
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
            "LONGTAP", "longtap" -> toLongTapEvent(args)
            "SWIPE", "swipe" -> toSwipeEvent(args)
            "KEY", "key" -> toKeyEvent(args)
            "TEXT", "text" -> toTextEvent(args)
            "VIEW", "view" -> toViewEvent(args)
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
            when (event.inject()) {
                YotaEvent.INJECT_SUCCEEDED -> SUCCEEDED
                YotaEvent.INJECT_FAILED_NO_SUCH_VIEW -> FAILED_NO_SUCH_VIEW
                YotaEvent.INJECT_FAILED_NULL_ROOT -> FAILED_ROOT_IS_NULL
                else -> FAILED_CANNOT_INJECT_EVENT
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
        var x: Float? = null
        var y: Float? = null

        for (opt in parser) {
            if (opt == "-x") {
                x = parser.getTyped(opt, java.lang.Float::parseFloat)
            } else if (opt == "-y") {
                y = parser.getTyped(opt, java.lang.Float::parseFloat)
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

    private fun toLongTapEvent(args: Array<String>): YotaEvent? {
        val parser = OptParser(args)
        var x: Float? = null
        var y: Float? = null

        for (opt in parser) {
            if (opt == "-x") {
                x = parser.getTyped(opt, java.lang.Float::parseFloat)
            } else if (opt == "-y") {
                y = parser.getTyped(opt, java.lang.Float::parseFloat)
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

            else -> return YotaLongTapEvent(x, y)
        }
    }

    private fun toSwipeEvent(args: Array<String>): YotaEvent? {
        val parser = OptParser(args)
        var fromX: Float? = null
        var fromY: Float? = null
        var toX: Float? = null
        var toY: Float? = null
        var steps: Int? = null

        for (opt in parser) {
            when (opt) {
                "--from-x" -> fromX = parser.getTyped(opt, java.lang.Float::parseFloat)
                "--from-y" -> fromY = parser.getTyped(opt, java.lang.Float::parseFloat)
                "--to-x" -> toX = parser.getTyped(opt, java.lang.Float::parseFloat)
                "--to-y" -> toY = parser.getTyped(opt, java.lang.Float::parseFloat)
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
        val keyCode = if (key.startsWith("KEYCODE")) {
            keyCodeFromString(key)
        } else {
            keyCodeFromString("KEYCODE_$key")
        }
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            Logger.e("Unknown key: $key")
            return null
        }
        return YotaKeyEvent(keyCode)
    }

    private fun toTextEvent(args: Array<String>): YotaEvent? {
        return YotaTextEvent(args.joinToString(" "))
    }

    private fun toViewEvent(args: Array<String>): YotaEvent? {
        val parser = OptParser(args)
        val selector = By.newSelector()
        var type: String? = null
        var dx: Float? = null
        var dy: Float? = null
        var steps: Int? = null
        try {
            for (opt in parser) {
                when (opt) {
                    "--type" -> type = parser.get(opt)
                    "--idx" -> selector.index(Integer.parseInt(parser.get(opt)))
                    "--cls" -> selector.clazz(parser.get(opt))
                    "--cls-matches" -> selector.clazz(Pattern.compile(parser.get(opt)!!))
                    "--pkg" -> selector.pkg(parser.get(opt))
                    "--pkg-matches" -> selector.pkg(Pattern.compile(parser.get(opt)!!))
                    "--txt" -> selector.text(parser.get(opt))
                    "--txt-matches" -> selector.text(Pattern.compile(parser.get(opt)!!))
                    "--txt-starts-with" -> selector.textStartsWith(parser.get(opt))
                    "--txt-ends-with" -> selector.textEndsWith(parser.get(opt))
                    "--txt-contains" -> selector.textContains(parser.get(opt))
                    "--desc" -> selector.desc(parser.get(opt))
                    "--desc-matches" -> selector.desc(Pattern.compile(parser.get(opt)!!))
                    "--desc-starts-with" -> selector.descStartsWith(parser.get(opt))
                    "--desc-ends-with" -> selector.descEndsWith(parser.get(opt))
                    "--desc-contains" -> selector.descContains(parser.get(opt))
                    "--res-id" -> selector.res(parser.get(opt))
                    "--res-id-matches" -> selector.res(Pattern.compile(parser.get(opt)!!))
                    "--res-id-contains" -> selector.resContains(parser.get(opt))
                    "--clickable" -> selector.clickable(true)
                    "--long-clickable" -> selector.longClickable(true)
                    "--checkable" -> selector.checkable(true)
                    "--checked" -> selector.checked(true)
                    "--focusable" -> selector.focusable(true)
                    "--focused" -> selector.focused(true)
                    "--scrollable" -> selector.scrollable(true)
                    "--enabled" -> selector.enabled(true)
                    "--selected" -> selector.selected(true)
                    "--dx" -> dx = java.lang.Float.parseFloat(parser.get(opt))
                    "--dy" -> dy = java.lang.Float.parseFloat(parser.get(opt))
                    "--steps" -> steps = Integer.parseInt(parser.get(opt))
                }
            }
            if (type == null) {
                Logger.e("No types specified, use --type to specify a type")
                return null
            }
            return when (type) {
                "tap" -> YotaTapViewEvent(selector)
                "longtap" -> YotaLongTapViewEvent(selector)
                "swipe" -> {
                    if (dx == null || dy == null || steps == null) {
                        Logger.e("No dx, dy, or steps specified for swipe, use --dx, --dy, --steps to specify them")
                        return null
                    }
                    YotaSwipeViewEvent(selector, dx, dy, steps)
                }
                else -> {
                    Logger.e("No such view event type $type")
                    null
                }
            }
        } catch (t: Throwable) {
            t.message?.also(Logger::e)
            return null
        }
    }

    private object NotYotaEvent : YotaEvent {
        override fun inject(): Int = 1
    }
}