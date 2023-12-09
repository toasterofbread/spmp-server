package spms.player

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.setenv
import platform.posix.signal
import spms.server.PROJECT_URL
import kotlin.system.exitProcess

@OptIn(ExperimentalForeignApi::class)
class StreamProviderServer(val port: Int) {
    private val server: ApplicationEngine

    init {
        setenv("KTOR_LOG_LEVEL", "WARN", 1)

        server = embeddedServer(
            CIO,
            port = port,
            host = "0.0.0.0",
            configure = {
                reuseAddress = true
            }
        ) {
            routing {
                get("/") {
                    call.respondText("<a href='$PROJECT_URL'>$PROJECT_URL</a>", ContentType("text", "html"))
                }

                get("/stream/{video_id}") {
                    val video_id: String = call.parameters.getOrFail("video_id")

                    val stream_url: String
                    try {
                        stream_url = VideoInfoProvider.getVideoStreamUrl(video_id)
                    }
                    catch (e: Throwable) {
                        call.respond(HttpStatusCode.InternalServerError, e.stackTraceToString())
                        return@get
                    }

                    call.respondRedirect(stream_url)
                }
            }
        }.start(false)

        // Ktor blocks interrupts for some reason
        signal(SIGINT, staticCFunction { signal ->
            exitProcess(signal)
        })
    }

    fun stop() {
        server.stop()
    }

    fun getStreamUrl(): String =
        "http://localhost:$port/stream/"
}
