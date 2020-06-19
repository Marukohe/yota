package io.github.clixyz.yota.cmds

import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.utils.Command
import io.github.clixyz.yota.utils.Logger
import io.github.clixyz.yota.utils.OptParser
import io.github.clixyz.yota.utils.accessors.getOrDefaultTyped
import org.json.simple.JSONObject
import org.nanohttpd.protocols.http.response.Status
import java.io.IOException

class YotaDump : Command, YotaServer.Handle {
    override val name: String = "dump"
    override val usage: String by lazy {
        "dump: dump ui hierarchy to an output file\n" +
        "\n" +
        "usage:\n" +
        "  yota dump\n" +
        "    [-c|--compressed]\n" +
        "    [-m|--meta KVs]\n" +
        "    -o|--output <OUTPUT_FILE> \n" +
        "\n" +
        "notes:\n" +
        "  KVs  comma separated key=value pairs, e.g., key1=value1,key2=value2\n" +
        "       these values will be added as attributes to the rooted tag"
    }

    companion object {
        val SUCCEEDED = Command.Status(0, "succeeded")
        val FAILED_INSUFFICIENT_ARGS = Command.Status(-1, "args are insufficient")
        val FAILED_ROOT_IS_NULL = Command.Status(-3, "failed to get system window: is null")
        val FAILED_IO_EXCEPTION = Command.Status(-4, "io exception happened")
        val FAILED_EXCEPTION = Command.Status(-4, "exception happened")
    }

    override fun exec(args: Array<String>): Command.Status {
        val parser = OptParser(args)
        var compressed = false
        var output = ""
        val meta = mutableMapOf<String, String>()

        for (opt in parser) {
            if (opt == "--meta" || opt == "-m") {
                val kvs = parser.get(opt)
                if (kvs  == null || kvs.isEmpty() || kvs.contains("-")) {
                    Logger.w("Invalid kv pairs, discard it, use format key1=value1,key2=value2,...")
                } else {
                    val kvArray = kvs.split(",")
                    for (kv in kvArray) {
                        val (key, value) = kv.split("=")
                        meta[key] = value
                    }
                }
            } else if (opt == "--compressed" || opt == "-c") {
                compressed = true
            } else if (opt == "--output" || opt == "-o") {
                val path = parser.get(opt)
                output = if (path == null || path.isEmpty()) {
                    ""
                } else {
                    path
                }
            }
        }

        if (output.isEmpty()) {
            Logger.e("No output file is provided, use -o to specify an output file")
            return FAILED_INSUFFICIENT_ARGS
        }

        return doDump(compressed, meta, output)
    }

    override fun handle(data: JSONObject): YotaServer.Handle.Result {
        // data: {"compressed": true, "meta": {"key": "value"}, "output": "/data/local/tmp/a.xml"}
        val compressed = data.getOrDefaultTyped("compressed", true)
        val meta = mutableMapOf<String, String>().apply {
            val obj = data.getOrDefaultTyped("meta", JSONObject())
            for ((key, value) in obj) {
                this[key.toString()] = value.toString()
            }
        }
        val output = data.getOrDefaultTyped("output", "")

        return if (output.isEmpty()) {
            YotaServer.Handle.Result(Status.OK, FAILED_INSUFFICIENT_ARGS.code, FAILED_INSUFFICIENT_ARGS.msg)
        } else {
            val status = doDump(compressed, meta, output)
            YotaServer.Handle.Result(Status.OK, status.code, status.msg)
        }
    }

    private fun doDump(compressed: Boolean, meta: Map<String, String>, output: String): Command.Status {
        return try {
            Droid.exec {
                it.ua.compressed = compressed
                it.ua.dump(output, meta)
            }
            SUCCEEDED
        } catch (e: UiAutoDelegate.NullRootException) {
            Logger.e("Root is null")
            FAILED_ROOT_IS_NULL
        } catch (e: IOException) {
            Logger.e("IO exception")
            FAILED_IO_EXCEPTION
        } catch (t: Throwable) {
            if (t.message == null) {
                FAILED_EXCEPTION
            } else {
                FAILED_EXCEPTION.copy(msg=t.message!!)
            }
        }
    }
}