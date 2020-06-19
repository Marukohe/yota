package io.github.clixyz.yota.cmds

import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.utils.Command
import io.github.clixyz.yota.utils.Logger
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.response.IStatus
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class YotaServer : Command {
    override val name: String = "server"
    override val usage: String by lazy {
        "server: start server at " + SERVER_PORT + " and accept commands\n" +
        "\n" +
        "usage: \n" +
        "  yota server   run server in foreground\n" +
        "  yota serverd  run server as daemon"
    }

    companion object {
        const val SERVER_PORT = 6659

        val SUCCEEDED = Command.Status(0, "succeeded")
        val FAILED_CANNOT_INIT_DROID = Command.Status(1, "failed to initialize droid")
        val FAILED_EXCEPTION = Command.Status(2, "exception happened")
    }

    private var server = Server()
    private var notStopped = false
    private val notStoppedLock = ReentrantLock()

    override fun exec(args: Array<String>): Command.Status {
        return try {
            notStoppedLock.withLock {
                notStopped = true
            }

            // initialize droid, so other handles do not need to
            if (initDroid()) {
                // dvm and art will exit exec main thread exits,
                // no matter whether other running threads are
                // user threads (not daemon threads) or not.
                // this is different from jvm (jvm will hang
                // exec there are user threads running). so we
                // use a flag mNotStopped to notify the stopping
                // behavior
                server.start()
                while (notStopped) {
                    Thread.sleep(100)
                }
                server.stop()
                Droid.deinit()

                SUCCEEDED
            } else {
                FAILED_CANNOT_INIT_DROID
            }
        } catch (t: Throwable) {
            t.message?.let(Logger::e)
            t.stackTrace?.let(Logger::e)
            FAILED_EXCEPTION
        }
    }

    private fun initDroid(): Boolean {
        return try {
            Droid.init()
            true
        } catch (t: Droid.UnableToInitException) {
            false
        }
    }

    interface Handle {
        fun handle(data: JSONObject): Result

        data class Result(val status: IStatus, val code: Int, val data: Any)
    }

    private inner class Server : NanoHTTPD(SERVER_PORT) {

        @Suppress("OverridingDeprecatedMember")
        override fun serve(session: IHTTPSession?): Response {
            val sess = session!!
            val method = sess.method.name
            val uri = sess.uri

            return when {
                uri == "/server/beat" -> { // check liveness
                    withLogging(uri, Status.OK, MIME_PLAINTEXT, "ok")
                }

                uri == "/server/stop" -> { // stop server
                    notStoppedLock.withLock {
                        notStopped = false
                    }
                    withLogging(uri, Status.OK, MIME_PLAINTEXT, "ok")
                }

                method.toUpperCase() == "POST" -> { // command
                    val handle: Handle? = when (uri) {
                        "/commands/dump" -> YotaDump()
                        else -> null
                    }

                    return if (handle == null) { // 404
                        withLogging(uri, Status.NOT_FOUND, MIME_PLAINTEXT,"command not found")
                    } else {
                        val body = sess.getBody() ?: JSONObject()
                        val result = handle.handle(body)
                        val message = """{"code": ${result.code}, "data": ${JSONValue.toJSONString(result.data)}}"""
                        withLogging(uri, result.status, "application/json", message)
                    }
                }

                else -> { // 404
                    withLogging(uri, Status.NOT_FOUND, MIME_PLAINTEXT,"not found")
                }
            }
        }

        private fun IHTTPSession.getBody(): JSONObject? {
            return try {
                val multipart = mutableMapOf<String, String>()
                this.parseBody(multipart)
                JSONValue.parseWithException(multipart["postData"]) as? JSONObject
            } catch (t: Throwable) {
                t.message?.let(Logger::e)
                null
            }
        }

        private fun withLogging(uri: String, status: IStatus, mine: String, message: String): Response {
            Logger.i("${System.currentTimeMillis()} $uri ${status.description}")
            return Response.newFixedLengthResponse(status, mine, message)
        }
    }
}