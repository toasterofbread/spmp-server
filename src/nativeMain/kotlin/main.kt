import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.ULongVarOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import libzmq.*

val TEST_SONGS = listOf(
    "dQw4w9WgXcQ",
    "7JANm3jOb2k",
    "BeLsFW4m194",
    "0MZJduzi1OU",
    "PWbRleMGagU"
)

const val PORT = 3973
const val MESSAGE_SIZE = 255

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    println("--- main(${args.toList()}) ---")

//    val mpv = object : EventEmitterMpvClient() {
//        override fun onEvent(event: PlayerEvent) {
//            println("EVENT: $event")
//        }
//    }

    val context = zmq_ctx_new()
    val socket = zmq_socket(context, ZMQ_ROUTER)
    var rc: Int = zmq_bind(socket, "tcp://*:$PORT")
    check(rc == 0)

    val poller = zmq_poller_new()
    zmq_poller_add(poller, socket, null, ZMQ_POLLIN.toShort())

    runBlocking {
        memScoped {
            val message_buffer: CPointer<ByteVarOf<Byte>> = allocArray(MESSAGE_SIZE)
            val message_buffer_size = (sizeOf<ByteVar>() * MESSAGE_SIZE).toULong()

            val has_more: IntVar = alloc()
            val has_more_size: ULongVarOf<ULong> = alloc()
            has_more_size.value = sizeOf<IntVar>().toULong()

            while (true) {
                val event: zmq_poller_event_t = alloc()
                zmq_poller_wait(poller, event.ptr, -1)

                println("Got event: ${event.events}")

                if (event.events.toInt() == ZMQ_POLLIN) {
                    val message: MutableList<String> = mutableListOf()

                    do {
                        val size = zmq_recv(socket, message_buffer, message_buffer_size, 0)
                        check(size >= 0)

                        if (message.isEmpty()) {
                            val client_id = message_buffer.pointed.ptr.readBytes(size).contentHashCode()
                            message.add("$client_id")
                        }
                        else {
                            val bytes = message_buffer.pointed.ptr.readBytes(size)
                            message.add(bytes.decodeToString())
                        }

                        rc = zmq_getsockopt(socket, ZMQ_RCVMORE, has_more.ptr, has_more_size.ptr)
                        check(rc == 0)
                    }
                    while (has_more.value == 1)

                    println("Collected message: ${message.toList()}")
                }
                else {
                    println("Skipping event, no message ${event.events}")
                }

                delay(500)
            }
        }
    }

    println("--- main() finished ---")
}
