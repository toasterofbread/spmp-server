package spms.localisation

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

enum class Language {
    EN, JA;

    companion object {
        val default: Language = EN

        fun fromCode(code: String?): Language? =
            when (code?.split('_', limit = 2)?.firstOrNull()?.uppercase()) {
                "EN" -> EN
                "JA" -> JA
                else -> null
            }

        @OptIn(ExperimentalForeignApi::class)
        val current: Language
            get() {
                var code: String? = null
                for (key in listOf("LANGUAGE", "LANG")) {
                    code = getenv(key)?.toKString()
                    if (code?.isNotBlank() == true) {
                        break
                    }
                }
                return fromCode(code) ?: default
            }
    }
}
