@file:Suppress("ktlint:standard:property-naming")

package io.github.kotlinmania.klang.common

import kotlin.time.Clock

actual var LOG_FILE_PATH: String? = null

actual fun logToFile(line: String) {
    println(line)
}

actual fun getEnv(name: String): String? = null

actual fun currentTimestamp(): String = Clock.System.now().toString()
