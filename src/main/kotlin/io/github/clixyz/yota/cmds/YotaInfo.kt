package io.github.clixyz.yota.cmds

import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.utils.Command
import io.github.clixyz.yota.utils.Logger
import io.github.clixyz.yota.utils.accessors.sliceArray
import org.json.simple.JSONObject


class YotaInfo : Command {
    override val name: String = "info"
    override val usage: String by lazy {
        "info: get information\n" +
        "\n" +
        "Available commands:\n" +
        "  yota info uiroot       show ui root info\n" +
        "  yota info topactivity  show top activity info\n" +
        "  yota info appcrash     show whether app crash\n" +
        "  yota info appanr       show whether app anr\n" +
        "  yota info isidle       test whether idle or not"
    }

    companion object {
        val CMD_SUCCEEDED = Command.Status(0, "succeeded")
        val CMD_FAILED_INSUFFICIENT_ARGS = Command.Status(-1, "args are insufficient")
        val CMD_FAILED_UNRECOGNIZED_SUBCOMMAND = Command.Status(-2, "unknown subcommand")
        val CMD_FAILED_NULL_ROOT = Command.Status(-3, "failed to get system window: is null")
        val CMD_FAILED = Command.Status(-4, "failed")
    }

    override fun exec(args: Array<String>): Command.Status {
        if (args.isEmpty()) {
            Logger.e("Insufficient arguments")
            return CMD_FAILED_INSUFFICIENT_ARGS
        }

        val command = args[0]
        val subArgs: Array<String> = args.sliceArray(1, args.size - 1)
        when (command) {
//            "uiroot" -> return showUiRoot(subArgs)
            "topactivity" -> return showTopActivity(subArgs)
//            "isidle" -> return showIsIdle(subArgs)
//            "appanr" -> return showAppAnr(subArgs)
//            "appcrash" -> return showAppCrash(subArgs)
        }

        Logger.e("Unrecognized subcommand $command")
        return CMD_FAILED_UNRECOGNIZED_SUBCOMMAND
    }

    private fun showTopActivity(args: Array<String>): Command.Status {
        try {
            Droid.exec { Logger.println(it.am.topActivityName!!) }
        } catch (t: UiAutoDelegate.NullRootException) {
            Logger.e("Root is null at present")
            return CMD_FAILED_NULL_ROOT
        } catch (t: NullPointerException) {
            Logger.w("Failed to get top activity")
            return CMD_FAILED
        }
        return CMD_SUCCEEDED
    }
}