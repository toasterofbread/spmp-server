package dev.toastbits.spms.localisation.strings

import kotlin.time.Duration

interface ServerActionLocalisation {
    fun sendingActionToServer(action_identifier: String): String
    fun actionSentAndWaitingForReply(action_identifier: String): String
    fun receivedReplyFromServer(action_identifier: String): String
    fun receivedEmptyReplyFromServer(action_identifier: String): String
    fun replyNotReceived(timeout: Duration): String

    val server_completed_request_successfully: String
    fun serverDidNotCompleteRequest(error: String, cause: String): String

    val option_help_json: String

    val status_key_queue: String
    val status_key_state: String
    val status_key_is_playing: String
    val status_key_current_item_index: String
    val status_key_current_position: String
    val status_key_duration: String
    val status_key_repeat_mode: String

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
    val seek_to_item_param_position_ms: String

    val seek_to_next_name: String
    val seek_to_next_help: String
    val seek_to_previous_name: String
    val seek_to_previous_help: String

    val seek_to_time_name: String
    val seek_to_time_help: String
    val seek_to_time_param_position_ms: String

    val set_repeat_mode_name: String
    val set_repeat_mode_help: String
    val set_repeat_mode_param_repeat_mode: String

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
