@file:Suppress("ktlint:standard:property-naming")

package io.github.kotlinmania.klang.common

actual var LOG_FILE_PATH: String? = null

// No filesystem access without dragging in a file-IO library; route to
// println() in the spirit of the JS variant that routes to console.log.
actual fun logToFile(line: String) {
    println(line)
}

// `System.getenv` resolves to the Kotlin/JVM stdlib bridge at the call site
// (no explicit import needed).
actual fun getEnv(name: String): String? = System.getenv(name)

// Wall-clock millis-since-epoch as a string. Avoids any datetime library
// without an `import java.*` line.
actual fun currentTimestamp(): String = System.currentTimeMillis().toString()
