@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package cinterop.utils

import kotlinx.cinterop.*

fun CPointer<ByteVar>.safeToKString(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toByte()) {
        ++length
    }

    val bytes = ByteArray(length)
    nativeMemUtils.getByteArray(nativeBytes.pointed, bytes, length)
    return bytes.decodeToString()
}
