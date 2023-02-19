package io.github.clixyz.yota.utils

import org.robolectric.util.Logger
import java.io.IOException

private val runtime = Runtime.getRuntime()

enum class Cmds(val command: String) {
    //commands for linux
    RESIGN("apksigner sign --ks %s --ks-key-alias %s --ks-pass pass:%s --key-pass pass:%s %s")
}

fun resign(buildToolFolder: String, keyStore: String, alias: String, apkFile: String, pwd: String) {
    Logger.info("resign the instrument app $apkFile")

    val cmd = "$buildToolFolder/${String
        .format(Cmds.RESIGN.command, keyStore, alias, pwd, pwd, apkFile)}"
    val out = execute(cmd)

    if (out.any { it.isNotBlank() }) {
        out.forEach { Logger.info(it) }
    }
}

private fun execute(cmd: String) : Array<String> =
    try {
        val dump = runtime.exec(cmd)
        val rAE = Array(2) { "" }
        dump.inputStream.bufferedReader().useLines { inLines ->
            dump.errorStream.bufferedReader().useLines { errLines ->
                rAE[0] = inLines.joinToString("\n")
                rAE[1] = errLines.joinToString("\n")
            }
        }
        rAE
    } catch (e: IOException) {
        e.printStackTrace()
        emptyArray()
    }