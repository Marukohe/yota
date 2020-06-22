package io.github.clixyz.yota.utils

class OptParser(val args: Array<String>) {

    private var parsed = false
    private var nextArg = 0
    private var curArgData: String? = null
    private var parsedArgs = mutableMapOf<String, String?>()

    constructor(args: List<String>) : this(args.toTypedArray())

    fun get(opt: String): String? = parsedArgs[opt]

    operator fun iterator(): Iterator<String> {
        if (!parsed) {
            parse()
        }
        return parsedArgs.keys.iterator()
    }

    private fun parse() {
        var opt: String?
        while (true) {
            opt = nextOption()
            if (opt == null) {
                break
            }
            parsedArgs[opt] = nextOptionData()
        }
        nextArg = -1
        curArgData = null
    }

    /**
     * Return the next command line option. This has a number of special cases
     * which closely, but not exactly, follow the POSIX command line options
     * patterns:
     *
     * -- means to stop processing additional options
     * -z means option z
     * -z ARGS means option z with (non-optional) arguments ARGS
     * -zARGS means option z with (optional) arguments ARGS
     * --zz means option zz
     * --zz ARGS means option zz with (non-optional) arguments ARGS
     *
     * Note that you cannot combine single letter options; -abc != -a -b -c
     *
     * @return Returns the option string, or null if there are no more options.
     */
    private fun nextOption(): String? {
        if (nextArg >= args.size) {
            return null
        }

        val arg = args[nextArg]
        if (!arg.startsWith("-")) { // not an argument, stop processing
            return null
        }
        nextArg++
        if (arg == "--") { // --, stop processing
            return null
        }
        if (arg.length > 1 && arg[1] != '-') { // -z, -z ARGS, or -zARGS
            if (arg.length > 2) { // -zARGS
                curArgData = arg.substring(2) // ARGS
                return arg.substring(0, 2) // -z
            } else { // -z, or -z ARGS
                curArgData = null
                return arg // -z
            }
        } else if (arg.length > 1) { // --zz, or --zz ARGS
            curArgData = null
            return arg // --zz
        } else { // -, skip it
            curArgData = null
            return nextOption()
        }
    }

    /**
     * Return the next data associated with the current option.
     *
     * @return Returns the data string, or null of there are no more arguments.
     */
    private fun nextOptionData(): String? {
        if (curArgData != null) {
            return curArgData!!
        }
        if (nextArg >= args.size) {
            return null
        }
        var arg = args[nextArg]
        if (arg.startsWith("-")) { // next option
            return null
        } else if (arg.startsWith("\"") && arg.endsWith("\"")) { // wrapped with ""
            arg = arg.substring(1, arg.length - 1)
        } else if (arg.startsWith("'") && arg.endsWith("'")) { // wrapped with ''
            arg = arg.substring(1, arg.length - 1)
        }
        nextArg++
        return arg
    }
}

fun main(args: Array<String>) {
    var parse = OptParser("-q1 -w_3 --neg \"-12\" -t 2_3 -e - --a --s 23 --sd 24 -- -p 23 -w 3".split(" "))
    for (opt in parse) {
        println("$opt => ${parse.get(opt)}")
    }

    println()

    parse = OptParser("-p com.example.simon.myapplication -s 0 -P dfs -C 10".split(" "))
    for (opt in parse) {
        println("$opt => ${parse.get(opt)}")
    }

    println()

    parse = OptParser("-x \"208.95996\" -y \"1133.9062\"".split(" "))
    for (opt in parse) {
        println("$opt => ${parse.get(opt)}")
    }
}