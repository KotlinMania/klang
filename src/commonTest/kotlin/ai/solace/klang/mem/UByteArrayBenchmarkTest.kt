package ai.solace.klang.mem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

/**
 * Benchmark comparing ByteArray vs UByteArray for buffer operations.
 *
 * Key question: Does UByteArray eliminate sign-extension overhead
 * and enable cleaner/faster operations?
 */
@OptIn(ExperimentalUnsignedTypes::class)
class UByteArrayBenchmarkTest {

    companion object {
        const val BUFFER_SIZE = 15360  // Same as TUI buffer: 80x24*8
        const val ITERATIONS = 10000
    }

    @Test
    fun testSignExtensionDifference() {
        println("\n" + "=".repeat(70))
        println("SIGN EXTENSION: ByteArray vs UByteArray")
        println("=".repeat(70))

        val signedArr = ByteArray(4)
        val unsignedArr = UByteArray(4)

        // Set high bit in first byte
        signedArr[0] = 0x80.toByte()
        unsignedArr[0] = 0x80u

        // What happens when we shift?
        val signedShift = signedArr[0].toInt() shl 8
        val signedMasked = (signedArr[0].toInt() and 0xFF) shl 8
        val unsignedShift = unsignedArr[0].toInt() shl 8

        println("signedArr[0] = ${signedArr[0]} (0x${signedArr[0].toUByte().toString(16)})")
        println("unsignedArr[0] = ${unsignedArr[0]} (0x${unsignedArr[0].toString(16)})")
        println()
        println("signedArr[0].toInt() shl 8      = $signedShift (0x${signedShift.toUInt().toString(16)})")
        println("(signedArr[0].toInt() and 0xFF) shl 8 = $signedMasked (0x${signedMasked.toUInt().toString(16)})")
        println("unsignedArr[0].toInt() shl 8    = $unsignedShift (0x${unsignedShift.toUInt().toString(16)})")
        println()

        // The key insight
        println("UByteArray eliminates need for 'and 0xFF' mask: ${unsignedShift == signedMasked}")
    }

    @Test
    fun benchmarkLoadWord() {
        println("\n" + "=".repeat(70))
        println("LOAD WORD BENCHMARK: ByteArray vs UByteArray")
        println("=".repeat(70))

        val signedArr = ByteArray(BUFFER_SIZE) { (it and 0xFF).toByte() }
        val unsignedArr = UByteArray(BUFFER_SIZE) { (it and 0xFF).toUByte() }

        // Warm up
        repeat(100) {
            loadWordSigned(signedArr, 0)
            loadWordUnsigned(unsignedArr, 0)
        }

        // Benchmark signed (with masking)
        var signedResult = 0
        val signedTime = measureTime {
            repeat(ITERATIONS) {
                for (addr in 0 until BUFFER_SIZE - 4 step 4) {
                    signedResult = loadWordSigned(signedArr, addr)
                }
            }
        }

        // Benchmark unsigned (no masking needed)
        var unsignedResult = 0
        val unsignedTime = measureTime {
            repeat(ITERATIONS) {
                for (addr in 0 until BUFFER_SIZE - 4 step 4) {
                    unsignedResult = loadWordUnsigned(unsignedArr, addr)
                }
            }
        }

        val signedMs = signedTime.inWholeNanoseconds / 1_000_000.0
        val unsignedMs = unsignedTime.inWholeNanoseconds / 1_000_000.0

        println("ByteArray (with mask):  ${signedMs}ms")
        println("UByteArray (no mask):   ${unsignedMs}ms")
        println("Speedup: ${signedMs / unsignedMs}x")

        assertEquals(signedResult, unsignedResult, "Results should match")
    }

    @Test
    fun benchmarkStoreWord() {
        println("\n" + "=".repeat(70))
        println("STORE WORD BENCHMARK: ByteArray vs UByteArray")
        println("=".repeat(70))

        val signedArr = ByteArray(BUFFER_SIZE)
        val unsignedArr = UByteArray(BUFFER_SIZE)
        val testValue = 0xDEADBEEF.toInt()

        // Warm up
        repeat(100) {
            storeWordSigned(signedArr, 0, testValue)
            storeWordUnsigned(unsignedArr, 0, testValue)
        }

        // Benchmark signed
        val signedTime = measureTime {
            repeat(ITERATIONS) {
                for (addr in 0 until BUFFER_SIZE - 4 step 4) {
                    storeWordSigned(signedArr, addr, testValue)
                }
            }
        }

        // Benchmark unsigned
        val unsignedTime = measureTime {
            repeat(ITERATIONS) {
                for (addr in 0 until BUFFER_SIZE - 4 step 4) {
                    storeWordUnsigned(unsignedArr, addr, testValue)
                }
            }
        }

        val signedMs = signedTime.inWholeNanoseconds / 1_000_000.0
        val unsignedMs = unsignedTime.inWholeNanoseconds / 1_000_000.0

        println("ByteArray store:  ${signedMs}ms")
        println("UByteArray store: ${unsignedMs}ms")
        println("Speedup: ${signedMs / unsignedMs}x")
    }

    @Test
    fun benchmarkMemset() {
        println("\n" + "=".repeat(70))
        println("MEMSET BENCHMARK: ByteArray vs UByteArray")
        println("=".repeat(70))

        val signedArr = ByteArray(BUFFER_SIZE)
        val unsignedArr = UByteArray(BUFFER_SIZE)

        // Warm up
        repeat(100) {
            signedArr.fill(0x20)
            unsignedArr.fill(0x20u)
        }

        // Benchmark
        val signedTime = measureTime {
            repeat(ITERATIONS) {
                signedArr.fill(0x20)
            }
        }

        val unsignedTime = measureTime {
            repeat(ITERATIONS) {
                unsignedArr.fill(0x20u)
            }
        }

        val signedMs = signedTime.inWholeNanoseconds / 1_000_000.0
        val unsignedMs = unsignedTime.inWholeNanoseconds / 1_000_000.0

        println("ByteArray.fill:  ${signedMs}ms")
        println("UByteArray.fill: ${unsignedMs}ms")
        println("Speedup: ${signedMs / unsignedMs}x")
    }

    // Load word from signed ByteArray (requires masking)
    private fun loadWordSigned(arr: ByteArray, addr: Int): Int {
        return (arr[addr].toInt() and 0xFF) or
               ((arr[addr + 1].toInt() and 0xFF) shl 8) or
               ((arr[addr + 2].toInt() and 0xFF) shl 16) or
               ((arr[addr + 3].toInt() and 0xFF) shl 24)
    }

    // Load word from UByteArray (no masking needed!)
    private fun loadWordUnsigned(arr: UByteArray, addr: Int): Int {
        return arr[addr].toInt() or
               (arr[addr + 1].toInt() shl 8) or
               (arr[addr + 2].toInt() shl 16) or
               (arr[addr + 3].toInt() shl 24)
    }

    // Store word to signed ByteArray
    private fun storeWordSigned(arr: ByteArray, addr: Int, value: Int) {
        arr[addr] = value.toByte()
        arr[addr + 1] = (value shr 8).toByte()
        arr[addr + 2] = (value shr 16).toByte()
        arr[addr + 3] = (value shr 24).toByte()
    }

    // Store word to UByteArray
    private fun storeWordUnsigned(arr: UByteArray, addr: Int, value: Int) {
        arr[addr] = value.toUByte()
        arr[addr + 1] = (value shr 8).toUByte()
        arr[addr + 2] = (value shr 16).toUByte()
        arr[addr + 3] = (value shr 24).toUByte()
    }
}
