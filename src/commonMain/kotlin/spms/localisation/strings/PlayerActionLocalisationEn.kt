package spms.localisation.strings

class PlayerActionLocalisationEn: PlayerActionLocalisation {
    override val set_auth_info_name: String = "Set authentication info"
    override val set_auth_info_help: String = "Provide the player with headers that will be used when streaming audio"
    override val set_auth_info_param_headers: String = "A JSON-encoded map of key-value request headers"

    override val set_local_files_name: String = "Set local files"
    override val set_local_files_help: String = "Identical to addLocalFiles, but clears previously added files before adding new ones"
    override val set_local_files_param_files: String = "A JSON-encoded map where keys are video IDs and values are absolute paths to files"

    override val add_local_files_name: String = "Add local files"
    override val add_local_files_help: String = "Provide the player with paths to local files for playback"
    override val add_local_files_param_files: String = "A JSON-encoded map where keys are video IDs and values are absolute paths to files"

    override val remove_local_files_name: String = "Remove local files"
    override val remove_local_files_help: String = "Remove previously registered local files"
    override val remove_local_files_param_ids: String = "A JSON-encoded list of video IDs to remove"
}
