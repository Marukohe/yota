package io.github.clixyz.yota

import io.github.clixyz.yota.cmds.YotaDump
import io.github.clixyz.yota.cmds.YotaInput
import io.github.clixyz.yota.cmds.YotaServer
import io.github.clixyz.yota.cmds.mnky.YotaMnky
import io.github.clixyz.yota.utils.Command
import io.github.clixyz.yota.utils.Logger
import io.github.clixyz.yota.utils.accessors.sliceArray
import java.io.PrintStream
import kotlin.system.exitProcess

private val HELP_COMMAND = object : Command {
    override val name: String
        get() = "help"
    override val usage: String by lazy {
        "help: show help message\n" +
        "\n" +
        "usage:\n" +
        "  yota help [command]  show command help message"
    }

    val SUCCEEDED = Command.Status(0, "succeeded")
    val FAILED = Command.Status(-1, "failed")

    override fun exec(args: Array<String>): Command.Status {
        if (args.isEmpty()) {
            showUsage(System.out)
            return SUCCEEDED
        }

        val name = args[0]
        for (cmd in COMMANDS) {
            if (name == cmd.name) {
                println(cmd.usage)
                return SUCCEEDED
            }
        }

        Logger.e("No such command: $name\n")
        showUsage(System.err)

        return FAILED
    }
}

private val COMMANDS: Array<Command> = arrayOf(
        HELP_COMMAND,
        YotaServer(),
        YotaDump(),
        YotaInput(),
        YotaMnky("/data/local/tmp")
)

private fun showUsage(out: PrintStream) {
    out.println("Yota: yet another testing toolkit for Android")
    for (c in COMMANDS) {
        out.println("\n--")
        out.println(c.usage)
    }
}

fun main(args: Array<String>) {
    try {
        if (args.isEmpty()) {
            showUsage(System.err)
            exitProcess(1)
        }

        val name = args[0]
        for (cmd in COMMANDS) {
            if (name == cmd.name) {
                exitProcess(cmd.exec(args.sliceArray(1)).code)
            }
        }

        Logger.e("No such command: $name")
        showUsage(System.err)

        exitProcess(1)
    } catch (t: Throwable) {
        t.message?.also(Logger::e)
        Logger.e(t.stackTrace)
    }
}