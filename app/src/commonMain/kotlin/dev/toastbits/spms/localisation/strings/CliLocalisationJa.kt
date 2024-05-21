package dev.toastbits.spms.localisation.strings

import dev.toastbits.spms.server.BUG_REPORT_URL
import kotlin.time.Duration

class CliLocalisationJa: CliLocalisation {
    override val bug_report_notice: String = "$BUG_REPORT_URL にバグを報告してください"

    override fun commandHelpRoot(mpv_enabled: Boolean): String =
        "SpMpのデスクトップ用サーバープログラム。" + (
            if (mpv_enabled) "オーディオのストリーミングと再生にはmpvを使用します。"
            else "このバイナリは mpv サポートなしでコンパイルされました。常に「headless」モードを使用します。"
        )

    override val command_help_ctrl: String = "他のサーバと交流するコマンドラインインターフェイス"

    override val option_group_help_controller: String = "CLIのオプション"
    override val option_help_version: String = "バージョン情報を出力し、プログラムを終える"
    override val option_help_silent: String = "ログを出力しない"
    override val option_help_language: String = "出力されるログの言語"
    override val option_help_halt: String = "実行時にプログラムを終える"
    override val option_help_server_ip: String = "接続先のサーバーのIPアドレス"
    override val option_help_server_port: String = "接続先のサーバーのポート"

    override val indicator_button_open_client: String = "クライアントを開く"
    override val indicator_button_stop_server: String = "終了"

    override fun connectingToSocket(address: String): String =
        "${address}のソケットに接続中..."
    override val releasing_socket: String = "ソケットの削除中..."

    override val sending_handshake: String = "ハンドシェイクの送信中..."
    override val handshake_reply_received: String = "サーバーからハンドシェイクの返信が来ました"

    override val poll_polling_server_for_events: String = "サーバーのイベントをポール中..."

    override fun errServerDidNotRespond(timeout: Duration): String =
        "タイムアウト（$timeout）の内にサーバーから返信が来ませんでした"
    override fun errServerDidNotSendEvents(timeout: Duration): String =
        "タイムアウト（$timeout）の内にサーバーからイベントが来ませんでした"
}
