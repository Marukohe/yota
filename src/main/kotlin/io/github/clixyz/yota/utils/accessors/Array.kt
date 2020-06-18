package io.github.clixyz.yota.utils.accessors

fun <T> Array<T>.sliceArray(from: Int, to: Int = -1): Array<T> {
    return this.sliceArray(from..(this.size + to))
}