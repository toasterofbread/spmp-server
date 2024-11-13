package dev.toastbits.spms.localisation.strings

import kotlin.time.Duration

class ServerActionLocalisationEn: ServerActionLocalisation {
    override fun sendingActionToServer(action_identifier: String): String =
        "Sending action '$action_identifier' to server..."
    override fun actionSentAndWaitingForReply(action_identifier: String): String =
        "Action '$action_identifier' sent to server. Waiting for reply..."

    override fun receivedReplyFromServer(action_identifier: String): String =
        "Received reply from server for action '$action_identifier'"
    override fun receivedEmptyReplyFromServer(action_identifier: String): String =
        "Received empty reply from server for action '$action_identifier', continuing to wait..."

    override fun replyNotReceived(timeout: Duration): String =
        "Did not receive valid reply from server within timeout ($timeout)"

    override val server_completed_request_successfully: String = "The server completed the request successfully"
    override fun serverDidNotCompleteRequest(error: String, cause: String): String =
        "The server was not able to complete the request\nError: ${error}\nCause: $cause"

    override val option_help_json: String = "Output result data in JSON format if possible"

    override val status_key_queue: String = "Queue"
    override val status_key_state: String = "Playback state"
    override val status_key_is_playing: String = "Is playing"
    override val status_key_current_item_index: String = "Current item index"
    override val status_key_current_position: String = "Item position"
    override val status_key_duration: String = "Item duration"
    override val status_key_repeat_mode: String = "Repeat mode"

    override val add_item_name: String = "Add item"
    override val add_item_help: String = "Add an item to the queue"
    override val add_item_param_item_id: String = "YouTube ID of the item to add"
    override val add_item_param_index: String = "The queue index at which to insert the item"

    override val clear_queue_name: String = "Clear queue"
    override val clear_queue_help: String = "Clear the current player queue"

    override val move_item_name: String = "Move item"
    override val move_item_help: String = "Move the item at index `from` in the queue to index `to`"
    override val move_item_param_from: String = "Initial queue index of the item to move"
    override val move_item_param_to: String = "Destination queue index"

    override val pause_name: String = "Pause"
    override val pause_help: String = "Pause playback"
    override val play_name: String = "Play"
    override val play_help: String = "Resume playback"
    override val play_pause_name: String = "Play/pause"
    override val play_pause_help: String = "Toggle playback pause"

    override val remove_item_name: String = "Remove item"
    override val remove_item_help: String = "Remove the item at index `from` from the queue"
    override val remove_param_from: String = "Queue index of the item to remove"

    override val seek_to_item_name: String = "Seek to item"
    override val seek_to_item_help: String = "Seek to the queue item at the specified index"
    override val seek_to_item_param_index: String = "Queue index of the item to seek to"
    override val seek_to_item_param_position_ms: String = "Optional position in ms to seek to"

    override val seek_to_next_name: String = "Seek to next"
    override val seek_to_next_help: String = "Seek to the next queue item"
    override val seek_to_previous_name: String = "Seek to previous"
    override val seek_to_previous_help: String = "Seek to the previous queue item"
    override val seek_to_previous_param_repeat_threshold_ms: String = "Threshold in ms beyond which seeking will repeat the item"

    override val seek_to_time_name: String = "Seek to time"
    override val seek_to_time_help: String = "Seek to the specified position within the playing item"
    override val seek_to_time_param_position_ms: String = "Position to seek to in milliseconds"

    override val set_repeat_mode_name: String = "Set repeat mode"
    override val set_repeat_mode_help: String = "Set the repeat mode to use for music playback"
    override val set_repeat_mode_param_repeat_mode: String = "Repeat mode index (see SpMsPlayerRepeatMode)"

    override val status_name: String = "Get status"
    override val status_help: String = "Get detailed information about the server's current status"
    override val status_output_start: String = "Server status"

    override val clients_name: String = "Get clients"
    override val clients_help: String = "Get a list of clients connected to the server"

    override val ready_to_play_name: String = "Notify ready to play"
    override val ready_to_play_help: String = "Notify server that client is ready to play the current item"
    override val ready_to_play_param_item_index: String = "Index of the current item"
    override val ready_to_play_param_item_id: String = "ID of the current item"
    override val ready_to_play_param_item_duration_ms: String = "Duration (ms) of the current item"
}
