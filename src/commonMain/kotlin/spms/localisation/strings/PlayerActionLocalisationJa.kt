package spms.localisation.strings

class PlayerActionLocalisationJa: PlayerActionLocalisation {
    override val set_auth_info_name: String = "認証データを設定"
    override val set_auth_info_help: String = "オーディオストリーミングに使用するヘッダーをプレイヤーに送信"
    override val set_auth_info_param_headers: String = "JSONでエンコードされたリクエストヘッダーのマップ"

    override val set_local_files_name: String = "ローカルファイルを設定"
    override val set_local_files_help: String = "addLocalFilesと同じ機能だが、新しいファイルを追加する前にもとから設定されていたファイルをクリア"
    override val set_local_files_param_files: String = "キーが動画IDでバリューがファイルへのパースのJSONでエンコードされたマップ"

    override val cancel_radio_name: String = "ラジオを止める"
    override val cancel_radio_help: String = "現在のプレイヤーラジオを停止する"

    override val add_local_files_name: String = "ローカルファイルを追加"
    override val add_local_files_help: String = "オーディオストリーミングに使用するこのデバイスに存在するファイルをプレイヤーに送信"
    override val add_local_files_param_files: String = "キーが動画IDでバリューがファイルへのパースのJSONでエンコードされたマップ"

    override val remove_local_files_name: String = "ローカルファイルを外す"
    override val remove_local_files_help: String = "前に追加されたローカルファイルをプレイヤーから外す"
    override val remove_local_files_param_ids: String = "アイテムが動画IDのJSONでエンコードされたリスト"
}
