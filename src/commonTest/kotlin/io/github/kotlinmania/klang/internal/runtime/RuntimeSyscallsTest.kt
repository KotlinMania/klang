package io.github.kotlinmania.klang.internal.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RuntimeSyscallsTest {
    @Test
    fun fopenWriteSeekAndReadUseRuntimeMemory() {
        val runtime = Runtime()
        with(runtime) {
            val stream = fopen("runtime-stdio-roundtrip.bin".ptr, "w+".ptr)
            assertNotEquals(0, stream.ptr)

            val out = malloc(5)
            memWrite(out, "hello".encodeToByteArray())
            assertEquals(5, fwrite(out, 1, 5, stream))
            assertEquals(5L, ftell(stream))

            assertEquals(0, fseek(stream, 0, RuntimeSyscalls.SEEK_SET))
            val input = malloc(5)
            assertEquals(5, fread(input, 1, 5, stream))

            val bytes = ByteArray(5)
            memRead(input, bytes)
            assertEquals("hello", bytes.decodeToString())
            fclose(stream)
        }
    }

    @Test
    fun appendModeWritesAtEndEvenAfterSeek() {
        val runtime = Runtime()
        with(runtime) {
            val initial = fopen("runtime-stdio-append.bin".ptr, "w".ptr)
            val abc = malloc(3)
            memWrite(abc, "abc".encodeToByteArray())
            assertEquals(3, fwrite(abc, 1, 3, initial))
            fclose(initial)

            val appender = fopen("runtime-stdio-append.bin".ptr, "a+".ptr)
            assertEquals(0, fseek(appender, 0, RuntimeSyscalls.SEEK_SET))
            val suffix = malloc(3)
            memWrite(suffix, "def".encodeToByteArray())
            assertEquals(3, fwrite(suffix, 1, 3, appender))

            assertEquals(0, fseek(appender, 0, RuntimeSyscalls.SEEK_SET))
            val all = malloc(6)
            assertEquals(6, fread(all, 1, 6, appender))

            val bytes = ByteArray(6)
            memRead(all, bytes)
            assertEquals("abcdef", bytes.decodeToString())
            fclose(appender)
        }
    }

    @Test
    fun missingReadFileAndInvalidSeekFailWithoutThrowing() {
        val runtime = Runtime()
        with(runtime) {
            val missing = fopen("runtime-stdio-missing.bin".ptr, "r".ptr)
            assertEquals(0, missing.ptr)

            val stream = fopen("runtime-stdio-seek.bin".ptr, "w+".ptr)
            assertEquals(-1, fseek(stream, -1, RuntimeSyscalls.SEEK_SET))
            assertEquals(-1, fseek(stream, 0, 99))
            fclose(stream)
            assertEquals(-1L, ftell(stream))
        }
    }
}
