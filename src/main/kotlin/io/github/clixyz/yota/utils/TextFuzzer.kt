package io.github.clixyz.yota.utils

import java.util.Random

class TextFuzzer(
        val random: Random = Random(System.currentTimeMillis()),
        val maxLength: Int = 128,
        val onlyAlnum: Boolean = false) {

    fun nextText(len: Int = random.nextInt(maxLength)): String {
        return if (onlyAlnum) {
            nextAlnumText(len)
        } else {
            val p = random.nextDouble()
            when {
                p < 0.2 -> nextUnicodeText(len)
                p < 0.6 -> nextAsciiUnicodeMixedText(len)
                else -> nextAsciiText(len)
            }
        }
    }

    fun nextAlnumText(len: Int = random.nextInt(maxLength)): String {
        val builder = StringBuilder()
        val count = 10 + 26 * 2
        for (i in 0 until len) {
            val index = random.nextInt(count)
            if (index < 10) {
                builder.appendCodePoint(0x30 + index)
            } else if (index < 10 + 26) {
                builder.appendCodePoint(0x41 + (index - 10))
            } else {
                builder.appendCodePoint(0x61 + (index - 10 - 26))
            }
        }
        return builder.toString()
    }

    fun nextUnicodeText(len: Int = random.nextInt(maxLength)): String {
        val builder = StringBuilder()
        for (i in 0 until len) {
            val codePoint = random.nextInt(Character.MAX_CODE_POINT + 1)
            if (!Character.isDefined(codePoint) ||
                    Character.isSurrogate(codePoint.toChar()) ||
                    Character.getType(codePoint) == Character.PRIVATE_USE.toInt()) {
                continue
            }

            builder.appendCodePoint(codePoint)
        }
        return builder.toString()
    }

    fun nextAsciiText(len: Int = random.nextInt(maxLength)): String {
        val builder = StringBuilder()
        for (i in 0 until len) {
            var c = random.nextInt(0x7F).toChar()
            while (Character.isISOControl(c)) {
                c = random.nextInt(0x7F).toChar()
            }
            builder.append(c)
        }
        return builder.toString()
    }

    fun nextAsciiUnicodeMixedText(len: Int = random.nextInt(maxLength)): String {
        val builder = StringBuilder()
        var rest = len
        while (rest > 0) {
            val curLen = if (rest < 10) {
                rest
            } else {
                random.nextInt(rest)
            }
            val curText = if (random.nextDouble() < 0.2) {
                nextUnicodeText(curLen)
            } else {
                nextAsciiText(curLen)
            }
            builder.append(curText)
            rest -= curText.length
        }
        return builder.toString()
    }
}

fun main(args: Array<String>) {
    val fuzzer = TextFuzzer(random=Random(0L), maxLength=32)
    println("===================================")
    for (i in 0..999) {
        println(fuzzer.nextText())
    }
    println("===================================")
    for (i in 0..19) {
        println(fuzzer.nextAlnumText())
    }
}