package io.github.clixyz.yota.utils.accessors

import java.util.*

fun Random.nextInt(from: Int, to: Int): Int {
    return from + nextInt(to - from)
}