package spms.localisation.strings

interface ServerActionLocalisation {
    fun sendingActionToServer(action_identifier: String): String
    fun actionSentAndWaitingForReply(action_identifier: String): String
    fun receivedReplyFromServer(action_identifier: String): String
    fun receivedEmptyReplyFromServer(action_identifier: String): String
    fun replyNotReceived(timeout_ms: Long): String

    val server_completed_request_successfully: String
    fun serverDidNotCompleteRequest(error: String, cause: String): String

    val option_help_json: String

    // --- Action-specific strings ---

    val add_item_name: String
    val add_item_help: String
    val add_item_param_item_id: String
    val add_item_param_index: String

    val clear_queue_name: String
    val clear_queue_help: String

    val move_item_name: String
    val move_item_help: String
    val move_item_param_from: String
    val move_item_param_to: String

    val pause_name: String
    val pause_help: String
    val play_name: String
    val play_help: String
    val play_pause_name: String
    val play_pause_help: String

    val remove_item_name: String
    val remove_item_help: String
    val remove_param_from: String

    val seek_to_item_name: String
    val seek_to_item_help: String
    val seek_to_item_param_index: String

    val seek_to_next_name: String
    val seek_to_next_help: String
    val seek_to_previous_name: String
    val seek_to_previous_help: String

    val seek_to_time_name: String
    val seek_to_time_help: String
    val seek_to_time_param_position_ms: String

    val set_volume_name: String
    val set_volume_help: String
    val set_volume_param_volume: String

    val status_name: String
    val status_help: String
    val status_output_start: String

    val clients_name: String
    val clients_help: String

    val ready_to_play_name: String
    val ready_to_play_help: String
    val ready_to_play_param_item_index: String
    val ready_to_play_param_item_id: String
    val ready_to_play_param_item_duration_ms: String
}
