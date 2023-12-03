package spms.localisation.strings

class ServerActionLocalisationJa: ServerActionLocalisation {
    override fun sendingActionToServer(action_identifier: String): String =
        "アクション「$action_identifier」をサーバーに送信中..."
    override fun actionSentAndWaitingForReply(action_identifier: String): String =
        "アクション「$action_identifier」をサーバーに送信しました。 返信を待つ..."

    override fun receivedReplyFromServer(action_identifier: String): String =
        "サーバーから「$action_identifier」アクションの返信を受け取りました"
    override fun receivedEmptyReplyFromServer(action_identifier: String): String =
        "サーバーから「$action_identifier」アクションの空の返信を受け取りました。返信を待つ..."

    override fun replyNotReceived(timeout_ms: Long): String =
        "タイムアウト（${timeout_ms}ミリ秒）の内にサーバから正確な返信が届きませんでした"

    override val server_completed_request_successfully: String = "サーバーがリクエストを正常に完了しました"
    override fun serverDidNotCompleteRequest(error: String, cause: String): String =
        "サーバーはリクエストを正常に実行されることができませんでした\\nエラー: ${error}\\n原因: $cause"

    override val option_help_json: String = "可能ならば結果のデータをJSONとして出力"

    override val add_item_name: String = "アイテムを追加"
    override val add_item_help: String = "キューにアイテムを付け足す"
    override val add_item_param_item_id: String = "入れるアイテムのYouTube ID"
    override val add_item_param_index: String = "アイテムをキューへと入れつインデックス"

    override val clear_queue_name: String = "キューをクリア"
    override val clear_queue_help: String = "プレイヤーの現在キューをクリア"

    override val move_item_name: String = "アイテムの移動"
    override val move_item_help: String = "インデックス「from」にあるアイテムをインデックス「to」へ移動"
    override val move_item_param_from: String = "移動前のインデックス"
    override val move_item_param_to: String = "移動後ののインデックス"

    override val pause_name: String = "一時停止"
    override val pause_help: String = "再生を一時停止"
    override val play_name: String = "再生"
    override val play_help: String = "再生を再開"
    override val play_pause_name: String = "再生・停止"
    override val play_pause_help: String = "再生をトグル"

    override val remove_item_name: String = "アイテムを削除"
    override val remove_item_help: String = "インデックス「from」にあるアイテムをキューから削除"
    override val remove_param_from: String = "削除するアイテムのインデックス"

    override val seek_to_item_name: String = "アイテムにシーク"
    override val seek_to_item_help: String = "選択されたインデックスのキューアイテムへシーク"
    override val seek_to_item_param_index: String = "シークするアイテムのインデックス"

    override val seek_to_next_name: String = "次にシーク"
    override val seek_to_next_help: String = "一つ後のアイテムへシーク"
    override val seek_to_previous_name: String = "前にシーク"
    override val seek_to_previous_help: String = "一つ前のアイテムへシーク"

    override val seek_to_time_name: String = "時間にシーク"
    override val seek_to_time_help: String = "再生中のアイテムの選択された時間へシーク"
    override val seek_to_time_param_position_ms: String = "シークする時間（ミリ秒）"

    override val set_volume_name: String = "音量を変える"
    override val set_volume_help: String = "再生音量を設定"
    override val set_volume_param_volume: String = "設定する音量（０が無音、１が最高）"

    override val status_name: String = "状態表示"
    override val status_help: String = "サーバーの現在の状態を表示"
    override val status_output_start: String = "サーバー状態"

    override val ready_to_play_name: String = ""
    override val ready_to_play_help: String
        get() = TODO("Not yet implemented")
    override val ready_to_play_param_item_index: String
        get() = TODO("Not yet implemented")
    override val ready_to_play_param_item_id: String
        get() = TODO("Not yet implemented")
}
