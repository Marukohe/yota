package io.github.clixyz.yota.utils.accessors

import org.json.simple.JSONObject

inline fun <K, reified V> JSONObject.getOrDefaultTyped(key: K, defaultValue: V): V {
    return try {
        this.getOrDefault(key, defaultValue) as V
    } catch (e: ClassCastException) {
        defaultValue
    }
}

fun main(args: Array<String>) {
    val a = JSONObject()
    a["compressed"] = 1
    println(a.getOrDefaultTyped("compressed", true))
}