@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

@file:Suppress("ktlint:standard:property-naming")

package io.github.kotlinmania.klang.common

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlin.time.Clock
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv

actual var LOG_FILE_PATH: String? = null

actual fun logToFile(line: String) {
    memScoped {
        val path = LOG_FILE_PATH ?: "zlib.log"
        val file = fopen(path, "a")
        if (file != null) {
            fputs(line, file)
            fclose(file)
        }
    }
}

actual fun currentTimestamp(): String = Clock.System.now().toString()

actual fun getEnv(name: String): String? {
    val v = getenv(name)
    return v?.toKString()
}
