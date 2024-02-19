package spms.socketapi.shared

const val SPMS_API_VERSION: Int = 0

const val SPMS_DEFAULT_PORT: Int = 3973
const val SPMS_EXPECT_REPLY_CHAR: Char = '!'
const val SPMS_MESSAGE_MAX_SIZE: Int = 1024

private const val SPMS_MESSAGE_TERMINATOR: Char = '\u0003'
private val ZMQ_MESSAGE_TERMINATOR: Char = '\u0000'

object SpMsSocketApi {
    fun encode(message_parts: List<String>): List<String> =
        message_parts.flatMap { part ->
            val chunks: List<String> = part.chunked(SPMS_MESSAGE_MAX_SIZE - 8)
            chunks.mapIndexed { i, chunk ->
                if (i + 1 == chunks.size) chunk + SPMS_MESSAGE_TERMINATOR else chunk
            }
        }

    fun decode(message_parts: List<String>): List<String> {
        val parts: MutableList<String> = mutableListOf()
        val current_part: StringBuilder = StringBuilder()

        for (part in message_parts) {
            val last: Char = part.getOrNull(part.length - 1) ?: continue
            if (last == SPMS_MESSAGE_TERMINATOR) {
                current_part.appendRange(part, 0, part.length - 1)
            }
            // Zeromq seems to terminate messages with \u0000 (but only sometimes?)
            else if (last == ZMQ_MESSAGE_TERMINATOR) {
                if (part.getOrNull(part.length - 2) == SPMS_MESSAGE_TERMINATOR) {
                    current_part.appendRange(part, 0, part.length - 2)
                }
                else {
                    current_part.appendRange(part, 0, part.length - 1)
                    continue
                }
            }
            else {
                current_part.append(part)
                continue
            }

            parts.add(current_part.toString())
            current_part.clear()
        }

        if (current_part.isNotEmpty()) {
            parts.add(current_part.toString())
        }

        return parts
    }
}
