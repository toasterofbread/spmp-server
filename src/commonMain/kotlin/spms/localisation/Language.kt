package spms.localisation

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv
import spms.socketapi.shared.SpMsLanguage

@OptIn(ExperimentalForeignApi::class)
val SpMsLanguage.Companion.current: SpMsLanguage
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
