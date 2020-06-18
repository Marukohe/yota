package io.github.clixyz.yota.utils

interface Command {
    /**
     * Name of this command
     */
    val name: String

    /**
     * Usage of this command
     */
    val usage: String

    /**
     * Run this command
     */
    fun exec(args: Array<String>): Status

    /**
     * Status of this command
     */
    data class Status(val code: Int, val msg: String)
}