@file:Suppress("ktlint:standard:property-naming")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package io.github.kotlinmania.klang.common

actual var LOG_FILE_PATH: String? = null

actual fun logToFile(line: String) {
    // No filesystem on the wasm-js side; logs go to console when one exists.
    jsConsoleLog(line)
}

actual fun getEnv(name: String): String? = jsGetEnv(name)

actual fun currentTimestamp(): String = jsCurrentTimestamp()

private fun jsConsoleLog(line: String) {
    js("if (typeof console !== 'undefined') { console.log(line); }")
}

private fun jsGetEnv(name: String): String? = js(
    "(typeof process !== 'undefined' && process && process.env && typeof process.env[name] === 'string') ? process.env[name] : null",
)

private fun jsCurrentTimestamp(): String = js("new Date().toISOString()")
