package dev.toastbits.spms.localisation.strings

import kotlin.time.Duration

class ServerActionLocalisationJa: ServerActionLocalisation {
    override fun sendingActionToServer(action_identifier: String): String =
        "アクション「$action_identifier」をサーバーに送信中..."
    override fun actionSentAndWaitingForReply(action_identifier: String): String =
        "アクション「$action_identifier」をサーバーに送信しました。 返信を待つ..."

    override fun receivedReplyFromServer(action_identifier: String): String =
        "サーバーから「$action_identifier」アクションの返信を受け取りました"
    override fun receivedEmptyReplyFromServer(action_identifier: String): String =
        "サーバーから「$action_identifier」アクションの空の返信を受け取りました。返信を待つ..."

    override fun replyNotReceived(timeout: Duration): String =
        "タイムアウト（$timeout）の内にサーバから正確な返信が届きませんでした"

    override val server_completed_request_successfully: String = "サーバーがリクエストを正常に完了しました"
    override fun serverDidNotCompleteRequest(error: String, cause: String): String =
        "サーバーはリクエストを正常に実行されることができませんでした\\nエラー: ${error}\\n原因: $cause"

    override val option_help_json: String = "可能ならば結果のデータをJSONとして出力"

    override val status_key_queue: String = "キュー"
    override val status_key_state: String = "再生状態"
    override val status_key_is_playing: String = "再生中"
    override val status_key_current_item_index: String = "再生中のアイテムのインデックス"
    override val status_key_current_position: String = "アイテム内の時間"
    override val status_key_duration: String = "アイテムの長さ"
    override val status_key_repeat_mode: String = "リピートモード"

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
    override val seek_to_item_param_position_ms: String = "シークする時間（ミリ秒）（オプション）"

    override val seek_to_next_name: String = "次にシーク"
    override val seek_to_next_help: String = "一つ後のアイテムへシーク"
    override val seek_to_previous_name: String = "前にシーク"
    override val seek_to_previous_help: String = "一つ前のアイテムへシーク"

    override val seek_to_time_name: String = "時間にシーク"
    override val seek_to_time_help: String = "再生中のアイテムの選択された時間へシーク"
    override val seek_to_time_param_position_ms: String = "シークする時間（ミリ秒）"

    override val set_repeat_mode_name: String = "繰り返しモードを設定する"
    override val set_repeat_mode_help: String = "再生の繰り返しモードを設定する"
    override val set_repeat_mode_param_repeat_mode: String = "繰り返しモードのインデックス（SpMsPlayerRepeatModeを参照）"

    override val status_name: String = "状態表示"
    override val status_help: String = "サーバーの現在の状態を表示"
    override val status_output_start: String = "サーバー状態"

    override val clients_name: String = "クライアント情報"
    override val clients_help: String = "サーバーに接続されているクライアントの情報を表示"

    override val ready_to_play_name: String = "サーバーに再生準備完了と知らせる"
    override val ready_to_play_help: String = "クライアントが再生を始める準備ができたとサーバーに知らせる"
    override val ready_to_play_param_item_index: String = "再生準備が完了したアイテムのインデックス"
    override val ready_to_play_param_item_id: String = "再生準備が完了したアイテムのID"
    override val ready_to_play_param_item_duration_ms: String = "再生準備が完了したアイテムの長さ（ミリ秒）"
}
