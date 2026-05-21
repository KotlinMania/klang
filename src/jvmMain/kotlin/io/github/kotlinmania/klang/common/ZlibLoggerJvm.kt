@file:Suppress("ktlint:standard:property-naming")

package io.github.kotlinmania.klang.common

actual var LOG_FILE_PATH: String? = null

actual fun logToFile(line: String) {
    println(line)
}

actual fun getEnv(name: String): String? = System.getenv(name)

actual fun currentTimestamp(): String = System.currentTimeMillis().toString()
