@file:Suppress("ktlint:standard:property-naming")

package io.github.kotlinmania.klang.common

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual var LOG_FILE_PATH: String? = null

actual fun logToFile(line: String) {
    val path = LOG_FILE_PATH ?: return
    runCatching { File(path).appendText(line + "\n") }
}

actual fun getEnv(name: String): String? = System.getenv(name)

actual fun currentTimestamp(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date())
}
