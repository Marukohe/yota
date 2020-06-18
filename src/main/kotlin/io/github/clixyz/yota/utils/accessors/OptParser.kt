package io.github.clixyz.yota.utils.accessors

import io.github.clixyz.yota.utils.OptParser

fun <C> OptParser.getTyped(opt: String, do_cast: (String) -> C): C? {
    return try {
        val value = this.get(opt)
        if (value == null) {
            null
        } else {
            do_cast(value)
        }
    } catch (t: Throwable) {
        null
    }
}
