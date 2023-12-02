package spms.localisation.strings

import spms.localisation.Language
import spms.localisation.SpMsLocalisation

class LocalisationJa: SpMsLocalisation {
    override val language: Language = Language.JA

    override val server: ServerLocalisation = ServerLocalisationJa()
    override val server_actions: ServerActionLocalisation = ServerActionLocalisationJa()
    override val cli: CliLocalisation = CliLocalisationJa()

    override fun usageError(): String =
        "エラー:"

    override fun badParameter(): String =
        "不正なバリュー"

    override fun badParameterWithMessage(message: String): String =
        "不正なバリュー: $message"

    override fun badParameterWithParam(paramName: String): String =
        "${paramName}には不正なバリュー"

    override fun badParameterWithMessageAndParam(paramName: String, message: String): String =
        "${paramName}には不正なバリュー: $message"

    override fun missingOption(paramName: String): String =
        "${paramName}オプションが必要です"

    override fun missingArgument(paramName: String): String =
        "${paramName}パラメータが必要です"

    override fun noSuchSubcommand(name: String, possibilities: List<String>): String =
        "「$name」というサブコマンドはありません" + when (possibilities.size) {
            0 -> ""
            1 -> "。もしかしたら${possibilities[0]}？"
            else -> possibilities.joinToString(prefix = "。（", postfix = "かもしれません）")
        }

    override fun noSuchOption(name: String, possibilities: List<String>): String =
        "「$name」というオプションはありません" + when (possibilities.size) {
            0 -> ""
            1 -> "。もしかしたら${possibilities[0]}？"
            else -> possibilities.joinToString(prefix = "。（", postfix = "かもしれません）")
        }

    override fun incorrectOptionValueCount(name: String, count: Int): String =
        when (count) {
            0 -> "オプション${name}に入力の必要はありません"
            1 -> "オプション${name}に入力の必要があります"
            else -> "オプション${name}に${count}個のバリューを入力する必要があります"
        }

    override fun incorrectArgumentValueCount(name: String, count: Int): String =
        when (count) {
            0 -> "パラメータ${name}に入力の必要はありません"
            1 -> "パラメータ${name}に入力の必要があります"
            else -> "パラメータ${name}に${count}個のバリューを入力する必要があります"
        }

    override fun mutexGroupException(name: String, others: List<String>): String =
        "オプション${name}は${others.joinToString("、")}と一緒に使えません"

    override fun fileNotFound(filename: String): String =
        "${filename}が見つかりませんでした"

    override fun invalidFileFormat(filename: String, message: String): String =
        "ファイルのフォーマットが正しくありません $filename: $message"

    override fun invalidFileFormat(filename: String, lineNumber: Int, message: String): String =
        "ファイルのフォーマットが正しくありません ${filename}の${lineNumber}行: $message"

    override fun unclosedQuote(): String =
        "閉じられてないクォーテーションマーク"

    override fun fileEndsWithSlash(): String =
        "ファイルの最後に \\"

    override fun extraArgumentOne(name: String): String =
        "想定外のパラメータ $name"

    override fun extraArgumentMany(name: String, count: Int): String =
        "複数の想定外のパラメータ $name"

    override fun invalidFlagValueInFile(name: String): String =
        "オプション${name}のファイルに不正なフラグがあります"

    override fun switchOptionEnvvar(): String =
        "スイッチオプションで環境変数は使用できません"

    override fun requiredMutexOption(options: String): String =
        "${options}のいずれかを入力しなければなりません"

    override fun invalidGroupChoice(value: String, choices: List<String>): String =
        "不可能な選択: $value. (${choices.joinToString()}のいずれかを使用できます)"

    override fun floatConversionError(value: String): String =
        "${value}は正確な少数ではありません"

    override fun intConversionError(value: String): String =
        "${value}は正確な整数ではありません"

    override fun boolConversionError(value: String): String =
        "${value}はブール値ではありません"

    override fun rangeExceededMax(value: String, limit: String): String =
        "${value}は最高数の${limit}より高いです。"

    override fun rangeExceededMin(value: String, limit: String): String =
        "${value}は最低数の${limit}より低いです。"

    override fun rangeExceededBoth(value: String, min: String, max: String): String =
        "${value}は範囲の${min}と${max}の外にあります"

    override fun invalidChoice(choice: String, choices: List<String>): String =
        "不可能な選択: $choice. (${choices.joinToString()}のいずれかを使用できます)"

    override fun pathTypeFile(): String =
        "ファイル"

    override fun pathTypeDirectory(): String =
        "ディレクトリ"

    override fun pathTypeOther(): String =
        "経路"

    override fun pathDoesNotExist(pathType: String, path: String): String =
        "$pathType \"$path\"は存在しません。"

    override fun pathIsFile(pathType: String, path: String): String =
        "$pathType \"$path\"はファイルです。"

    override fun pathIsDirectory(pathType: String, path: String): String =
        "$pathType \"$path\"はディレクトリです。"

    override fun pathIsNotWritable(pathType: String, path: String): String =
        "$pathType \"$path\"に出力できません。"

    override fun pathIsNotReadable(pathType: String, path: String): String =
        "$pathType \"$path\"から読み取れません。"

    override fun pathIsSymlink(pathType: String, path: String): String =
        "$pathType \"$path\"はソフトリンクです。"

    override fun defaultMetavar(): String =
        "バリュー"

    override fun stringMetavar(): String =
        "テキスト"

    override fun floatMetavar(): String =
        "float"

    override fun intMetavar(): String =
        "int"

    override fun pathMetavar(): String =
        "経路"

    override fun fileMetavar(): String =
        "ファイル"

    override fun usageTitle(): String =
        "使用:"

    override fun optionsTitle(): String =
        "オプション"

    override fun argumentsTitle(): String =
        "パラメータ"

    override fun commandsTitle(): String =
        "コマンド"

    override fun optionsMetavar(): String =
        "オプション"

    override fun commandMetavar(): String =
        "コマンド"

    override fun argumentsMetavar(): String =
        "パラメータ"

    override fun helpTagDefault(): String =
        "デフォルト"

    override fun helpTagRequired(): String =
        "必要"

    override fun helpOptionMessage(): String =
        "このメッセージを表示して、終える"
}
