package dev.toastbits.spms.localisation

import dev.toastbits.spms.socketapi.shared.SpMsLanguage

actual val SpMsLanguage.Companion.current: SpMsLanguage
    get() {
        var code: String? = null
        for (key in listOf("LANGUAGE", "LANG")) {
            code = System.getenv(key)
            if (code?.isNotBlank() == true) {
                break
            }
        }
        return fromCode(code) ?: default
    }
