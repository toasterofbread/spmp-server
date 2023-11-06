package spms.actions

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import spms.SpMpServer

class ServerActionSeekToItem: ServerAction(
    identifier = "seekToItem",
    help = "Seek to the queue item at the specified index",
    parameters = listOf(
        Parameter(
            Parameter.Type.Int,
            true,
            "index",
            "Queue index of the item to seek to"
        )
    )
) {
    override fun execute(server: SpMpServer, context: Context): JsonElement? {
        val index: Int = context.getParameterValue("index")!!.int
        server.mpv.seekToItem(index)
        return null
    }
}
