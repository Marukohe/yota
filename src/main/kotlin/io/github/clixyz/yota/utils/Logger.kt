package io.github.clixyz.yota.utils

import java.io.PrintStream

object Logger {

    fun e(trace: Array<StackTraceElement>) {
        for (e in trace) {
            System.err.println("[Yota] **   " + e.toString())
        }
    }

    fun e(msg: String) {
        System.err.println("[Yota] ** Error: $msg")
    }

    fun w(msg: String, out: PrintStream = System.out) {
        out.println("[Yota] // Warning: $msg")
    }

    fun d(msg: String, out: PrintStream = System.out) {
        out.println("[Yota] >>>>>>>>>> $msg")
    }

    fun i(msg: String, out: PrintStream = System.out) {
        out.println("[Yota] $msg")
    }

    fun println(msg: String, out: PrintStream = System.out) {
        out.println(msg)
    }

    fun printf(format: String, out: PrintStream = System.out, vararg args: String) {
        out.printf(format, args)
    }

    fun beginTrace(msg: String, out: PrintStream = System.out) {
        out.println(">>>>>>>>>> Begin: $msg")
    }

    fun endTrace(msg: String, out: PrintStream = System.out) {
        out.println(">>>>>>>>>> End: $msg")
    }
}