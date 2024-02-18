package spms.localisation.strings

class ServerLocalisationJa: ServerLocalisation {
    override fun serverBoundToPort(server: String, port: Int): String =
        "サーバー${server}をポート${port}に接続しました"

    override val polling_started: String = "ポーリングを開始しました"
    override val polling_ended: String = "ポーリングが終了しました"

    override val option_help_port: String = "接続先のサーバーのポート"
    override val option_help_gui: String = "mpvのグラフィカルインタフェースを表示"
    override val option_help_mute: String = "実行時にプレイヤーをミュートする"
    override val option_help_headless: String = "mpvプレイヤーなしで実行"

    override val indicator_button_open_client: String = "クライアントを開く"
    override val indicator_button_stop_server: String = "終了"
}
